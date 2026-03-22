package com.byteferry.byteferry.service;

import com.byteferry.byteferry.enums.UploadEnum;
import com.byteferry.byteferry.model.dto.PageResult;
import com.byteferry.byteferry.model.entity.*;
import com.byteferry.byteferry.model.enums.Visibility;
import com.byteferry.byteferry.repository.*;
import com.byteferry.byteferry.util.FileUploadUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class MomentService {

    private final MomentRepository momentRepository;
    private final MomentImageRepository momentImageRepository;
    private final MomentVisibilityRuleRepository visibilityRuleRepository;
    private final MomentShareLinkRepository shareLinkRepository;
    private final MomentReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final FileUploadUtils fileUploadUtils;
    private final CacheInvalidationService cacheInvalidationService;
    private final CacheManager redisCacheManager;

    // 为每个缓存键维护一个锁，使用 Caffeine 自动过期清理
    private final com.github.benmanes.caffeine.cache.Cache<String, Lock> lockCache =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(10, java.util.concurrent.TimeUnit.MINUTES)
                    .build();

    @Transactional
    public Moment createMoment(Long userId, String textContent, boolean cardMode,
                                Visibility visibility, List<Long> visibleUserIds,
                                MultipartFile[] images, MultipartFile[] liveImages, MultipartFile[] liveVideos) {

        // Create moment
        Moment moment = Moment.builder()
                .userId(userId)
                .textContent(textContent)
                .cardMode(cardMode)
                .visibility(visibility)
                .build();

        moment = momentRepository.save(moment);

        // Handle visibility rules - 批量保存
        if ((visibility == Visibility.VISIBLE_TO || visibility == Visibility.HIDDEN_FROM) && visibleUserIds != null) {
            List<MomentVisibilityRule> rules = new ArrayList<>();
            for (Long targetUserId : visibleUserIds) {
                MomentVisibilityRule rule = MomentVisibilityRule.builder()
                        .momentId(moment.getId())
                        .userId(targetUserId)
                        .build();
                rules.add(rule);
            }
            visibilityRuleRepository.saveAll(rules);
        }

        // Handle regular images - 批量保存
        List<MomentImage> imagesToSave = new ArrayList<>();
        if (images != null && images.length > 0) {
            for (int i = 0; i < Math.min(images.length, 9); i++) {
                String imageUrl = fileUploadUtils.upload(UploadEnum.MOMENT_IMAGE, images[i]);
                MomentImage momentImage = MomentImage.builder()
                        .momentId(moment.getId())
                        .imageUrl(imageUrl)
                        .isLivePhoto(false)
                        .sortOrder(i)
                        .build();
                imagesToSave.add(momentImage);
            }
        }

        // Handle live photos
        if (liveImages != null && liveVideos != null && liveImages.length == liveVideos.length) {
            int offset = (images != null) ? images.length : 0;
            for (int i = 0; i < Math.min(liveImages.length, 9 - offset); i++) {
                String imageUrl = fileUploadUtils.upload(UploadEnum.MOMENT_IMAGE, liveImages[i]);
                String videoUrl = fileUploadUtils.upload(UploadEnum.MOMENT_VIDEO, liveVideos[i]);
                MomentImage momentImage = MomentImage.builder()
                        .momentId(moment.getId())
                        .imageUrl(imageUrl)
                        .videoUrl(videoUrl)
                        .isLivePhoto(true)
                        .sortOrder(offset + i)
                        .build();
                imagesToSave.add(momentImage);
            }
        }

        // 批量保存所有图片
        if (!imagesToSave.isEmpty()) {
            momentImageRepository.saveAll(imagesToSave);
        }

        // Reload moment with images
        Moment result = momentRepository.findById(moment.getId()).orElse(moment);
        result.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(moment.getId()));
        loadUserInfo(result);

        // 清除缓存
        cacheInvalidationService.onMomentCreated(result);

        return result;
    }

    public Moment getMoment(Long id, Long viewerId) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Moment not found"));

        if (!canView(moment, viewerId)) {
            throw new RuntimeException("No permission to view this moment");
        }

        // Load images and user info
        moment.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(id));
        loadUserInfo(moment);
        return moment;
    }

    /**
     * 获取缓存键对应的锁（使用 Caffeine 自动过期）
     */
    private Lock getLock(String cacheKey) {
        return lockCache.get(cacheKey, k -> new ReentrantLock());
    }

    /**
     * 双重检测锁获取My Moments
     */
    public Page<Moment> getMyMoments(Long userId, Pageable pageable) {
        String cacheKey = userId + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
        Cache cache = redisCacheManager.getCache("myMoments");

        if (cache == null) {
            return loadMyMomentsFromDb(userId, pageable);
        }

        // 第一次检查缓存
        Cache.ValueWrapper wrapper = cache.get(cacheKey);
        if (wrapper != null) {
            try {
                Object cachedValue = wrapper.get();
                if (cachedValue instanceof PageResult) {
                    return ((PageResult<Moment>) cachedValue).toPage();
                } else {
                    // 旧格式的缓存，清除并重新加载
                    cache.evict(cacheKey);
                }
            } catch (Exception e) {
                // 反序列化失败，清除缓存
                cache.evict(cacheKey);
            }
        }

        // 获取锁
        Lock lock = getLock("myMoments:" + cacheKey);
        lock.lock();
        try {
            // 第二次检查缓存（双重检测）
            wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                try {
                    Object cachedValue = wrapper.get();
                    if (cachedValue instanceof PageResult) {
                        return ((PageResult<Moment>) cachedValue).toPage();
                    } else {
                        cache.evict(cacheKey);
                    }
                } catch (Exception e) {
                    cache.evict(cacheKey);
                }
            }

            // 从数据库加载
            Page<Moment> moments = loadMyMomentsFromDb(userId, pageable);

            // 写入缓存（使用PageResult包装）
            cache.put(cacheKey, PageResult.of(moments));

            return moments;
        } finally {
            lock.unlock();
        }
    }

    private Page<Moment> loadMyMomentsFromDb(Long userId, Pageable pageable) {
        Page<Moment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        batchLoadMomentDetails(moments.getContent());
        return moments;
    }

    public Page<Moment> getUserMoments(String username, Long viewerId, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Moment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        batchLoadMomentDetails(moments.getContent());
        return moments;
    }

    /**
     * 双重检测锁获取Timeline
     */
    public Page<Moment> getAllPublicMoments(Long viewerId, Pageable pageable) {
        String cacheKey = viewerId + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
        Cache cache = redisCacheManager.getCache("timeline");

        if (cache == null) {
            return loadTimelineFromDb(viewerId, pageable);
        }

        // 第一次检查缓存
        Cache.ValueWrapper wrapper = cache.get(cacheKey);
        if (wrapper != null) {
            try {
                Object cachedValue = wrapper.get();
                if (cachedValue instanceof PageResult) {
                    return ((PageResult<Moment>) cachedValue).toPage();
                } else {
                    // 旧格式的缓存，清除并重新加载
                    cache.evict(cacheKey);
                }
            } catch (Exception e) {
                // 反序列化失败，清除缓存
                cache.evict(cacheKey);
            }
        }

        // 获取锁
        Lock lock = getLock("timeline:" + cacheKey);
        lock.lock();
        try {
            // 第二次检查缓存（双重检测）
            wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                try {
                    Object cachedValue = wrapper.get();
                    if (cachedValue instanceof PageResult) {
                        return ((PageResult<Moment>) cachedValue).toPage();
                    } else {
                        cache.evict(cacheKey);
                    }
                } catch (Exception e) {
                    cache.evict(cacheKey);
                }
            }

            // 从数据库加载
            Page<Moment> moments = loadTimelineFromDb(viewerId, pageable);

            // 写入缓存（使用PageResult包装）
            cache.put(cacheKey, PageResult.of(moments));

            return moments;
        } finally {
            lock.unlock();
        }
    }

    private Page<Moment> loadTimelineFromDb(Long viewerId, Pageable pageable) {
        Page<Moment> moments = momentRepository.findAllByOrderByCreatedAtDesc(pageable);
        batchLoadMomentDetails(moments.getContent());
        return moments;
    }

    private void loadUserInfo(Moment moment) {
        userRepository.findById(moment.getUserId()).ifPresent(user -> {
            moment.setUsername(user.getUsername());
            moment.setAvatar(user.getAvatar());
        });
    }

    /**
     * 批量加载 Moment 的关联数据（images 和 user info）
     * 解决 N+1 查询问题：将 1+2N 次查询优化为 1+2 次
     */
    private void batchLoadMomentDetails(List<Moment> moments) {
        if (moments.isEmpty()) {
            return;
        }

        // 1. 批量查询所有 images（1次查询）
        List<Long> momentIds = moments.stream().map(Moment::getId).toList();
        List<MomentImage> allImages = momentImageRepository.findByMomentIdInOrderBySortOrder(momentIds);

        // 按 momentId 分组
        var imagesByMomentId = allImages.stream()
                .collect(java.util.stream.Collectors.groupingBy(MomentImage::getMomentId));

        // 2. 批量查询所有 users（1次查询）
        List<Long> userIds = moments.stream().map(Moment::getUserId).distinct().toList();
        List<User> users = userRepository.findByIdIn(userIds);

        // 构建 userId -> User 的映射
        var userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        // 3. 填充数据
        moments.forEach(moment -> {
            // 设置 images
            moment.setImages(imagesByMomentId.getOrDefault(moment.getId(), new ArrayList<>()));

            // 设置 user info
            User user = userMap.get(moment.getUserId());
            if (user != null) {
                moment.setUsername(user.getUsername());
                moment.setAvatar(user.getAvatar());
            }
        });
    }

    @Transactional
    public Moment updateMoment(Long id, Long userId, String textContent,
                                Visibility visibility, List<Long> visibleUserIds) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Moment not found"));

        if (!moment.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to edit this moment");
        }

        if (textContent != null) {
            moment.setTextContent(textContent);
        }
        if (visibility != null) {
            moment.setVisibility(visibility);

            // Update visibility rules - 批量保存
            visibilityRuleRepository.deleteByMomentId(id);
            if ((visibility == Visibility.VISIBLE_TO || visibility == Visibility.HIDDEN_FROM) && visibleUserIds != null) {
                List<MomentVisibilityRule> rules = new ArrayList<>();
                for (Long targetUserId : visibleUserIds) {
                    MomentVisibilityRule rule = MomentVisibilityRule.builder()
                            .momentId(id)
                            .userId(targetUserId)
                            .build();
                    rules.add(rule);
                }
                visibilityRuleRepository.saveAll(rules);
            }
        }

        return momentRepository.save(moment);
    }

    @Transactional
    public void deleteMoment(Long id, Long userId) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Moment not found"));

        if (!moment.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to delete this moment");
        }

        // Delete files from MinIO
        List<MomentImage> images = momentImageRepository.findByMomentIdOrderBySortOrder(id);
        List<String> urls = new ArrayList<>();
        for (MomentImage image : images) {
            urls.add(image.getImageUrl());
            if (image.getVideoUrl() != null) {
                urls.add(image.getVideoUrl());
            }
        }
        if (!urls.isEmpty()) {
            fileUploadUtils.deleteByUrls(urls);
        }

        momentRepository.deleteById(id);

        // 清除缓存
        cacheInvalidationService.onMomentDeleted(moment);
    }

    public boolean canView(Moment moment, Long viewerId) {
        if (moment.getUserId().equals(viewerId)) {
            return true;
        }

        return switch (moment.getVisibility()) {
            case PUBLIC -> true;
            case PRIVATE -> false;
            case VISIBLE_TO -> visibilityRuleRepository.existsByMomentIdAndUserId(moment.getId(), viewerId);
            case HIDDEN_FROM -> !visibilityRuleRepository.existsByMomentIdAndUserId(moment.getId(), viewerId);
            default -> false;
        };
    }

    // Part 6: Share Link methods
    @Transactional
    public MomentShareLink generateShareLink(Long userId) {
        MomentShareLink existing = shareLinkRepository.findByUserId(userId).orElse(null);

        if (existing != null) {
            // Regenerate share code
            existing.setShareCode(UUID.randomUUID().toString().replace("-", ""));
            return shareLinkRepository.save(existing);
        } else {
            MomentShareLink shareLink = MomentShareLink.builder()
                    .userId(userId)
                    .shareCode(UUID.randomUUID().toString().replace("-", ""))
                    .build();
            return shareLinkRepository.save(shareLink);
        }
    }

    public MomentShareLink getMyShareLink(Long userId) {
        return shareLinkRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Share link not found. Please generate one first."));
    }

    public Page<Moment> getMomentsByShareCode(String shareCode, Long viewerId, Pageable pageable) {
        MomentShareLink shareLink = shareLinkRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new RuntimeException("Invalid share code"));

        return getUserMoments(
                userRepository.findById(shareLink.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"))
                        .getUsername(),
                viewerId,
                pageable
        );
    }

    // Part 7: Unread count methods
    public long getUnreadCount(Long userId) {
        MomentReadStatus readStatus = readStatusRepository.findByUserId(userId).orElse(null);

        if (readStatus == null) {
            // 如果用户从未查看过，返回所有非自己的moment数量（优化：使用单个 COUNT 查询）
            return momentRepository.countByUserIdNot(userId);
        }

        return momentRepository.countUnreadMoments(userId, readStatus.getLastReadAt());
    }

    @Transactional
    public void markTimelineAsRead(Long userId) {
        MomentReadStatus readStatus = readStatusRepository.findByUserId(userId).orElse(null);

        if (readStatus == null) {
            readStatus = MomentReadStatus.builder()
                    .userId(userId)
                    .lastReadAt(java.time.LocalDateTime.now())
                    .build();
        } else {
            readStatus.setLastReadAt(java.time.LocalDateTime.now());
        }

        readStatusRepository.save(readStatus);
    }
}