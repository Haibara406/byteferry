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
@Table(name = "friendships",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "friend_id"})
    },
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_friend_id", columnList = "friend_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_user_status", columnList = "user_id, status")
    }
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public enum Status { PENDING, ACCEPTED, BLOCKED }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
