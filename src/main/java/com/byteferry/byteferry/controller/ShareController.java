package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.model.ShareData;
import com.byteferry.byteferry.service.FileStorageService;
import com.byteferry.byteferry.service.ShareService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final FileStorageService fileStorageService;

    @PostMapping("/text")
    public ResponseEntity<Map<String, String>> shareText(@RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
        }

        Integer expire = body.get("expireSeconds") != null
                ? Integer.valueOf(body.get("expireSeconds").toString()) : null;
        boolean deleteAfter = Boolean.TRUE.equals(body.get("deleteAfterDownload"));

        String code = shareService.shareText(content, expire, deleteAfter);
        return ResponseEntity.ok(Map.of("code", code));
    }

    @PostMapping("/file")
    public ResponseEntity<Map<String, String>> shareFile(
            @RequestParam("file") MultipartFile[] files,
            @RequestParam(value = "type", defaultValue = "FILE") String type,
            @RequestParam(value = "expireSeconds", required = false) Integer expireSeconds,
            @RequestParam(value = "deleteAfterDownload", defaultValue = "false") boolean deleteAfterDownload
    ) throws IOException {
        if (files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one file is required");
        }

        ShareData.Type shareType = "IMAGE".equalsIgnoreCase(type)
                ? ShareData.Type.IMAGE : ShareData.Type.FILE;
        boolean allImages = true;
        for (MultipartFile f : files) {
            String ct = f.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                allImages = false;
                break;
            }
        }
        if (allImages) {
            shareType = ShareData.Type.IMAGE;
        }

        String code = shareService.shareFiles(files, shareType, expireSeconds, deleteAfterDownload);
        return ResponseEntity.ok(Map.of("code", code));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> getShare(@PathVariable String code) {
        ShareData data = shareService.get(code.toUpperCase());
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found or expired");
        }

        List<Map<String, Object>> fileList = new ArrayList<>();
        if (data.getFiles() != null) {
            for (int i = 0; i < data.getFiles().size(); i++) {
                ShareData.FileInfo fi = data.getFiles().get(i);
                fileList.add(Map.of(
                        "index", i,
                        "fileName", fi.getFileName() != null ? fi.getFileName() : "",
                        "fileSize", fi.getFileSize() != null ? fi.getFileSize() : 0,
                        "mimeType", fi.getMimeType() != null ? fi.getMimeType() : ""
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "type", data.getType().name(),
                "content", data.getContent() != null ? data.getContent() : "",
                "files", fileList,
                "createdAt", data.getCreatedAt().toString(),
                "deleteAfterDownload", data.isDeleteAfterDownload()
        ));
    }

    /**
     * Preview file inline (for image preview, does NOT trigger delete-after-download)
     */
    @GetMapping("/{code}/preview/{index}")
    public ResponseEntity<Resource> preview(
            @PathVariable String code,
            @PathVariable int index) throws IOException {
        return serveFile(code.toUpperCase(), index, true);
    }

    /**
     * Download file as attachment (triggers delete-after-download if enabled)
     */
    @GetMapping("/{code}/download/{index}")
    public ResponseEntity<Resource> download(
            @PathVariable String code,
            @PathVariable int index) throws IOException {
        ResponseEntity<Resource> response = serveFile(code.toUpperCase(), index, false);
        shareService.handlePostDownload(code.toUpperCase());
        return response;
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Map<String, String>> deleteShare(@PathVariable String code) {
        shareService.deleteShare(code.toUpperCase());
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    private ResponseEntity<Resource> serveFile(String code, int index, boolean inline) throws IOException {
        ShareData data = shareService.get(code);
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found or expired");
        }
        if (data.getFiles() == null || index < 0 || index >= data.getFiles().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file index");
        }

        ShareData.FileInfo fi = data.getFiles().get(index);
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
