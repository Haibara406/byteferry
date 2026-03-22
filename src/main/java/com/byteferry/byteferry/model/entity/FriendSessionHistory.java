package com.byteferry.byteferry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "friend_session_history", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_user_closed", columnList = "user_id, closed_at")
})
public class FriendSessionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "admin_username", nullable = false, length = 50)
    private String adminUsername;

    // Comma-separated participant usernames
    @Column(name = "participants", length = 500)
    private String participants;

    @Column(name = "participant_count")
    private int participantCount;

    @Column(name = "item_count")
    private int itemCount;

    @Column(name = "expire_seconds")
    private int expireSeconds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "items_json", columnDefinition = "MEDIUMTEXT")
    private String itemsJson;

    @PrePersist
    protected void onCreate() { closedAt = LocalDateTime.now(); }
}
