package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.model.entity.SpaceFile;
import com.byteferry.byteferry.model.entity.SpaceItem;
import com.byteferry.byteferry.service.FileStorageService;
import com.byteferry.byteferry.service.SpaceService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/space")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final FileStorageService fileStorageService;

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
    public ResponseEntity<Map<String, Object>> addText(Authentication auth, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content required");
        }
        SpaceItem item = spaceService.addText(getUserId(auth), content);
        return ResponseEntity.ok(mapItem(item));
    }

    @PostMapping("/items/file")
    public ResponseEntity<Map<String, Object>> addFiles(Authentication auth,
            @RequestParam("file") MultipartFile[] files) throws IOException {
        if (files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Files required");
        }
        SpaceItem item = spaceService.addFiles(getUserId(auth), files);
        return ResponseEntity.ok(mapItem(item));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Map<String, String>> deleteItem(Authentication auth, @PathVariable Long id) {
        try {
            spaceService.deleteItem(getUserId(auth), id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
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
}
