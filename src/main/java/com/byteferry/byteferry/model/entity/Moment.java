package com.byteferry.byteferry.model.entity;

import com.byteferry.byteferry.model.enums.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
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
@Entity
@Table(name = "moments")
public class Moment implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "card_mode", nullable = false)
    @Builder.Default
    private boolean cardMode = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    @JsonProperty("images")
    @Builder.Default
    private List<MomentImage> images = new ArrayList<>();

    @Transient
    @JsonProperty("username")
    private String username;

    @Transient
    @JsonProperty("avatar")
    private String avatar;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
