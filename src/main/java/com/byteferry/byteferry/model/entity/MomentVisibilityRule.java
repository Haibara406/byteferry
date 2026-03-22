package com.byteferry.byteferry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "moment_visibility_rules",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"moment_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_moment_id", columnList = "moment_id"),
        @Index(name = "idx_user_id", columnList = "user_id")
    }
)
public class MomentVisibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "moment_id", nullable = false)
    private Long momentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
