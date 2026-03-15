package com.byteferry.byteferry.service;

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
public class ShareService {

    private static final String KEY_PREFIX = "share:";
    private static final int MAX_RETRY = 5;

    private final RedisTemplate<String, Object> redisTemplate;
    private final FileStorageService fileStorageService;

    @Value("${byteferry.share.default-expire-seconds}")
    private int defaultExpireSeconds;

    @Value("${byteferry.share.code-length}")
    private int codeLength;

    public String shareText(String content, Integer expireSeconds, boolean deleteAfterDownload) {
        ShareData data = ShareData.builder()
                .type(ShareData.Type.TEXT)
                .content(content)
                .createdAt(LocalDateTime.now())
                .expireSeconds(resolveExpire(expireSeconds))
                .deleteAfterDownload(deleteAfterDownload)
                .build();
        return save(data);
    }

    public String shareFiles(MultipartFile[] files, ShareData.Type type,
                             Integer expireSeconds, boolean deleteAfterDownload) throws IOException {
        List<ShareData.FileInfo> fileInfos = new ArrayList<>();
        for (MultipartFile file : files) {
            String filePath = fileStorageService.store(file);
            fileInfos.add(ShareData.FileInfo.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .build());
        }

        ShareData data = ShareData.builder()
                .type(type)
                .files(fileInfos)
                .createdAt(LocalDateTime.now())
                .expireSeconds(resolveExpire(expireSeconds))
                .deleteAfterDownload(deleteAfterDownload)
                .build();
        return save(data);
    }

    public ShareData get(String code) {
        Object obj = redisTemplate.opsForValue().get(KEY_PREFIX + code);
        if (obj == null) {
            return null;
        }
        return (ShareData) obj;
    }

    public void deleteShare(String code) {
        ShareData data = get(code);
        if (data != null && data.getFiles() != null) {
            for (ShareData.FileInfo fi : data.getFiles()) {
                fileStorageService.delete(fi.getFilePath());
            }
        }
        redisTemplate.delete(KEY_PREFIX + code);
    }

    public void handlePostDownload(String code) {
        ShareData data = get(code);
        if (data != null && data.isDeleteAfterDownload()) {
            deleteShare(code);
        }
    }

    private String save(ShareData data) {
        for (int i = 0; i < MAX_RETRY; i++) {
            String code = CodeGenerator.generate(codeLength);
            String key = KEY_PREFIX + code;
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(
                    key, data, data.getExpireSeconds(), TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(absent)) {
                return code;
            }
        }
        throw new RuntimeException("Failed to generate unique share code");
    }

    private int resolveExpire(Integer expireSeconds) {
        if (expireSeconds != null && expireSeconds > 0) {
            return Math.min(expireSeconds, 3600);
        }
        return defaultExpireSeconds;
    }
}
