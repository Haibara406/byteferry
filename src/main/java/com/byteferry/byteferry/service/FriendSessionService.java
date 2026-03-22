package com.byteferry.byteferry.service;

import com.byteferry.byteferry.model.FriendSessionData;
import com.byteferry.byteferry.model.SessionInvitation;
import com.byteferry.byteferry.model.ShareData;
import com.byteferry.byteferry.model.entity.FriendSessionHistory;
import com.byteferry.byteferry.model.entity.User;
import com.byteferry.byteferry.repository.FriendSessionHistoryRepository;
import com.byteferry.byteferry.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendSessionService {

    private static final String KEY_PREFIX = "fsession:";
    private static final String USER_SET_PREFIX = "fsession-user:";
    private static final String INVITE_PREFIX = "finvite:";
    private static final String INVITE_USER_PREFIX = "finvite-user:";
    private static final int INVITE_TTL_SECONDS = 300; // 5 minutes to respond

    private final RedisTemplate<String, Object> redisTemplate;
    private final FileStorageService fileStorageService;
    private final FriendService friendService;
    private final FriendSessionHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Temporary: keep old createSession for backward compatibility during migration
    // Will be replaced by invitation system in Step 3
    public FriendSessionData createSession(Long initiatorId, Long friendId, int expireSeconds) {
        if (!friendService.areFriends(initiatorId, friendId)) {
            throw new RuntimeException("Not friends");
        }
        expireSeconds = Math.min(Math.max(expireSeconds, 60), 7200);

        User userA = userRepository.findById(initiatorId).orElseThrow(() -> new RuntimeException("User not found"));
        User userB = userRepository.findById(friendId).orElseThrow(() -> new RuntimeException("Friend not found"));

        String sessionId = UUID.randomUUID().toString();

        // Create participants list
        List<FriendSessionData.Participant> participants = new ArrayList<>();
        participants.add(FriendSessionData.Participant.builder()
                .userId(initiatorId)
                .username(userA.getUsername())
                .role(FriendSessionData.Role.ADMIN)
                .inviteAllowed(true)
                .joinedAt(LocalDateTime.now())
                .build());
        participants.add(FriendSessionData.Participant.builder()
                .userId(friendId)
                .username(userB.getUsername())
                .role(FriendSessionData.Role.MEMBER)
                .inviteAllowed(true)
                .joinedAt(LocalDateTime.now())
                .build());

        FriendSessionData session = FriendSessionData.builder()
                .sessionId(sessionId)
                .adminId(initiatorId)
                .adminUsername(userA.getUsername())
                .status(FriendSessionData.Status.ACTIVE)
                .globalInviteEnabled(true)
                .createdAt(LocalDateTime.now())
                .activatedAt(LocalDateTime.now())
                .expireSeconds(expireSeconds)
                .participants(participants)
                .items(new ArrayList<>())
                .nextItemId(0)
                .build();

        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, session, expireSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(USER_SET_PREFIX + initiatorId, sessionId);
        redisTemplate.opsForSet().add(USER_SET_PREFIX + friendId, sessionId);
        return session;
    }

    public FriendSessionData getSession(String sessionId) {
        Object obj = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
        return obj != null ? (FriendSessionData) obj : null;
    }

    public FriendSessionData getActiveSession(String sessionId, Long userId) {
        FriendSessionData s = getSession(sessionId);
        if (s == null) throw new RuntimeException("Session not found or expired");

        // Check if user is a participant
        boolean isParticipant = s.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (!isParticipant) {
            throw new RuntimeException("Access denied");
        }

        if (s.getStatus() != FriendSessionData.Status.ACTIVE) {
            throw new RuntimeException("Session is closed");
        }
        return s;
    }

    public FriendSessionData.FriendSessionItem addTextItem(String sessionId, Long senderId, String senderUsername, String content) {
        FriendSessionData s = getActiveSession(sessionId, senderId);
        autoActivateIfNeeded(s);
        FriendSessionData.FriendSessionItem item = FriendSessionData.FriendSessionItem.builder()
                .id(s.getNextItemId())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .type(ShareData.Type.TEXT)
                .content(content)
                .addedAt(LocalDateTime.now())
                .build();
        s.getItems().add(item);
        s.setNextItemId(s.getNextItemId() + 1);
        savePreserveTTL(s);
        return item;
    }

    public FriendSessionData.FriendSessionItem addFileItem(String sessionId, Long senderId, String senderUsername, MultipartFile[] files) throws IOException {
        FriendSessionData s = getActiveSession(sessionId, senderId);
        autoActivateIfNeeded(s);

        List<ShareData.FileInfo> fileInfos = new ArrayList<>();
        boolean allImages = true;
        for (MultipartFile f : files) {
            String path = fileStorageService.store(f);
            String ct = f.getContentType();
            if (ct == null || !ct.startsWith("image/")) allImages = false;
            fileInfos.add(ShareData.FileInfo.builder()
                    .fileName(f.getOriginalFilename())
                    .filePath(path)
                    .fileSize(f.getSize())
                    .mimeType(ct)
                    .build());
        }

        FriendSessionData.FriendSessionItem item = FriendSessionData.FriendSessionItem.builder()
                .id(s.getNextItemId())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .type(allImages ? ShareData.Type.IMAGE : ShareData.Type.FILE)
                .files(fileInfos)
                .addedAt(LocalDateTime.now())
                .build();
        s.getItems().add(item);
        s.setNextItemId(s.getNextItemId() + 1);
        savePreserveTTL(s);
        return item;
    }

    public void closeSession(String sessionId, Long userId) {
        FriendSessionData s = getSession(sessionId);
        if (s == null) {
            log.warn("closeSession: session {} not found in Redis (may have expired)", sessionId);
            throw new RuntimeException("Session not found or expired");
        }

        // Only admin can close the session
        if (!s.getAdminId().equals(userId)) {
            throw new RuntimeException("Only the admin can close the session");
        }

        closeSession(s);
    }

    /**
     * Close session using pre-fetched session data (avoids double Redis read).
     */
    public void closeSession(FriendSessionData s) {
        String sessionId = s.getSessionId();

        // Serialize items to JSON for history (keep files on disk for historical access)
        String itemsJson = null;
        if (s.getItems() != null && !s.getItems().isEmpty()) {
            try {
                itemsJson = objectMapper.writeValueAsString(s.getItems());
            } catch (Exception e) {
                log.error("Failed to serialize items for session {}: {}", sessionId, e.getMessage());
            }
        }

        // Save history for each participant
        String participantNames = s.getParticipants() != null && !s.getParticipants().isEmpty()
                ? s.getParticipants().stream()
                    .map(FriendSessionData.Participant::getUsername)
                    .collect(Collectors.joining(", "))
                : "Unknown";

        int itemCount = s.getItems() != null ? s.getItems().size() : 0;
        int participantCount = s.getParticipants() != null ? s.getParticipants().size() : 0;

        if (s.getParticipants() != null) {
            for (FriendSessionData.Participant p : s.getParticipants()) {
                try {
                    historyRepository.save(FriendSessionHistory.builder()
                            .sessionId(sessionId)
                            .userId(p.getUserId())
                            .adminUsername(s.getAdminUsername())
                            .participants(participantNames)
                            .participantCount(participantCount)
                            .itemCount(itemCount)
                            .expireSeconds(s.getExpireSeconds())
                            .createdAt(s.getCreatedAt())
                            .closedAt(LocalDateTime.now())
                            .itemsJson(itemsJson)
                            .build());
                    log.info("Saved history for session {} user {}", sessionId, p.getUserId());
                } catch (Exception e) {
                    log.error("Failed to save history for session {} user {}: {}", sessionId, p.getUserId(), e.getMessage(), e);
                }
                // Prune old history (keep only 10 most recent per user)
                pruneHistory(p.getUserId());
            }
        }

        // Delete the session from Redis
        Boolean deleted = redisTemplate.delete(KEY_PREFIX + sessionId);
        log.info("Deleted session {} from Redis: {}", sessionId, deleted);

        // Then remove from all participants' user sets
        if (s.getParticipants() != null) {
            for (FriendSessionData.Participant p : s.getParticipants()) {
                redisTemplate.opsForSet().remove(USER_SET_PREFIX + p.getUserId(), sessionId);
            }
        }
    }

    public long getRemainingSeconds(String sessionId) {
        Long ttl = redisTemplate.getExpire(KEY_PREFIX + sessionId, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    public List<FriendSessionData> getActiveSessions(Long userId) {
        Set<Object> ids = redisTemplate.opsForSet().members(USER_SET_PREFIX + userId);
        if (ids == null || ids.isEmpty()) return List.of();
        List<FriendSessionData> result = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        for (Object id : ids) {
            String sessionId = id.toString();
            // Force fresh read from Redis
            Object obj = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);

            if (obj == null) {
                // Session doesn't exist, mark for cleanup
                toRemove.add(sessionId);
                continue;
            }

            FriendSessionData s = (FriendSessionData) obj;
            if (s.getStatus() == FriendSessionData.Status.ACTIVE) {
                result.add(s);
            } else {
                // Session exists but not active, mark for cleanup
                toRemove.add(sessionId);
            }
        }

        // Clean up all stale references in one go
        if (!toRemove.isEmpty()) {
            for (String sessionId : toRemove) {
                redisTemplate.opsForSet().remove(USER_SET_PREFIX + userId, sessionId);
            }
        }

        return result;
    }

    public List<FriendSessionHistory> getHistory(Long userId) {
        return historyRepository.findByUserIdOrderByClosedAtDesc(userId);
    }

    public FriendSessionHistory getHistoryDetail(Long userId, Long historyId) {
        FriendSessionHistory h = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History record not found"));
        if (!h.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return h;
    }

    private void pruneHistory(Long userId) {
        List<FriendSessionHistory> all = historyRepository.findByUserIdOrderByClosedAtDesc(userId);
        if (all.size() <= 10) return;

        List<FriendSessionHistory> toDelete = new ArrayList<>(all.subList(10, all.size()));

        // 批量删除数据库记录
        List<Long> idsToDelete = toDelete.stream().map(FriendSessionHistory::getId).toList();
        historyRepository.deleteByIdIn(idsToDelete);

        // 删除文件
        for (FriendSessionHistory h : toDelete) {
            String sid = h.getSessionId();
            // Only delete files when no other user still references this session
            if (historyRepository.countBySessionId(sid) == 0) {
                deleteHistoryFiles(h.getItemsJson());
            }
        }
        log.info("Pruned {} old history records for user {}", toDelete.size(), userId);
    }

    private void deleteHistoryFiles(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) return;
        try {
            List<FriendSessionData.FriendSessionItem> items = objectMapper.readValue(
                    itemsJson,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, FriendSessionData.FriendSessionItem.class));
            for (FriendSessionData.FriendSessionItem item : items) {
                if (item.getFiles() != null) {
                    for (ShareData.FileInfo fi : item.getFiles()) {
                        fileStorageService.delete(fi.getFilePath());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse itemsJson for file cleanup: {}", e.getMessage());
        }
    }

    // ==================== Activation ====================

    /**
     * Admin manually activates the session. Sets activatedAt and real TTL.
     */
    public void activateSession(String sessionId, Long adminId) {
        FriendSessionData s = getActiveSession(sessionId, adminId);
        if (!s.getAdminId().equals(adminId)) {
            throw new RuntimeException("Only the admin can activate the session");
        }
        if (s.getActivatedAt() != null) {
            throw new RuntimeException("Session is already activated");
        }
        s.setActivatedAt(LocalDateTime.now());
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, s, s.getExpireSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Auto-activate if not yet activated (called when first message is sent).
     */
    private void autoActivateIfNeeded(FriendSessionData s) {
        if (s.getActivatedAt() == null) {
            s.setActivatedAt(LocalDateTime.now());
            redisTemplate.opsForValue().set(KEY_PREFIX + s.getSessionId(), s, s.getExpireSeconds(), TimeUnit.SECONDS);
        }
    }

    // ==================== Multi-User Session Management ====================

    public void kickMember(String sessionId, Long adminId, Long targetId) {
        FriendSessionData s = getActiveSession(sessionId, adminId);
        if (!s.getAdminId().equals(adminId)) {
            throw new RuntimeException("Only the admin can kick members");
        }
        if (adminId.equals(targetId)) {
            throw new RuntimeException("Cannot kick yourself");
        }
        s.getParticipants().removeIf(p -> p.getUserId().equals(targetId));
        savePreserveTTL(s);
        redisTemplate.opsForSet().remove(USER_SET_PREFIX + targetId, sessionId);
    }

    public void leaveSession(String sessionId, Long userId) {
        FriendSessionData s = getActiveSession(sessionId, userId);

        // Admin cannot leave directly — must transfer or close
        if (s.getAdminId().equals(userId)) {
            if (s.getParticipants().size() <= 1) {
                // Admin is the only one left, just close
                closeSession(s);
                return;
            }
            throw new RuntimeException("Admin cannot leave. Transfer admin role first or close the session.");
        }

        s.getParticipants().removeIf(p -> p.getUserId().equals(userId));
        redisTemplate.opsForSet().remove(USER_SET_PREFIX + userId, sessionId);

        if (s.getParticipants().isEmpty()) {
            closeSession(s);
            return;
        }
        savePreserveTTL(s);
    }

    public void transferAdmin(String sessionId, Long adminId, Long newAdminId) {
        FriendSessionData s = getActiveSession(sessionId, adminId);
        if (!s.getAdminId().equals(adminId)) {
            throw new RuntimeException("Only the admin can transfer admin role");
        }
        if (adminId.equals(newAdminId)) {
            throw new RuntimeException("Cannot transfer to yourself");
        }
        FriendSessionData.Participant newAdmin = s.getParticipants().stream()
                .filter(p -> p.getUserId().equals(newAdminId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Target user is not a participant"));

        // Update roles
        s.getParticipants().stream()
                .filter(p -> p.getUserId().equals(adminId))
                .findFirst()
                .ifPresent(p -> p.setRole(FriendSessionData.Role.MEMBER));
        newAdmin.setRole(FriendSessionData.Role.ADMIN);
        s.setAdminId(newAdminId);
        s.setAdminUsername(newAdmin.getUsername());

        // Remove old admin from session
        s.getParticipants().removeIf(p -> p.getUserId().equals(adminId));
        redisTemplate.opsForSet().remove(USER_SET_PREFIX + adminId, sessionId);

        savePreserveTTL(s);
    }

    public void toggleGlobalInvite(String sessionId, Long adminId, boolean enabled) {
        FriendSessionData s = getActiveSession(sessionId, adminId);
        if (!s.getAdminId().equals(adminId)) {
            throw new RuntimeException("Only the admin can change invite permissions");
        }
        s.setGlobalInviteEnabled(enabled);
        savePreserveTTL(s);
    }

    public void toggleMemberInvite(String sessionId, Long adminId, Long memberId, boolean enabled) {
        FriendSessionData s = getActiveSession(sessionId, adminId);
        if (!s.getAdminId().equals(adminId)) {
            throw new RuntimeException("Only the admin can change invite permissions");
        }
        s.getParticipants().stream()
                .filter(p -> p.getUserId().equals(memberId))
                .findFirst()
                .ifPresent(p -> p.setInviteAllowed(enabled));
        savePreserveTTL(s);
    }

    // ==================== Invitation System ====================

    /**
     * Create a new session invitation. Creates the session in Redis (no TTL yet)
     * and sends an invitation to the target user.
     */
    public SessionInvitation createInvitation(Long fromUserId, String fromUsername, Long toUserId, int expireSeconds, String sessionName) {
        if (!friendService.areFriends(fromUserId, toUserId)) {
            throw new RuntimeException("Not friends");
        }
        expireSeconds = Math.min(Math.max(expireSeconds, 60), 7200);

        User fromUser = userRepository.findById(fromUserId).orElseThrow(() -> new RuntimeException("User not found"));

        // Create the session (no TTL yet — TTL starts when invitee accepts)
        String sessionId = UUID.randomUUID().toString();
        FriendSessionData session = FriendSessionData.builder()
                .sessionId(sessionId)
                .sessionName(sessionName != null && !sessionName.isBlank() ? sessionName : "Session")
                .adminId(fromUserId)
                .adminUsername(fromUser.getUsername())
                .status(FriendSessionData.Status.ACTIVE)
                .globalInviteEnabled(true)
                .createdAt(LocalDateTime.now())
                .expireSeconds(expireSeconds)
                .participants(new ArrayList<>(List.of(
                        FriendSessionData.Participant.builder()
                                .userId(fromUserId)
                                .username(fromUser.getUsername())
                                .role(FriendSessionData.Role.ADMIN)
                                .inviteAllowed(true)
                                .joinedAt(LocalDateTime.now())
                                .build()
                )))
                .items(new ArrayList<>())
                .nextItemId(0)
                .build();

        // Store session without TTL initially (use a long TTL as safety net: invite TTL + session TTL)
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, session, INVITE_TTL_SECONDS + expireSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(USER_SET_PREFIX + fromUserId, sessionId);

        // Create invitation
        String invitationId = UUID.randomUUID().toString();
        SessionInvitation invitation = SessionInvitation.builder()
                .invitationId(invitationId)
                .sessionId(sessionId)
                .sessionName(session.getSessionName())
                .fromUserId(fromUserId)
                .fromUsername(fromUsername)
                .toUserId(toUserId)
                .expireSeconds(expireSeconds)
                .status(SessionInvitation.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        redisTemplate.opsForValue().set(INVITE_PREFIX + invitationId, invitation, INVITE_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(INVITE_USER_PREFIX + toUserId, invitationId);

        return invitation;
    }

    /**
     * Invite a friend to an existing active session.
     * Caller must be a participant with invite permission.
     */
    public SessionInvitation inviteToExistingSession(String sessionId, Long inviterId, String inviterUsername, Long friendId) {
        FriendSessionData s = getActiveSession(sessionId, inviterId);

        if (!friendService.areFriends(inviterId, friendId)) {
            throw new RuntimeException("Not friends");
        }

        // Check invite permission
        FriendSessionData.Participant inviter = s.getParticipants().stream()
                .filter(p -> p.getUserId().equals(inviterId))
                .findFirst().orElseThrow(() -> new RuntimeException("Access denied"));

        if (inviter.getRole() != FriendSessionData.Role.ADMIN) {
            if (!s.isGlobalInviteEnabled() || !inviter.isInviteAllowed()) {
                throw new RuntimeException("You do not have permission to invite");
            }
        }

        // Check if already a participant
        boolean alreadyIn = s.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(friendId));
        if (alreadyIn) {
            throw new RuntimeException("User is already in this session");
        }

        // Create invitation pointing to existing session
        String invitationId = UUID.randomUUID().toString();
        SessionInvitation invitation = SessionInvitation.builder()
                .invitationId(invitationId)
                .sessionId(sessionId)
                .sessionName(s.getSessionName())
                .fromUserId(inviterId)
                .fromUsername(inviterUsername)
                .toUserId(friendId)
                .expireSeconds(s.getExpireSeconds())
                .status(SessionInvitation.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        redisTemplate.opsForValue().set(INVITE_PREFIX + invitationId, invitation, INVITE_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(INVITE_USER_PREFIX + friendId, invitationId);

        return invitation;
    }

    /**
     * Accept an invitation. Adds the user to the session.
     * If this is the first invitee, activatedAt is set and TTL starts.
     */
    public FriendSessionData acceptInvitation(Long userId, String invitationId) {
        SessionInvitation inv = getInvitation(invitationId);
        if (inv == null) throw new RuntimeException("Invitation not found or expired");
        if (!inv.getToUserId().equals(userId)) throw new RuntimeException("Access denied");
        if (inv.getStatus() != SessionInvitation.Status.PENDING) throw new RuntimeException("Invitation already handled");

        FriendSessionData s = getSession(inv.getSessionId());
        if (s == null) throw new RuntimeException("Session no longer exists");
        if (s.getStatus() != FriendSessionData.Status.ACTIVE) throw new RuntimeException("Session is closed");

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Add participant
        s.getParticipants().add(FriendSessionData.Participant.builder()
                .userId(userId)
                .username(user.getUsername())
                .role(FriendSessionData.Role.MEMBER)
                .inviteAllowed(true)
                .joinedAt(LocalDateTime.now())
                .build());

        // If first invitee joins, set activatedAt and start real TTL
        if (s.getActivatedAt() == null) {
            s.setActivatedAt(LocalDateTime.now());
            redisTemplate.opsForValue().set(KEY_PREFIX + s.getSessionId(), s, s.getExpireSeconds(), TimeUnit.SECONDS);
        } else {
            savePreserveTTL(s);
        }

        // Register in user set
        redisTemplate.opsForSet().add(USER_SET_PREFIX + userId, s.getSessionId());

        // Mark invitation as accepted
        inv.setStatus(SessionInvitation.Status.ACCEPTED);
        redisTemplate.opsForValue().set(INVITE_PREFIX + invitationId, inv, 10, TimeUnit.SECONDS); // keep briefly
        redisTemplate.opsForSet().remove(INVITE_USER_PREFIX + userId, invitationId);

        return s;
    }

    /**
     * Decline an invitation.
     * If this was the only invitation for a new session (admin is alone), clean up the session.
     */
    public SessionInvitation declineInvitation(Long userId, String invitationId) {
        SessionInvitation inv = getInvitation(invitationId);
        if (inv == null) throw new RuntimeException("Invitation not found or expired");
        if (!inv.getToUserId().equals(userId)) throw new RuntimeException("Access denied");
        if (inv.getStatus() != SessionInvitation.Status.PENDING) throw new RuntimeException("Invitation already handled");

        inv.setStatus(SessionInvitation.Status.DECLINED);
        redisTemplate.opsForValue().set(INVITE_PREFIX + invitationId, inv, 10, TimeUnit.SECONDS);
        redisTemplate.opsForSet().remove(INVITE_USER_PREFIX + userId, invitationId);

        // If session has only the admin (no one else joined), clean it up
        FriendSessionData s = getSession(inv.getSessionId());
        if (s != null && s.getParticipants().size() <= 1 && s.getActivatedAt() == null) {
            redisTemplate.delete(KEY_PREFIX + s.getSessionId());
            redisTemplate.opsForSet().remove(USER_SET_PREFIX + s.getAdminId(), s.getSessionId());
        }

        return inv;
    }

    public SessionInvitation getInvitation(String invitationId) {
        Object obj = redisTemplate.opsForValue().get(INVITE_PREFIX + invitationId);
        return obj != null ? (SessionInvitation) obj : null;
    }

    public List<SessionInvitation> getPendingInvitations(Long userId) {
        Set<Object> ids = redisTemplate.opsForSet().members(INVITE_USER_PREFIX + userId);
        if (ids == null || ids.isEmpty()) return List.of();
        List<SessionInvitation> result = new ArrayList<>();
        for (Object id : ids) {
            SessionInvitation inv = getInvitation(id.toString());
            if (inv != null && inv.getStatus() == SessionInvitation.Status.PENDING) {
                result.add(inv);
            } else {
                redisTemplate.opsForSet().remove(INVITE_USER_PREFIX + userId, id);
            }
        }
        return result;
    }

    private void savePreserveTTL(FriendSessionData s) {
        String key = KEY_PREFIX + s.getSessionId();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long remaining = (ttl != null && ttl > 0) ? ttl : s.getExpireSeconds();
        redisTemplate.opsForValue().set(key, s, remaining, TimeUnit.SECONDS);
    }
}
