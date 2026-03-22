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
@Table(name = "moment_read_status")
public class MomentReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @PrePersist
    protected void onCreate() {
        if (lastReadAt == null) {
            lastReadAt = LocalDateTime.now();
        }
    }
}
