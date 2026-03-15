package com.byteferry.byteferry.service;

import com.byteferry.byteferry.model.entity.SpaceFile;
import com.byteferry.byteferry.model.entity.SpaceItem;
import com.byteferry.byteferry.repository.SpaceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceItemRepository spaceItemRepository;
    private final FileStorageService fileStorageService;

    public List<SpaceItem> getItems(Long userId) {
        return spaceItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public SpaceItem addText(Long userId, String content) {
        SpaceItem item = SpaceItem.builder()
                .userId(userId)
                .type(SpaceItem.ItemType.TEXT)
                .content(content)
                .build();
        return spaceItemRepository.save(item);
    }

    public SpaceItem addFiles(Long userId, MultipartFile[] files) throws IOException {
        boolean allImages = true;
        for (MultipartFile f : files) {
            String ct = f.getContentType();
            if (ct == null || !ct.startsWith("image/")) { allImages = false; break; }
        }

        SpaceItem item = SpaceItem.builder()
                .userId(userId)
                .type(allImages ? SpaceItem.ItemType.IMAGE : SpaceItem.ItemType.FILE)
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
        // Delete files from disk
        for (SpaceFile sf : item.getFiles()) {
            fileStorageService.delete(sf.getFilePath());
        }
        spaceItemRepository.delete(item);
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
