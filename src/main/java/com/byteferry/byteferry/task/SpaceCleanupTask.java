package com.byteferry.byteferry.task;

import com.byteferry.byteferry.service.SpaceService;
import com.byteferry.byteferry.websocket.SpaceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceCleanupTask {

    private final SpaceService spaceService;
    private final SpaceWebSocketHandler spaceWsHandler;

    @Scheduled(fixedRate = 30000)
    public void cleanupExpiredItems() {
        List<Long> affectedUserIds = spaceService.cleanupExpired();
        for (Long userId : affectedUserIds) {
            if (spaceService.hasActiveItems(userId)) {
                spaceWsHandler.notifyUser(userId);
            } else {
                spaceWsHandler.notifyEmpty(userId);
            }
        }
        if (!affectedUserIds.isEmpty()) {
            log.debug("Cleaned up expired space items for {} users", affectedUserIds.size());
        }
    }
}
