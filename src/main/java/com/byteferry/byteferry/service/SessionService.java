package com.byteferry.byteferry.service;

import com.byteferry.byteferry.model.SessionData;
import com.byteferry.byteferry.model.ShareData;
import com.byteferry.byteferry.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String KEY_PREFIX = "session:";
    private static final int MAX_RETRY = 5;

    private final RedisTemplate<String, Object> redisTemplate;
    private final FileStorageService fileStorageService;

    @Value("${byteferry.session.default-expire-seconds:1800}")
    private int defaultExpireSeconds;

    @Value("${byteferry.share.code-length}")
    private int codeLength;

    public SessionData createSession(Integer expireSeconds) {
        int expire = resolveExpire(expireSeconds);

        for (int i = 0; i < MAX_RETRY; i++) {
            String code = CodeGenerator.generate(codeLength);
            String key = KEY_PREFIX + code;

            SessionData session = SessionData.builder()
                    .code(code)
                    .status(SessionData.Status.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .expireSeconds(expire)
                    .items(new ArrayList<>())
                    .nextItemId(0)
                    .build();

            Boolean absent = redisTemplate.opsForValue().setIfAbsent(
                    key, session, expire, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(absent)) {
                return session;
            }
        }
        throw new RuntimeException("Failed to generate unique session code");
    }

    public SessionData getSession(String code) {
        Object obj = redisTemplate.opsForValue().get(KEY_PREFIX + code);
        if (obj == null) {
            return null;
        }
        return (SessionData) obj;
    }

    public SessionData.SessionItem addTextItem(String code, String content) {
        SessionData session = getActiveSession(code);

        SessionData.SessionItem item = SessionData.SessionItem.builder()
                .id(session.getNextItemId())
                .type(ShareData.Type.TEXT)
                .content(content)
                .addedAt(LocalDateTime.now())
                .build();

        session.getItems().add(item);
        session.setNextItemId(session.getNextItemId() + 1);
        saveAndRefreshTTL(session);
        return item;
    }

    public SessionData.SessionItem addFileItem(String code, MultipartFile[] files) throws IOException {
        SessionData session = getActiveSession(code);

        List<ShareData.FileInfo> fileInfos = new ArrayList<>();
        boolean allImages = true;
        for (MultipartFile file : files) {
            String filePath = fileStorageService.store(file);
            String ct = file.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                allImages = false;
            }
            fileInfos.add(ShareData.FileInfo.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .mimeType(ct)
                    .build());
        }

        SessionData.SessionItem item = SessionData.SessionItem.builder()
                .id(session.getNextItemId())
                .type(allImages ? ShareData.Type.IMAGE : ShareData.Type.FILE)
                .files(fileInfos)
                .addedAt(LocalDateTime.now())
                .build();

        session.getItems().add(item);
        session.setNextItemId(session.getNextItemId() + 1);
        saveAndRefreshTTL(session);
        return item;
    }

    public void closeSession(String code) {
        SessionData session = getSession(code);
        if (session == null) return;

        // Delete all files on disk
        if (session.getItems() != null) {
            for (SessionData.SessionItem item : session.getItems()) {
                if (item.getFiles() != null) {
                    for (ShareData.FileInfo fi : item.getFiles()) {
                        fileStorageService.delete(fi.getFilePath());
                    }
                }
            }
        }

        // Keep CLOSED status briefly so receiver can detect it
        session.setStatus(SessionData.Status.CLOSED);
        session.setItems(new ArrayList<>());
        String key = KEY_PREFIX + code;
        redisTemplate.opsForValue().set(key, session, 60, TimeUnit.SECONDS);
    }

    public long getRemainingSeconds(String code) {
        Long ttl = redisTemplate.getExpire(KEY_PREFIX + code, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    private SessionData getActiveSession(String code) {
        SessionData session = getSession(code);
        if (session == null) {
            throw new RuntimeException("Session not found or expired");
        }
        if (session.getStatus() != SessionData.Status.ACTIVE) {
            throw new RuntimeException("Session is closed");
        }
        return session;
    }

    private void saveAndRefreshTTL(SessionData session) {
        String key = KEY_PREFIX + session.getCode();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long remainingTTL = (ttl != null && ttl > 0) ? ttl : session.getExpireSeconds();
        redisTemplate.opsForValue().set(key, session, remainingTTL, TimeUnit.SECONDS);
    }

    private int resolveExpire(Integer expireSeconds) {
        if (expireSeconds != null && expireSeconds > 0) {
            return Math.min(expireSeconds, 7200); // max 2 hours
        }
        return defaultExpireSeconds;
    }
}
