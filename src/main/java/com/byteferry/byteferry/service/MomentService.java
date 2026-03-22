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

    // 为每个缓存键维护一个锁
    private final ConcurrentHashMap<String, Lock> lockMap = new ConcurrentHashMap<>();

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

        // Handle visibility rules
        if ((visibility == Visibility.VISIBLE_TO || visibility == Visibility.HIDDEN_FROM) && visibleUserIds != null) {
            for (Long targetUserId : visibleUserIds) {
                MomentVisibilityRule rule = MomentVisibilityRule.builder()
                        .momentId(moment.getId())
                        .userId(targetUserId)
                        .build();
                visibilityRuleRepository.save(rule);
            }
        }

        // Handle regular images
        if (images != null && images.length > 0) {
            for (int i = 0; i < Math.min(images.length, 9); i++) {
                String imageUrl = fileUploadUtils.upload(UploadEnum.MOMENT_IMAGE, images[i]);
                MomentImage momentImage = MomentImage.builder()
                        .momentId(moment.getId())
                        .imageUrl(imageUrl)
                        .isLivePhoto(false)
                        .sortOrder(i)
                        .build();
                momentImageRepository.save(momentImage);
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
                momentImageRepository.save(momentImage);
            }
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
     * 获取缓存键对应的锁
     */
    private Lock getLock(String cacheKey) {
        return lockMap.computeIfAbsent(cacheKey, k -> new ReentrantLock());
    }

    /**
     * 双重检测锁获取My Moments
     */
    public Page<Moment> getMyMoments(Long userId, Pageable pageable) {
        String cacheKey = userId + ":" + pageable.getPageNumber();
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
        moments.forEach(m -> {
            m.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(m.getId()));
            loadUserInfo(m);
        });
        return moments;
    }

    public Page<Moment> getUserMoments(String username, Long viewerId, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Moment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        // Load images and user info for all moments
        moments.forEach(moment -> {
            moment.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(moment.getId()));
            loadUserInfo(moment);
        });

        return moments;
    }

    /**
     * 双重检测锁获取Timeline
     */
    public Page<Moment> getAllPublicMoments(Long viewerId, Pageable pageable) {
        String cacheKey = viewerId + ":" + pageable.getPageNumber();
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

        // Load images and user info for all moments
        moments.forEach(moment -> {
            moment.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(moment.getId()));
            loadUserInfo(moment);
        });

        return moments;
    }

    private void loadUserInfo(Moment moment) {
        userRepository.findById(moment.getUserId()).ifPresent(user -> {
            moment.setUsername(user.getUsername());
            moment.setAvatar(user.getAvatar());
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

        if (textContent != null) moment.setTextContent(textContent);
        if (visibility != null) {
            moment.setVisibility(visibility);

            // Update visibility rules
            visibilityRuleRepository.deleteByMomentId(id);
            if ((visibility == Visibility.VISIBLE_TO || visibility == Visibility.HIDDEN_FROM) && visibleUserIds != null) {
                for (Long targetUserId : visibleUserIds) {
                    MomentVisibilityRule rule = MomentVisibilityRule.builder()
                            .momentId(id)
                            .userId(targetUserId)
                            .build();
                    visibilityRuleRepository.save(rule);
                }
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
        if (moment.getUserId().equals(viewerId)) return true;

        switch (moment.getVisibility()) {
            case PUBLIC:
                return true;
            case PRIVATE:
                return false;
            case VISIBLE_TO:
                return visibilityRuleRepository.existsByMomentIdAndUserId(moment.getId(), viewerId);
            case HIDDEN_FROM:
                return !visibilityRuleRepository.existsByMomentIdAndUserId(moment.getId(), viewerId);
            default:
                return false;
        }
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
            // 如果用户从未查看过，返回所有非自己的moment数量
            return momentRepository.count() - momentRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged()).getTotalElements();
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
