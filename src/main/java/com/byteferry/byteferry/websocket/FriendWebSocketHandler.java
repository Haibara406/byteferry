package com.byteferry.byteferry.websocket;

import com.byteferry.byteferry.config.JwtUtil;
import com.byteferry.byteferry.model.entity.Friendship;
import com.byteferry.byteferry.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_ATTR = "userId";

    private final JwtUtil jwtUtil;
    private final FriendshipRepository friendshipRepository;

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        if (userId == null) {
            try { session.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            return;
        }
        session.getAttributes().put(USER_ID_ATTR, userId);

        // Single-device enforcement: close existing sessions for this user
        Set<WebSocketSession> existing = userSessions.get(userId);
        if (existing != null) {
            for (WebSocketSession old : existing) {
                if (old.isOpen() && !old.getId().equals(session.getId())) {
                    try {
                        old.sendMessage(new TextMessage("{\"type\":\"session_replaced\",\"message\":\"Connected from another device\"}"));
                        old.close(CloseStatus.POLICY_VIOLATION);
                    } catch (IOException ignored) {}
                }
            }
            existing.clear();
        }

        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        broadcastOnlineStatus(userId, true);
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
                    broadcastOnlineStatus(userId, false);
                }
            }
        }
    }

    public void notifyFriendRequest(Long targetUserId, Long fromUserId, String fromUsername) {
        sendJson(targetUserId, "{\"type\":\"friend_request\",\"fromUserId\":" + fromUserId + ",\"fromUsername\":\"" + fromUsername + "\"}");
    }

    public void notifyFriendAccepted(Long targetUserId, Long byUserId, String byUsername) {
        sendJson(targetUserId, "{\"type\":\"friend_accepted\",\"userId\":" + byUserId + ",\"username\":\"" + byUsername + "\"}");
    }

    public void notifyFriendRemoved(Long targetUserId) {
        sendJson(targetUserId, "{\"type\":\"friend_removed\"}");
    }

    public void notifySessionCreated(Long targetUserId, String sessionId, String initiatorUsername) {
        sendJson(targetUserId, "{\"type\":\"session_created\",\"sessionId\":\"" + sessionId + "\",\"username\":\"" + initiatorUsername + "\"}");
    }

    public void notifySessionUpdate(Long targetUserId, String sessionId) {
        sendJson(targetUserId, "{\"type\":\"session_update\",\"sessionId\":\"" + sessionId + "\"}");
    }

    public void notifySessionClosed(Long targetUserId, String sessionId) {
        sendJson(targetUserId, "{\"type\":\"session_closed\",\"sessionId\":\"" + sessionId + "\"}");
    }

    // ==================== Invitation Events ====================

    public void notifySessionInvitation(Long targetUserId, String invitationId, String sessionId,
                                         Long fromUserId, String fromUsername, int expireSeconds) {
        sendJson(targetUserId, "{\"type\":\"session_invitation\",\"invitationId\":\"" + invitationId
                + "\",\"sessionId\":\"" + sessionId
                + "\",\"fromUserId\":" + fromUserId
                + ",\"fromUsername\":\"" + fromUsername
                + "\",\"expireSeconds\":" + expireSeconds + "}");
    }

    public void notifyInvitationAccepted(Long targetUserId, String sessionId, Long byUserId, String byUsername) {
        sendJson(targetUserId, "{\"type\":\"invitation_accepted\",\"sessionId\":\"" + sessionId
                + "\",\"userId\":" + byUserId
                + ",\"username\":\"" + byUsername + "\"}");
    }

    public void notifyInvitationDeclined(Long targetUserId, String invitationId, Long byUserId, String byUsername) {
        sendJson(targetUserId, "{\"type\":\"invitation_declined\",\"invitationId\":\"" + invitationId
                + "\",\"byUserId\":" + byUserId
                + ",\"byUsername\":\"" + byUsername + "\"}");
    }

    public void notifyMemberJoined(Long targetUserId, String sessionId, Long newUserId, String newUsername) {
        sendJson(targetUserId, "{\"type\":\"session_member_joined\",\"sessionId\":\"" + sessionId
                + "\",\"userId\":" + newUserId
                + ",\"username\":\"" + newUsername + "\"}");
    }

    public void notifyMemberLeft(Long targetUserId, String sessionId, Long leftUserId, String leftUsername) {
        sendJson(targetUserId, "{\"type\":\"session_member_left\",\"sessionId\":\"" + sessionId
                + "\",\"userId\":" + leftUserId
                + ",\"username\":\"" + leftUsername + "\"}");
    }

    public void notifyMemberKicked(Long kickedUserId, String sessionId, String adminUsername) {
        sendJson(kickedUserId, "{\"type\":\"session_member_kicked\",\"sessionId\":\"" + sessionId
                + "\",\"kickedBy\":\"" + adminUsername + "\"}");
    }

    public void notifyAdminTransferred(Long newAdminId, String sessionId, String oldAdminUsername) {
        sendJson(newAdminId, "{\"type\":\"admin_transferred\",\"sessionId\":\"" + sessionId
                + "\",\"oldAdminUsername\":\"" + oldAdminUsername + "\"}");
    }

    public boolean isUserOnline(Long userId) {
        Set<WebSocketSession> s = userSessions.get(userId);
        return s != null && !s.isEmpty();
    }

    public Map<Long, Boolean> getOnlineStatuses(Collection<Long> userIds) {
        Map<Long, Boolean> result = new ConcurrentHashMap<>();
        for (Long uid : userIds) result.put(uid, isUserOnline(uid));
        return result;
    }

    private void broadcastOnlineStatus(Long userId, boolean online) {
        String msg = "{\"type\":\"online_status\",\"userId\":" + userId + ",\"online\":" + online + "}";
        for (Friendship f : friendshipRepository.findByUserIdAndStatus(userId, Friendship.Status.ACCEPTED)) {
            sendJson(f.getFriendId(), msg);
        }
    }

    private void sendJson(Long userId, String json) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try { s.sendMessage(msg); } catch (IOException e) {
                    log.warn("Failed to send WS to userId={}", userId, e);
                }
            }
        }
    }

    private Long extractUserId(WebSocketSession session) {
        try {
            String query = Objects.requireNonNull(session.getUri()).getQuery();
            String token = UriComponentsBuilder.newInstance()
                    .query(query).build().getQueryParams().getFirst("token");
            if (token != null && jwtUtil.isValid(token)) {
                return jwtUtil.getUserId(token);
            }
        } catch (Exception e) { log.warn("WS auth failed", e); }
        return null;
    }
}