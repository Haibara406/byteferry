package com.byteferry.byteferry.xhs;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class XhsImageExtractorService {

    private static final Pattern URL_IN_TEXT_PATTERN = Pattern.compile("(https?://[^\\s]+)");
    private static final Pattern TITLE_PATTERN = Pattern.compile("property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN_ALT = Pattern.compile("content=[\"']([^\"']+)[\"'][^>]*property=[\"']og:title[\"']", Pattern.CASE_INSENSITIVE);
    // 匹配 sns-webpic-qc.xhscdn.com 域名的图片（无水印原图）
    // 匹配三种格式：src="http://..."、"url":"http://..."、"url":"http:\u002F\u002F..."
    private static final Pattern SHORTCUT_IMAGE_PATTERN = Pattern.compile("(?:src|content|\"url\")\\s*[=:]\\s*[\"'](https?(?::|\\\\u002F)(?:\\/|\\\\u002F){2}sns-webpic-qc\\.xhscdn\\.com[^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public Map<String, Object> extractImages(String rawInput) {
        String sourceUrl = extractUrlFromInput(rawInput);
        validateXhsDomain(sourceUrl);
        return extractImagesViaHtml(sourceUrl);
    }

    private Map<String, Object> extractImagesViaHtml(String sourceUrl) {
        FetchResult fetchResult = fetch(sourceUrl);
        String finalUrl = fetchResult.finalUrl();
        String html = fetchResult.html();
        String title = extractTitle(html);
        List<String> images = collectImageUrls(html);

        if (images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No downloadable image was recognized. Please confirm that it is a public link to a Xiaohongshu image and text post");
        }

        return Map.of(
                "sourceUrl", finalUrl,
                "title", title == null || title.isBlank() ? "rednote post" : title,
                "imageCount", images.size(),
                "images", images
        );
    }

    private String extractUrlFromInput(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please provide the sharing link");
        }
        String trimmed = rawInput.trim();
        Matcher matcher = URL_IN_TEXT_PATTERN.matcher(trimmed);
        String url = matcher.find() ? matcher.group(1) : trimmed;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            URI parsed = new URI(url);
            if (parsed.getHost() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The link format is incorrect");
            }
            return url;
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The link format is incorrect");
        }
    }

    private void validateXhsDomain(String url) {
        try {
            String host = new URI(url).getHost();
            if (host == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The link format is incorrect");
            }
            String lowered = host.toLowerCase();
            boolean valid = lowered.endsWith("xiaohongshu.com")
                    || lowered.endsWith("xhslink.com")
                    || lowered.endsWith("xhscdn.com");
            if (!valid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only links shared on rednote are supported");
            }
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The link format is incorrect");
        }
    }

    private FetchResult fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://www.xiaohongshu.com/")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to capture. The target page returns a status code " + status);
            }
            return new FetchResult(response.uri().toString(), response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to capture. Please try again later");
        }
    }

    private String extractTitle(String html) {
        Matcher m1 = TITLE_PATTERN.matcher(html);
        if (m1.find()) {
            return unescapeHtml(m1.group(1));
        }
        Matcher m2 = TITLE_PATTERN_ALT.matcher(html);
        if (m2.find()) {
            return unescapeHtml(m2.group(1));
        }
        return null;
    }

    private List<String> collectImageUrls(String html) {
        Set<String> imageUrls = new LinkedHashSet<>();

        // 方法 1：从 JSON 数据中提取 traceId（最可靠的无水印方式）
        List<String> traceIdUrls = extractTraceIdUrls(html);
        imageUrls.addAll(traceIdUrls);

        // 方法 2：如果没有找到 traceId，尝试转换现有 URL
        if (imageUrls.isEmpty()) {
            List<String> transformedUrls = extractAndTransformUrls(html);
            imageUrls.addAll(transformedUrls);
        }

        return new ArrayList<>(imageUrls);
    }

    /**
     * 从 HTML 中的 JSON 数据提取 traceId 并构造无水印 URL
     */
    private List<String> extractTraceIdUrls(String html) {
        List<String> urls = new ArrayList<>();

        // 查找 imageList 数组
        Pattern imageListPattern = Pattern.compile("\"imageList\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = imageListPattern.matcher(html);

        if (matcher.find()) {
            String imageListJson = matcher.group(1);

            // 提取所有 traceId
            Pattern traceIdPattern = Pattern.compile("\"traceId\"\\s*:\\s*\"([^\"]+)\"");
            Matcher traceIdMatcher = traceIdPattern.matcher(imageListJson);

            while (traceIdMatcher.find()) {
                String traceId = traceIdMatcher.group(1);
                // 使用 ci.xiaohongshu.com 域名获取无水印图片
                urls.add("https://ci.xiaohongshu.com/" + traceId);
            }
        }

        return urls;
    }

    /**
     * 提取并转换 xhscdn.com URL 为无水印版本
     */
    private List<String> extractAndTransformUrls(String html) {
        List<String> urls = new ArrayList<>();

        Matcher matcher = SHORTCUT_IMAGE_PATTERN.matcher(html);
        while (matcher.find()) {
            String matched = matcher.group(1);
            String normalized = normalizeEscapedUrl(matched);

            if (normalized != null && normalized.contains("xhscdn.com")) {
                String transformed = transformXhsCdnUrl(normalized);
                if (transformed != null && !urls.contains(transformed)) {
                    urls.add(transformed);
                }
            }
        }

        return urls;
    }

    /**
     * 转换 xhscdn.com URL 为无水印版本
     * 从 http://sns-webpic-qc.xhscdn.com/202404121854/a7e6fa93538d17fa5da39ed6195557d7/{token}!nd_dft_wlteh_webp_3
     * 转换为 https://ci.xiaohongshu.com/{token}
     */
    private String transformXhsCdnUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("xhscdn.com")) {
            return originalUrl;
        }

        // 跳过视频 URL
        if (originalUrl.contains("video") || originalUrl.contains("sns-video")) {
            return originalUrl;
        }

        // 提取 token：从第 5 个 / 之后的部分
        String[] parts = originalUrl.split("/");
        if (parts.length > 5) {
            StringBuilder tokenBuilder = new StringBuilder();
            for (int i = 5; i < parts.length; i++) {
                if (i > 5) tokenBuilder.append("/");
                tokenBuilder.append(parts[i]);
            }
            String fullToken = tokenBuilder.toString();

            // 去掉 ! 或 ? 之后的参数（这些参数会添加水印）
            String token = fullToken.split("[!?]")[0];

            // 使用 ci.xiaohongshu.com 获取无水印图片
            return "https://ci.xiaohongshu.com/" + token;
        }

        return originalUrl;
    }

    private String normalizeEscapedUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        // 处理 Unicode 转义：\u002F -> /，\u003A -> :
        String url = raw
                .replace("\\u002F", "/")
                .replace("\\u002f", "/")
                .replace("\\u003A", ":")
                .replace("\\u003a", ":")
                .replace("&amp;", "&")
                .trim();

        // 确保是有效的 URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return null;
        }
        return url;
    }

    private String unescapeHtml(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private record FetchResult(String finalUrl, String html) {}
}