# 小红书无水印图片提取实现文档

## 背景

小红书（RedNote/XHS）的图片分享链接默认会返回带水印的图片。本文档记录了如何通过分析小红书的页面结构和 CDN 机制，实现无水印图片的提取。

## 问题分析

### 初始问题
- 直接从 HTML 的 `<img src>` 标签提取的图片 URL 包含水印参数
- 例如：`http://sns-webpic-qc.xhscdn.com/.../token!h5_1080jpg` 中的 `!h5_1080jpg` 会添加水印

### 关键发现
通过分析开源项目 [XHS_Downloader_Android](https://github.com/neoruaa/XHS_Downloader_Android)，发现了两个关键突破点：

1. **JSON 数据中的 traceId**：小红书页面 HTML 中嵌入了 JSON 数据，包含 `imageList` 数组，每个图片对象都有 `traceId` 字段
2. **CDN 域名转换**：使用 `ci.xiaohongshu.com` 域名可以获取无水印原图

## 技术实现

### 方案一：提取 traceId（推荐）

#### 原理
小红书页面的 HTML 中包含类似这样的 JSON 结构：

```json
{
  "imageList": [
    {
      "traceId": "1040g2sg30s6qcqhm5u005p7bnqkgbqhm5u0bnqk",
      "urlDefault": "...",
      "url": "..."
    }
  ]
}
```

#### 实现代码

```java
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
```

#### 优势
- 最可靠的方式，直接使用官方数据
- 不需要复杂的 URL 解析
- 获取的是原始图片标识符

### 方案二：URL 转换（备用）

#### 原理
将带水印的 CDN URL 转换为无水印版本：

**转换前：**
```
http://sns-webpic-qc.xhscdn.com/202404121854/a7e6fa93538d17fa5da39ed6195557d7/1040g2sg30s6qcqhm5u005p7bnqkgbqhm5u0bnqk!nd_dft_wlteh_webp_3
```

**转换后：**
```
https://ci.xiaohongshu.com/1040g2sg30s6qcqhm5u005p7bnqkgbqhm5u0bnqk
```

#### 实现代码

```java
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
```

#### 转换步骤
1. 按 `/` 分割 URL
2. 提取第 5 个 `/` 之后的所有部分作为 token
3. 去除 `!` 或 `?` 后的参数（如 `!nd_dft_wlteh_webp_3`、`!h5_1080jpg`）
4. 拼接到 `https://ci.xiaohongshu.com/` 后面

### 综合策略

```java
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
```

## 关键技术点

### 1. CDN 域名对比

| 域名 | 用途 | 是否有水印 |
|------|------|-----------|
| `sns-webpic-qc.xhscdn.com` | 网页展示用 CDN | 有水印（带参数时） |
| `sns-img-qc.xhscdn.com` | 图片存储 CDN | 可能有水印 |
| `ci.xiaohongshu.com` | 内容接口 | 无水印原图 |

### 2. 水印参数识别

常见的水印参数：
- `!h5_1080jpg` - H5 页面 1080p 压缩版本
- `!nd_dft_wlteh_webp_3` - 默认水印 WebP 格式
- `!nc_n_webp_mw_1` - 移动端水印版本

**关键：去除 `!` 或 `?` 后的所有参数即可获取原图**

### 3. 正则表达式说明

```java
// 匹配 imageList 数组（使用 DOTALL 模式匹配多行）
Pattern imageListPattern = Pattern.compile("\"imageList\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);

// 匹配 traceId 字段
Pattern traceIdPattern = Pattern.compile("\"traceId\"\\s*:\\s*\"([^\"]+)\"");
```

- `\\s*` - 匹配可能的空白字符
- `(.*?)` - 非贪婪匹配任意内容
- `Pattern.DOTALL` - 让 `.` 可以匹配换行符

## 测试验证

### 测试用例

```java
// 原始带水印 URL
String watermarkedUrl = "http://sns-webpic-qc.xhscdn.com/202404121854/a7e6fa93538d17fa5da39ed6195557d7/1040g2sg30s6qcqhm5u005p7bnqkgbqhm5u0bnqk!h5_1080jpg";

// 转换后无水印 URL
String unwatermarkedUrl = transformXhsCdnUrl(watermarkedUrl);
// 结果: https://ci.xiaohongshu.com/1040g2sg30s6qcqhm5u005p7bnqkgbqhm5u0bnqk
```

### 验证方法

1. 访问转换前的 URL - 图片带水印
2. 访问转换后的 URL - 图片无水印
3. 对比两张图片的视觉效果

## 注意事项

### 1. 视频 URL 处理
视频 URL 不需要转换，保持原样：
```java
if (originalUrl.contains("video") || originalUrl.contains("sns-video")) {
    return originalUrl;
}
```

### 2. URL 格式兼容性
- 处理 Unicode 转义：`\u002F` → `/`
- 处理 HTML 实体：`&amp;` → `&`
- 确保 URL 以 `http://` 或 `https://` 开头

### 3. 错误处理
- 如果 traceId 提取失败，回退到 URL 转换方式
- 如果两种方式都失败，返回空列表并提示用户

## 性能优化

1. **使用 LinkedHashSet 去重**：保持顺序的同时避免重复 URL
2. **优先级策略**：先尝试最可靠的 traceId 方式，失败后再尝试 URL 转换
3. **正则预编译**：将常用正则表达式定义为静态常量

## 参考资源

- [XHS_Downloader_Android](https://github.com/neoruaa/XHS_Downloader_Android) - Android 端小红书下载器
- 小红书 CDN 架构分析
- 图片水印技术原理

## 更新日志

- **2026-03-19**：实现基于 traceId 的无水印图片提取
- **2026-03-19**：添加 URL 转换备用方案
- **2026-03-19**：完善错误处理和回退机制

## 总结

通过分析小红书的页面结构和 CDN 机制，我们实现了两种互补的无水印图片提取方案：

1. **traceId 方案**：从 JSON 数据直接提取官方标识符，最可靠
2. **URL 转换方案**：通过解析和转换 CDN URL，作为备用方案

核心思路是：**使用 `ci.xiaohongshu.com` 域名 + 去除水印参数 = 无水印原图**

这种方法不依赖第三方 API，稳定性高，且符合小红书的 CDN 架构设计。
