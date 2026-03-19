package com.byteferry.byteferry.xhs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/xhs")
@RequiredArgsConstructor
public class XhsImageController {

    private final XhsImageExtractorService xhsImageExtractorService;

    @PostMapping("/extract-images")
    public ResponseEntity<Map<String, Object>> extractImages(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        return ResponseEntity.ok(xhsImageExtractorService.extractImages(url));
    }

    @GetMapping("/proxy-download")
    public ResponseEntity<byte[]> proxyDownload(@RequestParam String url) {
        byte[] content = xhsImageExtractorService.downloadFile(url);

        // 根据 URL 判断文件类型
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (url.contains("video") || url.endsWith(".mp4")) {
            contentType = "video/mp4";
        } else if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (url.endsWith(".png")) {
            contentType = "image/png";
        } else if (url.endsWith(".webp")) {
            contentType = "image/webp";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .body(content);
    }
}
