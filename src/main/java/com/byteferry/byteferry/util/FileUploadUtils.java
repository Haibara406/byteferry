package com.byteferry.byteferry.util;

import com.byteferry.byteferry.enums.UploadEnum;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Component
public class FileUploadUtils {

    @Resource
    private MinioClient client;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.publicUrl:${minio.endpoint}}")
    private String publicUrl;

    @Value("${minio.url}")
    private String url;

    /**
     * 上传文件（自动生成 UUID 文件名）
     */
    public String upload(UploadEnum uploadEnum, MultipartFile file) {
        checkFile(uploadEnum, file);
        checkFormat(uploadEnum, file);
        try {
            String name = UUID.randomUUID().toString();
            String ext = getFileExtension(file.getOriginalFilename());
            String objectName = uploadEnum.getDir() + name + "." + ext;

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .headers(Map.of("Content-Type", Objects.requireNonNull(file.getContentType())))
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build();
            client.putObject(args);

            return publicUrl + "/" + bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（指定文件名）
     */
    public String upload(UploadEnum uploadEnum, MultipartFile file, String fileName) {
        checkFile(uploadEnum, file);
        checkFormat(uploadEnum, file);
        try {
            String ext = getFileExtension(file.getOriginalFilename());
            String objectName = uploadEnum.getDir() + fileName + "." + ext;

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .headers(Map.of("Content-Type", Objects.requireNonNull(file.getContentType())))
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build();
            client.putObject(args);

            return publicUrl + "/" + bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 单文件删除
     */
    public boolean deleteFile(String dir, String fileName) {
        try {
            String objectName = dir + fileName;
            if (!isFileExist(dir, fileName)) {
                log.warn("文件 {} 不存在", objectName);
                return false;
            }
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("文件 {} 已从 MinIO 删除", objectName);
            return true;
        } catch (Exception e) {
            log.error("删除 MinIO 文件 {} 失败: {}", fileName, e.getMessage());
            return false;
        }
    }

    /**
     * 通过完整 URL 删除文件
     */
    public boolean deleteByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return false;
        try {
            // URL format: http://154.94.235.178:9000/byteferry/moment/image/xxx.jpg
            String prefix = publicUrl + "/" + bucketName + "/";
            if (!fileUrl.startsWith(prefix)) {
                // Try old URL format for backward compatibility
                String oldPrefix = url + "/" + bucketName + "/";
                if (fileUrl.startsWith(oldPrefix)) {
                    prefix = oldPrefix;
                } else {
                    return false;
                }
            }
            String objectName = fileUrl.substring(prefix.length());
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("文件 {} 已从 MinIO 删除", objectName);
            return true;
        } catch (Exception e) {
            log.error("通过 URL 删除文件失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通过完整 URL 批量删除文件
     */
    public boolean deleteByUrls(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) return false;
        try {
            String prefix = publicUrl + "/" + bucketName + "/";
            String oldPrefix = url + "/" + bucketName + "/";
            List<DeleteObject> deleteObjects = fileUrls.stream()
                    .filter(fileUrl -> fileUrl != null && (fileUrl.startsWith(prefix) || fileUrl.startsWith(oldPrefix)))
                    .map(fileUrl -> {
                        String actualPrefix = fileUrl.startsWith(prefix) ? prefix : oldPrefix;
                        return new DeleteObject(fileUrl.substring(actualPrefix.length()));
                    })
                    .toList();

            if (deleteObjects.isEmpty()) return false;

            RemoveObjectsArgs args = RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(deleteObjects)
                    .build();
            Iterable<io.minio.Result<DeleteError>> results = client.removeObjects(args);
            for (io.minio.Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.error("文件 {} 删除错误: {}", error.objectName(), error.message());
                return false;
            }
            log.info("批量删除 {} 个文件成功", deleteObjects.size());
            return true;
        } catch (Exception e) {
            log.error("通过 URL 批量删除文件失败", e);
            return false;
        }
    }

    /**
     * 批量删除
     */
    public boolean deleteFiles(List<String> objectNames) {
        try {
            List<DeleteObject> deleteObjects = objectNames.stream()
                    .map(DeleteObject::new).toList();
            RemoveObjectsArgs args = RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(deleteObjects)
                    .build();
            Iterable<io.minio.Result<DeleteError>> results = client.removeObjects(args);
            for (io.minio.Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.error("文件 {} 删除错误: {}", error.objectName(), error.message());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            return false;
        }
    }

    /**
     * 获取目录下的所有文件名称
     */
    public List<String> listFiles(String dir) {
        dir = dir.endsWith("/") ? dir : dir + "/";
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(dir)
                .build();
        Iterable<io.minio.Result<Item>> results = client.listObjects(args);

        List<String> fileNames = new ArrayList<>();
        results.forEach(result -> {
            try {
                fileNames.add(result.get().objectName());
            } catch (Exception e) {
                log.error("获取文件列表出错", e);
            }
        });
        return fileNames;
    }

    /**
     * 判断文件是否存在
     */
    public boolean isFileExist(String dir, String fileName) {
        dir = dir.endsWith("/") ? dir : dir + "/";
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(dir)
                .build();
        Iterable<io.minio.Result<Item>> results = client.listObjects(args);

        for (io.minio.Result<Item> result : results) {
            try {
                if (result.get().objectName().equals(dir + fileName)) {
                    return true;
                }
            } catch (Exception e) {
                log.error("判断文件是否存在出错", e);
            }
        }
        return false;
    }

    /**
     * 从完整路径中截取文件名
     */
    public String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    /**
     * 获取文件后缀（不含点号）
     */
    public String getFileExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    private void checkFile(UploadEnum uploadEnum, MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }
        double sizeInMB = (double) file.getSize() / (1024 * 1024);
        if (sizeInMB >= uploadEnum.getLimitSize()) {
            throw new RuntimeException("上传文件超过限制大小: " + uploadEnum.getLimitSize() + "MB");
        }
    }

    private void checkFormat(UploadEnum uploadEnum, MultipartFile file) {
        String ext = getFileExtension(file.getOriginalFilename());
        if (!uploadEnum.getFormat().contains(ext)) {
            throw new RuntimeException("不支持的文件格式: " + ext + "，支持: " + uploadEnum.getFormat());
        }
    }
}
