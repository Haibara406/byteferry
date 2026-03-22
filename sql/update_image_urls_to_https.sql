-- 更新 moment_images 表中的图片 URL，从 HTTP 改为 HTTPS
-- 执行前请先备份数据库！

-- 更新 image_url
UPDATE moment_images
SET image_url = REPLACE(image_url, 'http://154.94.235.178:9000', 'https://minio.haikari.top')
WHERE image_url LIKE 'http://154.94.235.178:9000%';

-- 更新 video_url
UPDATE moment_images
SET video_url = REPLACE(video_url, 'http://154.94.235.178:9000', 'https://minio.haikari.top')
WHERE video_url LIKE 'http://154.94.235.178:9000%';

-- 检查更新结果
SELECT COUNT(*) as total_images,
       SUM(CASE WHEN image_url LIKE 'https://minio.haikari.top%' THEN 1 ELSE 0 END) as https_images,
       SUM(CASE WHEN image_url LIKE 'http://%' THEN 1 ELSE 0 END) as http_images
FROM moment_images;

-- 查看更新后的示例数据
SELECT id, image_url, video_url FROM moment_images LIMIT 5;

