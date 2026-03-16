package com.byteferry.byteferry.service;

import com.byteferry.byteferry.model.entity.SpaceFile;
import com.byteferry.byteferry.model.entity.SpaceItem;
import com.byteferry.byteferry.repository.SpaceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceItemRepository spaceItemRepository;
    private final FileStorageService fileStorageService;

    public List<SpaceItem> getItems(Long userId) {
        return spaceItemRepository.findActiveByUserId(userId, LocalDateTime.now());
    }

    public boolean hasActiveItems(Long userId) {
        return spaceItemRepository.countActiveByUserId(userId, LocalDateTime.now()) > 0;
    }

    public SpaceItem addText(Long userId, String content, int expireSeconds) {
        SpaceItem item = SpaceItem.builder()
                .userId(userId)
                .type(SpaceItem.ItemType.TEXT)
                .content(content)
                .expireAt(LocalDateTime.now().plusSeconds(expireSeconds))
                .build();
        return spaceItemRepository.save(item);
    }

    public SpaceItem addFiles(Long userId, MultipartFile[] files, int expireSeconds) throws IOException {
        boolean allImages = true;
        for (MultipartFile f : files) {
            String ct = f.getContentType();
            if (ct == null || !ct.startsWith("image/")) { allImages = false; break; }
        }

        SpaceItem item = SpaceItem.builder()
                .userId(userId)
                .type(allImages ? SpaceItem.ItemType.IMAGE : SpaceItem.ItemType.FILE)
                .expireAt(LocalDateTime.now().plusSeconds(expireSeconds))
                .build();

        for (MultipartFile f : files) {
            String path = fileStorageService.store(f);
            SpaceFile sf = SpaceFile.builder()
                    .item(item)
                    .fileName(f.getOriginalFilename())
                    .filePath(path)
                    .fileSize(f.getSize())
                    .mimeType(f.getContentType())
                    .build();
            item.getFiles().add(sf);
        }

        return spaceItemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        SpaceItem item = spaceItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (!item.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        for (SpaceFile sf : item.getFiles()) {
            fileStorageService.delete(sf.getFilePath());
        }
        spaceItemRepository.delete(item);
    }

    @Transactional
    public void clearAll(Long userId) {
        List<SpaceItem> items = spaceItemRepository.findByUserId(userId);
        for (SpaceItem item : items) {
            for (SpaceFile sf : item.getFiles()) {
                fileStorageService.delete(sf.getFilePath());
            }
        }
        spaceItemRepository.deleteAll(items);
    }

    @Transactional
    public List<Long> cleanupExpired() {
        List<SpaceItem> expired = spaceItemRepository.findByExpireAtNotNullAndExpireAtBefore(LocalDateTime.now());
        if (expired.isEmpty()) return List.of();

        List<Long> affectedUserIds = expired.stream()
                .map(SpaceItem::getUserId)
                .distinct()
                .toList();

        for (SpaceItem item : expired) {
            for (SpaceFile sf : item.getFiles()) {
                fileStorageService.delete(sf.getFilePath());
            }
        }
        spaceItemRepository.deleteAll(expired);
        return affectedUserIds;
    }

    public SpaceFile getFile(Long userId, Long itemId, Long fileId) {
        SpaceItem item = spaceItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (!item.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return item.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found"));
    }
}
