package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.model.SessionData;
import com.byteferry.byteferry.model.ShareData;
import com.byteferry.byteferry.service.FileStorageService;
import com.byteferry.byteferry.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestBody(required = false) Map<String, Object> body) {
        Integer expire = null;
        if (body != null && body.get("expireSeconds") != null) {
            expire = Integer.valueOf(body.get("expireSeconds").toString());
        }

        SessionData session = sessionService.createSession(expire);
        return ResponseEntity.ok(Map.of(
                "code", session.getCode(),
                "expireSeconds", session.getExpireSeconds(),
                "createdAt", session.getCreatedAt().toString()
        ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String code) {
        SessionData session = sessionService.getSession(code.toUpperCase());
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or expired");
        }

        long remaining = sessionService.getRemainingSeconds(code.toUpperCase());

        List<Map<String, Object>> itemList = new ArrayList<>();
        if (session.getItems() != null) {
            for (SessionData.SessionItem item : session.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("type", item.getType().name());
                itemMap.put("content", item.getContent() != null ? item.getContent() : "");
                itemMap.put("addedAt", item.getAddedAt().toString());

                List<Map<String, Object>> fileList = new ArrayList<>();
                if (item.getFiles() != null) {
                    for (int i = 0; i < item.getFiles().size(); i++) {
                        ShareData.FileInfo fi = item.getFiles().get(i);
                        Map<String, Object> fileMap = new HashMap<>();
                        fileMap.put("index", i);
                        fileMap.put("fileName", fi.getFileName() != null ? fi.getFileName() : "");
                        fileMap.put("fileSize", fi.getFileSize() != null ? fi.getFileSize() : 0);
                        fileMap.put("mimeType", fi.getMimeType() != null ? fi.getMimeType() : "");
                        fileList.add(fileMap);
                    }
                }
                itemMap.put("files", fileList);
                itemList.add(itemMap);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", session.getCode());
        result.put("status", session.getStatus().name());
        result.put("createdAt", session.getCreatedAt().toString());
        result.put("expireSeconds", session.getExpireSeconds());
        result.put("remainingSeconds", remaining);
        result.put("itemCount", session.getItems() != null ? session.getItems().size() : 0);
        result.put("items", itemList);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{code}/items/text")
    public ResponseEntity<Map<String, Object>> addTextItem(
            @PathVariable String code,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
        }

        try {
            SessionData.SessionItem item = sessionService.addTextItem(code.toUpperCase(), content);
            return ResponseEntity.ok(Map.of(
                    "id", item.getId(),
                    "type", item.getType().name()
            ));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{code}/items/file")
    public ResponseEntity<Map<String, Object>> addFileItem(
            @PathVariable String code,
            @RequestParam("file") MultipartFile[] files) throws IOException {
        if (files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one file is required");
        }

        try {
            SessionData.SessionItem item = sessionService.addFileItem(code.toUpperCase(), files);
            return ResponseEntity.ok(Map.of(
                    "id", item.getId(),
                    "type", item.getType().name()
            ));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{code}/items/{itemId}/preview/{fileIndex}")
    public ResponseEntity<Resource> preview(
            @PathVariable String code,
            @PathVariable int itemId,
            @PathVariable int fileIndex) throws IOException {
        return serveSessionFile(code.toUpperCase(), itemId, fileIndex, true);
    }

    @GetMapping("/{code}/items/{itemId}/download/{fileIndex}")
    public ResponseEntity<Resource> download(
            @PathVariable String code,
            @PathVariable int itemId,
            @PathVariable int fileIndex) throws IOException {
        return serveSessionFile(code.toUpperCase(), itemId, fileIndex, false);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Map<String, String>> closeSession(@PathVariable String code) {
        sessionService.closeSession(code.toUpperCase());
        return ResponseEntity.ok(Map.of("message", "Session closed"));
    }

    private ResponseEntity<Resource> serveSessionFile(
            String code, int itemId, int fileIndex, boolean inline) throws IOException {
        SessionData session = sessionService.getSession(code);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or expired");
        }

        SessionData.SessionItem item = session.getItems().stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        if (item.getFiles() == null || fileIndex < 0 || fileIndex >= item.getFiles().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file index");
        }

        ShareData.FileInfo fi = item.getFiles().get(fileIndex);
        Path filePath = fileStorageService.load(fi.getFilePath());

        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on disk");
        }

        long fileSize = Files.size(filePath);
        InputStream inputStream = Files.newInputStream(filePath);
        InputStreamResource resource = new InputStreamResource(inputStream);

        String encodedName = URLEncoder.encode(
                fi.getFileName() != null ? fi.getFileName() : "download",
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        MediaType mediaType = fi.getMimeType() != null
                ? MediaType.parseMediaType(fi.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;

        String disposition = inline
                ? "inline; filename*=UTF-8''" + encodedName
                : "attachment; filename*=UTF-8''" + encodedName;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(fileSize)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }
}
