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
        // Scan all user sets for stale session references
        Set<String> keys = redisTemplate.keys(USER_SET_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return;

        for (String userSetKey : keys) {
            Set<Object> sessionIds = redisTemplate.opsForSet().members(userSetKey);
            if (sessionIds == null) continue;

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
    }
}
