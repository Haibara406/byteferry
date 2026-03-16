package com.byteferry.byteferry.websocket;

import com.byteferry.byteferry.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_ATTR = "userId";

    private final JwtUtil jwtUtil;

    // userId -> set of connected sessions
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        if (userId == null) {
            try { session.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            return;
        }
        session.getAttributes().put(USER_ID_ATTR, userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Space WS connected: userId={}, sessions={}", userId, userSessions.get(userId).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(USER_ID_ATTR);
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
    }

    public void notifyUser(Long userId) {
        sendToUser(userId, "refresh");
    }

    public void notifyEmpty(Long userId) {
        sendToUser(userId, "empty");
    }

    private void sendToUser(Long userId, String text) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        TextMessage msg = new TextMessage(text);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(msg);
                } catch (IOException e) {
                    log.warn("Failed to send WS message to userId={}", userId, e);
                }
            }
        }
    }

    private Long extractUserId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            String token = UriComponentsBuilder.newInstance()
                    .query(query).build().getQueryParams().getFirst("token");
            if (token != null && jwtUtil.isValid(token)) {
                return jwtUtil.getUserId(token);
            }
        } catch (Exception e) {
            log.warn("WS auth failed", e);
        }
        return null;
    }
}
