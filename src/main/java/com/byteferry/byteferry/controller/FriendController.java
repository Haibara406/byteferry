package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.model.FriendSessionData;
import com.byteferry.byteferry.model.SessionInvitation;
import com.byteferry.byteferry.model.ShareData;
import com.byteferry.byteferry.model.entity.Friendship;
import com.byteferry.byteferry.model.entity.FriendSessionHistory;
import com.byteferry.byteferry.model.entity.User;
import com.byteferry.byteferry.repository.UserRepository;
import com.byteferry.byteferry.service.FileStorageService;
import com.byteferry.byteferry.service.FriendService;
import com.byteferry.byteferry.service.FriendSessionService;
import com.byteferry.byteferry.websocket.FriendWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final FriendSessionService friendSessionService;
    private final FriendWebSocketHandler friendWsHandler;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private Long getUserId(Authentication auth) {
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return (Long) auth.getPrincipal();
    }

    private String getUsername(Authentication auth) {
        return auth.getDetails() != null ? auth.getDetails().toString() : "";
    }

    // ==================== Friend Management ====================

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> sendRequest(Authentication auth, @RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username required");
        try {
            Friendship f = friendService.sendRequest(getUserId(auth), username.trim());
            User target = userRepository.findById(f.getFriendId()).orElse(null);
            friendWsHandler.notifyFriendRequest(f.getFriendId(), getUserId(auth), getUsername(auth));
            Map<String, Object> result = new HashMap<>();
            result.put("id", f.getId());
            result.put("friendId", f.getFriendId());
            result.put("username", target != null ? target.getUsername() : "");
            result.put("status", f.getStatus().name());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/request/{id}/accept")
    public ResponseEntity<Map<String, Object>> acceptRequest(Authentication auth, @PathVariable Long id) {
        try {
            Friendship f = friendService.acceptRequest(getUserId(auth), id);
            User sender = userRepository.findById(f.getUserId()).orElse(null);
            friendWsHandler.notifyFriendAccepted(f.getUserId(), getUserId(auth), getUsername(auth));
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Accepted");
            result.put("friendId", f.getUserId());
            result.put("username", sender != null ? sender.getUsername() : "");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/request/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectRequest(Authentication auth, @PathVariable Long id) {
        try {
            friendService.rejectRequest(getUserId(auth), id);
            return ResponseEntity.ok(Map.of("message", "Rejected"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getFriendList(Authentication auth) {
        List<Friendship> friends = friendService.getFriends(getUserId(auth));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Friendship f : friends) {
            User u = userRepository.findById(f.getFriendId()).orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("friendshipId", f.getId());
            m.put("friendId", f.getFriendId());
            m.put("username", u != null ? u.getUsername() : "");
            m.put("online", friendWsHandler.isUserOnline(f.getFriendId()));
            m.put("since", f.getAcceptedAt() != null ? f.getAcceptedAt().toString() : "");
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> getRequests(Authentication auth) {
        Long userId = getUserId(auth);
        List<Friendship> received = friendService.getPendingRequests(userId);
        List<Friendship> sent = friendService.getSentRequests(userId);

        List<Map<String, Object>> recvList = new ArrayList<>();
        for (Friendship f : received) {
            User u = userRepository.findById(f.getUserId()).orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("userId", f.getUserId());
            m.put("username", u != null ? u.getUsername() : "");
            m.put("createdAt", f.getCreatedAt().toString());
            recvList.add(m);
        }
        List<Map<String, Object>> sentList = new ArrayList<>();
        for (Friendship f : sent) {
            User u = userRepository.findById(f.getFriendId()).orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("friendId", f.getFriendId());
            m.put("username", u != null ? u.getUsername() : "");
            m.put("createdAt", f.getCreatedAt().toString());
            sentList.add(m);
        }
        return ResponseEntity.ok(Map.of("received", recvList, "sent", sentList));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Map<String, String>> removeFriend(Authentication auth, @PathVariable Long friendId) {
        friendService.removeFriend(getUserId(auth), friendId);
        friendWsHandler.notifyFriendRemoved(friendId);
        return ResponseEntity.ok(Map.of("message", "Removed"));
    }

    @PostMapping("/{friendId}/block")
    public ResponseEntity<Map<String, String>> blockFriend(Authentication auth, @PathVariable Long friendId) {
        friendService.blockFriend(getUserId(auth), friendId);
        friendWsHandler.notifyFriendRemoved(friendId);
        return ResponseEntity.ok(Map.of("message", "Blocked"));
    }

    // ==================== Friend Sessions ====================

    // ---- Invitation endpoints ----

    @PostMapping("/session/invite")
    public ResponseEntity<Map<String, Object>> sendInvitation(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = getUserId(auth);
        String username = getUsername(auth);
        Long friendId = Long.valueOf(body.get("friendId").toString());
        String existingSessionId = body.get("sessionId") != null ? body.get("sessionId").toString() : null;

        try {
            SessionInvitation inv;
            if (existingSessionId != null) {
                // Invite to existing session
                inv = friendSessionService.inviteToExistingSession(existingSessionId, userId, username, friendId);
            } else {
                // New session invitation
                int expire = body.get("expireSeconds") != null ? Integer.parseInt(body.get("expireSeconds").toString()) : 1800;
                String sessionName = body.get("sessionName") != null ? body.get("sessionName").toString() : null;
                inv = friendSessionService.createInvitation(userId, username, friendId, expire, sessionName);
            }

            friendWsHandler.notifySessionInvitation(friendId, inv.getInvitationId(), inv.getSessionId(),
                    userId, username, inv.getExpireSeconds());

            Map<String, Object> result = new HashMap<>();
            result.put("invitationId", inv.getInvitationId());
            result.put("sessionId", inv.getSessionId());
            result.put("toUserId", friendId);
            result.put("expireSeconds", inv.getExpireSeconds());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/session/invite/{invId}/accept")
    public ResponseEntity<Map<String, Object>> acceptInvitation(Authentication auth, @PathVariable String invId) {
        Long userId = getUserId(auth);
        String username = getUsername(auth);
        try {
            FriendSessionData s = friendSessionService.acceptInvitation(userId, invId);

            // Notify all existing participants that a new member joined
            for (FriendSessionData.Participant p : s.getParticipants()) {
                if (!p.getUserId().equals(userId)) {
                    friendWsHandler.notifyMemberJoined(p.getUserId(), s.getSessionId(), userId, username);
                }
            }

            // Notify the invitation sender specifically
            SessionInvitation inv = friendSessionService.getInvitation(invId);
            if (inv != null) {
                friendWsHandler.notifyInvitationAccepted(inv.getFromUserId(), s.getSessionId(), userId, username);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", s.getSessionId());
            result.put("expireSeconds", s.getExpireSeconds());
            result.put("remainingSeconds", friendSessionService.getRemainingSeconds(s.getSessionId()));
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/session/invite/{invId}/decline")
    public ResponseEntity<Map<String, String>> declineInvitation(Authentication auth, @PathVariable String invId) {
        Long userId = getUserId(auth);
        String username = getUsername(auth);
        try {
            SessionInvitation inv = friendSessionService.declineInvitation(userId, invId);
            friendWsHandler.notifyInvitationDeclined(inv.getFromUserId(), invId, userId, username);
            return ResponseEntity.ok(Map.of("message", "Invitation declined"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/session/invitations")
    public ResponseEntity<List<Map<String, Object>>> getPendingInvitations(Authentication auth) {
        Long userId = getUserId(auth);
        List<SessionInvitation> invitations = friendSessionService.getPendingInvitations(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SessionInvitation inv : invitations) {
            Map<String, Object> m = new HashMap<>();
            m.put("invitationId", inv.getInvitationId());
            m.put("sessionId", inv.getSessionId());
            m.put("fromUserId", inv.getFromUserId());
            m.put("fromUsername", inv.getFromUsername());
            m.put("expireSeconds", inv.getExpireSeconds());
            m.put("createdAt", inv.getCreatedAt().toString());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ---- Session endpoints ----

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(Authentication auth, @PathVariable String sessionId) {
        Long userId = getUserId(auth);
        FriendSessionData s = friendSessionService.getSession(sessionId);
        if (s == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");

        // Check if user is a participant
        boolean isParticipant = s.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        long remaining = friendSessionService.getRemainingSeconds(sessionId);

        List<Map<String, Object>> items = new ArrayList<>();
        for (FriendSessionData.FriendSessionItem item : s.getItems()) {
            Map<String, Object> im = new HashMap<>();
            im.put("id", item.getId());
            im.put("senderId", item.getSenderId());
            im.put("senderUsername", item.getSenderUsername());
            im.put("type", item.getType().name());
            im.put("content", item.getContent() != null ? item.getContent() : "");
            im.put("addedAt", item.getAddedAt().toString());
            List<Map<String, Object>> fileList = new ArrayList<>();
            if (item.getFiles() != null) {
                for (int i = 0; i < item.getFiles().size(); i++) {
                    ShareData.FileInfo fi = item.getFiles().get(i);
                    Map<String, Object> fm = new HashMap<>();
                    fm.put("index", i);
                    fm.put("fileName", fi.getFileName() != null ? fi.getFileName() : "");
                    fm.put("fileSize", fi.getFileSize() != null ? fi.getFileSize() : 0);
                    fm.put("mimeType", fi.getMimeType() != null ? fi.getMimeType() : "");
                    fileList.add(fm);
                }
            }
            im.put("files", fileList);
            items.add(im);
        }

        // Build participants list for response
        List<Map<String, Object>> participantsList = new ArrayList<>();
        for (FriendSessionData.Participant p : s.getParticipants()) {
            Map<String, Object> pm = new HashMap<>();
            pm.put("userId", p.getUserId());
            pm.put("username", p.getUsername());
            pm.put("role", p.getRole().name());
            pm.put("inviteAllowed", p.isInviteAllowed());
            pm.put("joinedAt", p.getJoinedAt().toString());
            participantsList.add(pm);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", s.getSessionId());
        result.put("sessionName", s.getSessionName());
        result.put("status", s.getStatus().name());
        result.put("adminId", s.getAdminId());
        result.put("adminUsername", s.getAdminUsername());
        result.put("globalInviteEnabled", s.isGlobalInviteEnabled());
        result.put("participants", participantsList);
        result.put("expireSeconds", s.getExpireSeconds());
        result.put("remainingSeconds", remaining);
        result.put("activatedAt", s.getActivatedAt() != null ? s.getActivatedAt().toString() : null);
        result.put("itemCount", s.getItems().size());
        result.put("items", items);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/session/{sessionId}/items/text")
    public ResponseEntity<Map<String, Object>> addText(Authentication auth, @PathVariable String sessionId, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content required");
        try {
            Long userId = getUserId(auth);
            FriendSessionData.FriendSessionItem item = friendSessionService.addTextItem(sessionId, userId, getUsername(auth), content);
            FriendSessionData s = friendSessionService.getSession(sessionId);

            // Notify all other participants
            for (FriendSessionData.Participant p : s.getParticipants()) {
                if (!p.getUserId().equals(userId)) {
                    friendWsHandler.notifySessionUpdate(p.getUserId(), sessionId);
                }
            }

            return ResponseEntity.ok(Map.of("id", item.getId(), "type", item.getType().name()));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/items/file")
    public ResponseEntity<Map<String, Object>> addFile(Authentication auth, @PathVariable String sessionId, @RequestParam("file") MultipartFile[] files) throws IOException {
        if (files.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Files required");
        try {
            Long userId = getUserId(auth);
            FriendSessionData.FriendSessionItem item = friendSessionService.addFileItem(sessionId, userId, getUsername(auth), files);
            FriendSessionData s = friendSessionService.getSession(sessionId);

            // Notify all other participants
            for (FriendSessionData.Participant p : s.getParticipants()) {
                if (!p.getUserId().equals(userId)) {
                    friendWsHandler.notifySessionUpdate(p.getUserId(), sessionId);
                }
            }

            return ResponseEntity.ok(Map.of("id", item.getId(), "type", item.getType().name()));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/session/{sessionId}/items/{itemId}/preview/{fileIndex}")
    public ResponseEntity<Resource> preview(Authentication auth, @PathVariable String sessionId, @PathVariable int itemId, @PathVariable int fileIndex) throws IOException {
        return serveFile(getUserId(auth), sessionId, itemId, fileIndex, true);
    }

    @GetMapping("/session/{sessionId}/items/{itemId}/download/{fileIndex}")
    public ResponseEntity<Resource> download(Authentication auth, @PathVariable String sessionId, @PathVariable int itemId, @PathVariable int fileIndex) throws IOException {
        return serveFile(getUserId(auth), sessionId, itemId, fileIndex, false);
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, String>> closeSession(Authentication auth, @PathVariable String sessionId) {
        Long userId = getUserId(auth);
        FriendSessionData s = friendSessionService.getSession(sessionId);
        if (s == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        // Only admin can close
        if (!s.getAdminId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the admin can close the session");
        }

        // Notify all other participants before closing
        for (FriendSessionData.Participant p : s.getParticipants()) {
            if (!p.getUserId().equals(userId)) {
                friendWsHandler.notifySessionClosed(p.getUserId(), sessionId);
            }
        }

        // Pass pre-fetched session to avoid second Redis read
        friendSessionService.closeSession(s);
        return ResponseEntity.ok(Map.of("message", "Session closed"));
    }

    // ---- Multi-user session management ----

    @PostMapping("/session/{sessionId}/kick/{targetId}")
    public ResponseEntity<Map<String, String>> kickMember(Authentication auth, @PathVariable String sessionId, @PathVariable Long targetId) {
        Long userId = getUserId(auth);
        try {
            friendSessionService.kickMember(sessionId, userId, targetId);

            // Notify kicked user
            friendWsHandler.notifyMemberKicked(targetId, sessionId, getUsername(auth));

            // Notify remaining participants
            FriendSessionData s = friendSessionService.getSession(sessionId);
            if (s != null) {
                for (FriendSessionData.Participant p : s.getParticipants()) {
                    if (!p.getUserId().equals(userId)) {
                        friendWsHandler.notifyMemberLeft(p.getUserId(), sessionId, targetId, "");
                    }
                }
            }

            return ResponseEntity.ok(Map.of("message", "Member kicked"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/leave")
    public ResponseEntity<Map<String, String>> leaveSession(Authentication auth, @PathVariable String sessionId) {
        Long userId = getUserId(auth);
        String username = getUsername(auth);
        try {
            FriendSessionData s = friendSessionService.getSession(sessionId);
            friendSessionService.leaveSession(sessionId, userId);

            // Notify remaining participants
            if (s != null) {
                for (FriendSessionData.Participant p : s.getParticipants()) {
                    if (!p.getUserId().equals(userId)) {
                        friendWsHandler.notifyMemberLeft(p.getUserId(), sessionId, userId, username);
                    }
                }
            }

            return ResponseEntity.ok(Map.of("message", "Left session"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/activate")
    public ResponseEntity<Map<String, Object>> activateSession(Authentication auth, @PathVariable String sessionId) {
        Long userId = getUserId(auth);
        try {
            friendSessionService.activateSession(sessionId, userId);
            long remaining = friendSessionService.getRemainingSeconds(sessionId);

            // Notify all participants
            FriendSessionData s = friendSessionService.getSession(sessionId);
            if (s != null) {
                for (FriendSessionData.Participant p : s.getParticipants()) {
                    if (!p.getUserId().equals(userId)) {
                        friendWsHandler.notifySessionUpdate(p.getUserId(), sessionId);
                    }
                }
            }

            return ResponseEntity.ok(Map.of("remainingSeconds", remaining, "message", "Session activated"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/transfer")
    public ResponseEntity<Map<String, String>> transferAdmin(Authentication auth, @PathVariable String sessionId, @RequestBody Map<String, Object> body) {
        Long userId = getUserId(auth);
        String username = getUsername(auth);
        Long newAdminId = Long.valueOf(body.get("newAdminId").toString());
        try {
            FriendSessionData s = friendSessionService.getSession(sessionId);
            friendSessionService.transferAdmin(sessionId, userId, newAdminId);

            // Notify all remaining participants about admin change
            FriendSessionData updated = friendSessionService.getSession(sessionId);
            if (updated != null) {
                for (FriendSessionData.Participant p : updated.getParticipants()) {
                    friendWsHandler.notifySessionUpdate(p.getUserId(), sessionId);
                }
            }
            // Notify the new admin specifically about admin transfer
            friendWsHandler.notifyAdminTransferred(newAdminId, sessionId, username);
            // Notify the old admin that they left
            friendWsHandler.notifyMemberLeft(userId, sessionId, userId, username);

            return ResponseEntity.ok(Map.of("message", "Admin transferred and you have left the session"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/toggle-invite")
    public ResponseEntity<Map<String, String>> toggleGlobalInvite(Authentication auth, @PathVariable String sessionId, @RequestBody Map<String, Object> body) {
        boolean enabled = Boolean.parseBoolean(body.get("enabled").toString());
        try {
            friendSessionService.toggleGlobalInvite(sessionId, getUserId(auth), enabled);

            // Notify all participants about permission change
            FriendSessionData s = friendSessionService.getSession(sessionId);
            if (s != null) {
                for (FriendSessionData.Participant p : s.getParticipants()) {
                    if (!p.getUserId().equals(getUserId(auth))) {
                        friendWsHandler.notifySessionUpdate(p.getUserId(), sessionId);
                    }
                }
            }

            return ResponseEntity.ok(Map.of("message", "Global invite " + (enabled ? "enabled" : "disabled")));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/toggle-member-invite")
    public ResponseEntity<Map<String, String>> toggleMemberInvite(Authentication auth, @PathVariable String sessionId, @RequestBody Map<String, Object> body) {
        Long memberId = Long.valueOf(body.get("userId").toString());
        boolean enabled = Boolean.parseBoolean(body.get("enabled").toString());
        try {
            friendSessionService.toggleMemberInvite(sessionId, getUserId(auth), memberId, enabled);
            friendWsHandler.notifySessionUpdate(memberId, sessionId);
            return ResponseEntity.ok(Map.of("message", "Member invite " + (enabled ? "enabled" : "disabled")));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/session/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveSessions(Authentication auth) {
        Long userId = getUserId(auth);
        List<FriendSessionData> sessions = friendSessionService.getActiveSessions(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendSessionData s : sessions) {
            Map<String, Object> m = new HashMap<>();
            m.put("sessionId", s.getSessionId());
            m.put("sessionName", s.getSessionName());
            m.put("adminId", s.getAdminId());
            m.put("adminUsername", s.getAdminUsername());

            // Build participant summary
            List<Map<String, Object>> participants = new ArrayList<>();
            for (FriendSessionData.Participant p : s.getParticipants()) {
                Map<String, Object> pm = new HashMap<>();
                pm.put("userId", p.getUserId());
                pm.put("username", p.getUsername());
                pm.put("role", p.getRole().name());
                participants.add(pm);
            }
            m.put("participants", participants);
            m.put("remainingSeconds", friendSessionService.getRemainingSeconds(s.getSessionId()));
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/session/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(Authentication auth) {
        Long userId = getUserId(auth);
        List<FriendSessionHistory> history = friendSessionService.getHistory(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendSessionHistory h : history) {
            Map<String, Object> m = new HashMap<>();
            m.put("sessionId", h.getSessionId());
            m.put("adminUsername", h.getAdminUsername());
            m.put("participants", h.getParticipants());
            m.put("participantCount", h.getParticipantCount());
            m.put("itemCount", h.getItemCount());
            m.put("createdAt", h.getCreatedAt().toString());
            m.put("closedAt", h.getClosedAt() != null ? h.getClosedAt().toString() : "");
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Resource> serveFile(Long userId, String sessionId, int itemId, int fileIndex, boolean inline) throws IOException {
        FriendSessionData s = friendSessionService.getSession(sessionId);
        if (s == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        boolean isParticipant = s.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (!isParticipant) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        FriendSessionData.FriendSessionItem item = s.getItems().stream()
                .filter(i -> i.getId() == itemId).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (item.getFiles() == null || fileIndex < 0 || fileIndex >= item.getFiles().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file index");

        ShareData.FileInfo fi = item.getFiles().get(fileIndex);
        Path path = fileStorageService.load(fi.getFilePath());
        if (!Files.exists(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");

        long size = Files.size(path);
        InputStream in = Files.newInputStream(path);
        String encodedName = URLEncoder.encode(fi.getFileName() != null ? fi.getFileName() : "download", StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = fi.getMimeType() != null ? MediaType.parseMediaType(fi.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType).contentLength(size)
                .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encodedName)
                .body(new InputStreamResource(in));
    }
}
