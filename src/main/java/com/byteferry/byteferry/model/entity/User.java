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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 500)
    @Builder.Default
    private String avatar = "https://minio.haikari.top/byteferry/default/default-avatar.png";

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Gender gender = Gender.UNKNOWN;

    @Column(name = "email_bound", nullable = false)
    @Builder.Default
    private boolean emailBound = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Gender {
        MALE, FEMALE, OTHER, UNKNOWN
    }
}
