package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.model.entity.SpaceFile;
import com.byteferry.byteferry.model.entity.SpaceItem;
import com.byteferry.byteferry.service.FileStorageService;
import com.byteferry.byteferry.service.SpaceService;
import com.byteferry.byteferry.websocket.SpaceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/space")
@RequiredArgsConstructor
public class SpaceController {

    private static final int DEFAULT_EXPIRE_SECONDS = 1800;

    private final SpaceService spaceService;
    private final FileStorageService fileStorageService;
    private final SpaceWebSocketHandler spaceWsHandler;

    private Long getUserId(Authentication auth) {
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/items")
    public ResponseEntity<List<Map<String, Object>>> getItems(Authentication auth) {
        List<SpaceItem> items = spaceService.getItems(getUserId(auth));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SpaceItem item : items) {
            result.add(mapItem(item));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/items/text")
    public ResponseEntity<Map<String, Object>> addText(Authentication auth, @RequestBody Map<String, Object> body) {
        String content = body.get("content") != null ? body.get("content").toString() : null;
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content required");
        }
        int expire = parseExpire(body.get("expireSeconds"));
        Long userId = getUserId(auth);
        SpaceItem item = spaceService.addText(userId, content, expire);
        spaceWsHandler.notifyUser(userId);
        return ResponseEntity.ok(mapItem(item));
    }

    @PostMapping("/items/file")
    public ResponseEntity<Map<String, Object>> addFiles(Authentication auth,
            @RequestParam("file") MultipartFile[] files,
            @RequestParam(value = "expireSeconds", required = false) Integer expireSeconds) throws IOException {
        if (files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Files required");
        }
        int expire = expireSeconds != null && expireSeconds > 0 ? Math.min(expireSeconds, 7200) : DEFAULT_EXPIRE_SECONDS;
        Long userId = getUserId(auth);
        SpaceItem item = spaceService.addFiles(userId, files, expire);
        spaceWsHandler.notifyUser(userId);
        return ResponseEntity.ok(mapItem(item));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Map<String, String>> deleteItem(Authentication auth, @PathVariable Long id) {
        try {
            Long userId = getUserId(auth);
            spaceService.deleteItem(userId, id);
            if (spaceService.hasActiveItems(userId)) {
                spaceWsHandler.notifyUser(userId);
            } else {
                spaceWsHandler.notifyEmpty(userId);
            }
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/items/clear")
    public ResponseEntity<Map<String, String>> clearAll(Authentication auth) {
        Long userId = getUserId(auth);
        spaceService.clearAll(userId);
        spaceWsHandler.notifyEmpty(userId);
        return ResponseEntity.ok(Map.of("message", "Cleared"));
    }

    @GetMapping("/items/{itemId}/files/{fileId}/preview")
    public ResponseEntity<Resource> preview(Authentication auth,
            @PathVariable Long itemId, @PathVariable Long fileId) throws IOException {
        return serveFile(getUserId(auth), itemId, fileId, true);
    }

    @GetMapping("/items/{itemId}/files/{fileId}/download")
    public ResponseEntity<Resource> download(Authentication auth,
            @PathVariable Long itemId, @PathVariable Long fileId) throws IOException {
        return serveFile(getUserId(auth), itemId, fileId, false);
    }

    private ResponseEntity<Resource> serveFile(Long userId, Long itemId, Long fileId, boolean inline) throws IOException {
        SpaceFile sf = spaceService.getFile(userId, itemId, fileId);
        Path path = fileStorageService.load(sf.getFilePath());
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        long size = Files.size(path);
        InputStream in = Files.newInputStream(path);
        String encodedName = URLEncoder.encode(
                sf.getFileName() != null ? sf.getFileName() : "download", StandardCharsets.UTF_8
        ).replace("+", "%20");

        MediaType mediaType = sf.getMimeType() != null
                ? MediaType.parseMediaType(sf.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(size)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encodedName)
                .body(new InputStreamResource(in));
    }

    private Map<String, Object> mapItem(SpaceItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("type", item.getType().name());
        map.put("content", item.getContent() != null ? item.getContent() : "");
        map.put("createdAt", item.getCreatedAt().toString());

        if (item.getExpireAt() != null) {
            map.put("expireAt", item.getExpireAt().toString());
            long remaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), item.getExpireAt());
            map.put("remainingSeconds", Math.max(remaining, 0));
        }

        List<Map<String, Object>> files = new ArrayList<>();
        for (SpaceFile sf : item.getFiles()) {
            Map<String, Object> fm = new HashMap<>();
            fm.put("id", sf.getId());
            fm.put("fileName", sf.getFileName() != null ? sf.getFileName() : "");
            fm.put("fileSize", sf.getFileSize() != null ? sf.getFileSize() : 0);
            fm.put("mimeType", sf.getMimeType() != null ? sf.getMimeType() : "");
            files.add(fm);
        }
        map.put("files", files);
        return map;
    }

    private int parseExpire(Object val) {
        if (val == null) return DEFAULT_EXPIRE_SECONDS;
        try {
            int v = Integer.parseInt(val.toString());
            return v > 0 ? Math.min(v, 7200) : DEFAULT_EXPIRE_SECONDS;
        } catch (NumberFormatException e) {
            return DEFAULT_EXPIRE_SECONDS;
        }
    }
}
