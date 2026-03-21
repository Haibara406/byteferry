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
@Table(name = "moment_templates")
public class MomentTemplate {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "html_template", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String htmlTemplate;

    @Column(name = "preview_image", length = 500)
    private String previewImage;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;
}
