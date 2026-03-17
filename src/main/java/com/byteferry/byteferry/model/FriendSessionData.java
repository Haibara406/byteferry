package com.byteferry.byteferry.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendSessionData implements Serializable {

    public enum Status { ACTIVE, CLOSED }

    public enum Role { ADMIN, MEMBER }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Participant implements Serializable {
        private Long userId;
        private String username;
        private Role role;
        @Builder.Default
        private boolean inviteAllowed = true;
        private String activeDeviceWsId;  // WebSocket session ID for single-device enforcement
        private LocalDateTime joinedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendSessionItem implements Serializable {
        private int id;
        private Long senderId;
        private String senderUsername;
        private ShareData.Type type;
        private String content;
        private List<ShareData.FileInfo> files;
        private LocalDateTime addedAt;
    }

    private String sessionId;
    private String sessionName;  // Custom session name
    private Long adminId;
    private String adminUsername;
    private Status status;
    @Builder.Default
    private boolean globalInviteEnabled = true;
    private int expireSeconds;
    private LocalDateTime createdAt;
    // When first invitee joins, TTL starts from this moment
    private LocalDateTime activatedAt;

    @Builder.Default
    private List<Participant> participants = new ArrayList<>();
    @Builder.Default
    private List<FriendSessionItem> items = new ArrayList<>();
    @Builder.Default
    private int nextItemId = 0;
}