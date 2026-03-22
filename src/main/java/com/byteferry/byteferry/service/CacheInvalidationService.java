package com.byteferry.byteferry.service;

import com.byteferry.byteferry.model.entity.Moment;
import com.byteferry.byteferry.model.enums.Visibility;
import com.byteferry.byteferry.repository.MomentVisibilityRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationService {

    private final CacheManager caffeineCacheManager;
    private final CacheManager redisCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MomentVisibilityRuleRepository visibilityRuleRepository;

    private static final String CACHE_INVALIDATION_STREAM = "cache:invalidation:stream";

    /**
     * 发布缓存失效消息到Redis Stream
     */
    public void publishCacheInvalidation(String cacheType, Set<Long> userIds) {
        Map<String, Object> message = new HashMap<>();
        message.put("cacheType", cacheType);
        message.put("userIds", new ArrayList<>(userIds));
        message.put("timestamp", System.currentTimeMillis());

        ObjectRecord<String, Map<String, Object>> record = StreamRecords.newRecord()
                .ofObject(message)
                .withStreamKey(CACHE_INVALIDATION_STREAM);

        redisTemplate.opsForStream().add(record);

        // 立即在本地执行失效操作
        invalidateCacheLocally(cacheType, userIds);
    }

    /**
     * 本地执行缓存失效
     */
    public void invalidateCacheLocally(String cacheType, Set<Long> userIds) {
        for (Long userId : userIds) {
            // 清除Caffeine缓存
            var caffeineCache = caffeineCacheManager.getCache(cacheType);
            if (caffeineCache != null) {
                // 清除该用户所有分页的缓存
                caffeineCache.evict(userId);
            }

            // 清除Redis缓存（使用SCAN代替KEYS，避免阻塞）
            String cacheKeyPattern = cacheType + "::" + userId + ":*";
            scanAndDelete(cacheKeyPattern);
        }

        log.debug("Invalidated {} cache for {} users", cacheType, userIds.size());
    }

    /**
     * 使用SCAN命令安全地删除匹配的键，避免阻塞Redis
     */
    private void scanAndDelete(String pattern) {
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            var cursor = connection.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build()
            );

            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                connection.del(key);
            }

            try {
                cursor.close();
            } catch (Exception e) {
                log.warn("Error closing cursor", e);
            }

            return null;
        });
    }

    /**
     * 当用户发布新Moment时，清除相关缓存
     */
    public void onMomentCreated(Moment moment) {
        // 1. 清除发布者的myMoments缓存
        Set<Long> myMomentUsers = new HashSet<>();
        myMomentUsers.add(moment.getUserId());
        publishCacheInvalidation("myMoments", myMomentUsers);

        // 2. 清除受影响用户的timeline缓存
        Set<Long> affectedUsers = getAffectedUsersForTimeline(moment);
        if (!affectedUsers.isEmpty()) {
            publishCacheInvalidation("timeline", affectedUsers);
        }
    }

    /**
     * 当用户删除Moment时，清除相关缓存
     */
    public void onMomentDeleted(Moment moment) {
        // 1. 清除发布者的myMoments缓存
        Set<Long> myMomentUsers = new HashSet<>();
        myMomentUsers.add(moment.getUserId());
        publishCacheInvalidation("myMoments", myMomentUsers);

        // 2. 清除受影响用户的timeline缓存
        Set<Long> affectedUsers = getAffectedUsersForTimeline(moment);
        if (!affectedUsers.isEmpty()) {
            publishCacheInvalidation("timeline", affectedUsers);
        }
    }

    /**
     * 根据Moment的可见性规则，获取timeline受影响的用户
     */
    private Set<Long> getAffectedUsersForTimeline(Moment moment) {
        Set<Long> affectedUsers = new HashSet<>();

        switch (moment.getVisibility()) {
            case PUBLIC:
                // PUBLIC的moment所有人都能看到
                // 我们需要清除所有用户的timeline缓存
                // 但为了避免查询所有用户，我们使用通配符清除所有timeline缓存
                clearAllTimelineCache();
                break;

            case VISIBLE_TO:
                // 只有指定用户能看到
                List<Long> visibleToUsers = visibilityRuleRepository.findByMomentId(moment.getId())
                        .stream()
                        .map(rule -> rule.getUserId())
                        .toList();
                affectedUsers.addAll(visibleToUsers);
                break;

            case HIDDEN_FROM:
                // 除了指定用户外都能看到
                // 这种情况类似PUBLIC，需要清除大部分用户的缓存
                // 为了简化，我们也清除所有timeline缓存
                clearAllTimelineCache();
                break;

            case PRIVATE:
                // 私密的moment，不影响任何人的timeline
                break;
        }

        return affectedUsers;
    }

    /**
     * 清除所有用户的timeline缓存
     */
    private void clearAllTimelineCache() {
        // 清除所有Caffeine缓存
        var caffeineCache = caffeineCacheManager.getCache("timeline");
        if (caffeineCache != null) {
            caffeineCache.clear();
        }

        // 使用SCAN清除所有Redis缓存
        String cacheKeyPattern = "timeline::*";
        int deletedCount = scanAndDeleteWithCount(cacheKeyPattern);
        log.debug("Cleared all timeline cache, {} keys deleted", deletedCount);
    }

    /**
     * 使用SCAN命令删除并返回删除数量
     */
    private int scanAndDeleteWithCount(String pattern) {
        return redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Integer>) connection -> {
            int count = 0;
            var cursor = connection.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build()
            );

            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                connection.del(key);
                count++;
            }

            try {
                cursor.close();
            } catch (Exception e) {
                log.warn("Error closing cursor", e);
            }

            return count;
        });
    }
}
