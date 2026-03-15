package com.byteferry.byteferry.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${byteferry.storage.file-dir}")
    private String fileDir;

    private Path storagePath;

    @PostConstruct
    public void init() throws IOException {
        storagePath = Paths.get(fileDir).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
    }

    public String store(MultipartFile file) throws IOException {
        String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = storagePath.resolve(uniqueName).normalize();

        if (!target.startsWith(storagePath)) {
            throw new IOException("Invalid file path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        if (!Files.exists(target) || Files.size(target) == 0) {
            throw new IOException("File storage failed: " + target);
        }

        return target.toString();
    }

    public Path load(String filePath) {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(storagePath)) {
            throw new RuntimeException("Invalid file path");
        }
        return path;
    }

    public void delete(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
