package com.byteferry.byteferry.xhs;

import lombok.RequiredArgsConstructor;
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
}
