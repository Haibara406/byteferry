package com.byteferry.byteferry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "moment_images", indexes = {
    @Index(name = "idx_moment_id", columnList = "moment_id"),
    @Index(name = "idx_moment_sort", columnList = "moment_id, sort_order")
})
public class MomentImage implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "moment_id", nullable = false)
    private Long momentId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "is_live_photo", nullable = false)
    @Builder.Default
    private boolean isLivePhoto = false;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;
}
