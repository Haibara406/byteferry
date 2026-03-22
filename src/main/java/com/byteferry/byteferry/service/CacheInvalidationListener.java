package com.byteferry.byteferry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis Stream监听器，用于接收缓存失效消息
 * 当有多个应用实例时，可以通过这个监听器同步缓存失效
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener implements StreamListener<String, MapRecord<String, String, Object>> {

    private final CacheInvalidationService cacheInvalidationService;

    @Override
    public void onMessage(MapRecord<String, String, Object> message) {
        try {
            Map<String, Object> body = message.getValue();
            String cacheType = (String) body.get("cacheType");
            List<Integer> userIdList = (List<Integer>) body.get("userIds");

            if (cacheType != null && userIdList != null) {
                Set<Long> userIds = new HashSet<>();
                for (Integer userId : userIdList) {
                    userIds.add(userId.longValue());
                }

                // 执行本地缓存失效
                cacheInvalidationService.invalidateCacheLocally(cacheType, userIds);

                log.debug("Processed cache invalidation message: type={}, users={}", cacheType, userIds.size());
            }
        } catch (Exception e) {
            log.error("Error processing cache invalidation message", e);
        }
    }
}
