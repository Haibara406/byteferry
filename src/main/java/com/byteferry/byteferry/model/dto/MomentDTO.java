package com.byteferry.byteferry.model.dto;

import com.byteferry.byteferry.model.entity.MomentImage;
import com.byteferry.byteferry.model.enums.Visibility;
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
public class MomentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String textContent;
    private boolean cardMode;
    private Visibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<MomentImage> images = new ArrayList<>();

    private String username;
    private String avatar;
}
