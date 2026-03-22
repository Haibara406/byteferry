package com.byteferry.byteferry.task;

import com.byteferry.byteferry.model.FriendSessionData;
import com.byteferry.byteferry.model.entity.FriendSessionHistory;
import com.byteferry.byteferry.repository.FriendSessionHistoryRepository;
import com.byteferry.byteferry.service.FriendSessionService;
import com.byteferry.byteferry.websocket.FriendWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendSessionCleanupTask {

    private static final String USER_SET_PREFIX = "fsession-user:";
    private static final String KEY_PREFIX = "fsession:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final FriendSessionHistoryRepository historyRepository;
    private final FriendWebSocketHandler friendWsHandler;

    @Scheduled(fixedRate = 30000)
    public void cleanupExpiredSessions() {
        // 使用SCAN代替KEYS，避免阻塞Redis
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            var cursor = connection.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(USER_SET_PREFIX + "*")
                    .count(100)
                    .build()
            );

            while (cursor.hasNext()) {
                byte[] keyBytes = cursor.next();
                String userSetKey = new String(keyBytes);

                Set<Object> sessionIds = redisTemplate.opsForSet().members(userSetKey);
                if (sessionIds == null) {
                    continue;
                }

                for (Object sidObj : sessionIds) {
                    String sid = sidObj.toString();
                    Boolean exists = redisTemplate.hasKey(KEY_PREFIX + sid);
                    if (Boolean.FALSE.equals(exists)) {
                        // Session expired from Redis (TTL), clean up reference
                        redisTemplate.opsForSet().remove(userSetKey, sid);
                        log.debug("Removed stale friend session reference: {}", sid);
                    }
                }
            }

            try {
                cursor.close();
            } catch (Exception e) {
                log.warn("Error closing cursor", e);
            }

            return null;
        });
    }
}