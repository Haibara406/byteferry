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

        FetchResult fetchResult = fetch(sourceUrl);
        String finalUrl = fetchResult.finalUrl();
        String html = fetchResult.html();
        String title = extractTitle(html);
        List<String> images = collectImageUrls(html);

        if (images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未识别到可下载图片，请确认是公开的小红书图文帖分享链接");
        }

        return Map.of(
                "sourceUrl", finalUrl,
                "title", title == null || title.isBlank() ? "小红书帖子" : title,
                "imageCount", images.size(),
                "images", images
        );
    }

    private String extractUrlFromInput(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请提供分享链接");
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "链接格式不正确");
            }
            return url;
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "链接格式不正确");
        }
    }

    private void validateXhsDomain(String url) {
        try {
            String host = new URI(url).getHost();
            if (host == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "链接格式不正确");
            }
            String lowered = host.toLowerCase();
            boolean valid = lowered.endsWith("xiaohongshu.com")
                    || lowered.endsWith("xhslink.com")
                    || lowered.endsWith("xhscdn.com");
            if (!valid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持小红书分享链接");
            }
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "链接格式不正确");
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
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "抓取失败，目标页面返回状态码 " + status);
            }
            return new FetchResult(response.uri().toString(), response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "抓取失败，请稍后重试");
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
        // 使用 LinkedHashMap 来保持顺序，key 是文件名（用于去重），value 是完整 URL
        Map<String, String> imageMap = new LinkedHashMap<>();

        Matcher matcher = SHORTCUT_IMAGE_PATTERN.matcher(html);
        while (matcher.find()) {
            String matched = matcher.group(1);
            String normalized = normalizeEscapedUrl(matched);

            if (normalized != null && normalized.contains("!h5_1080jpg")) {
                // 提取文件名部分（URL 的最后一段，用于去重）
                // 例如：1040g00831tq55abqne005nss8acg891o0ncfpig!h5_1080jpg
                String fileName = extractFileName(normalized);

                // 只保留第一次出现的版本（来自 <img src>，通常是高清无水印版本）
                if (fileName != null && !imageMap.containsKey(fileName)) {
                    imageMap.put(fileName, normalized);
                }
            }
        }

        return new ArrayList<>(imageMap.values());
    }

    private String extractFileName(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        return null;
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