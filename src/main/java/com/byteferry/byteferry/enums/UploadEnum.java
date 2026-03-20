package com.byteferry.byteferry.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum UploadEnum {

    USER_AVATAR("user/avatar/", "用户头像",
            List.of("jpg", "jpeg", "png", "webp"), 5.0),

    MOMENT_IMAGE("moment/image/", "动态图片",
            List.of("jpg", "jpeg", "png", "gif", "webp"), 10.0),

    MOMENT_VIDEO("moment/video/", "动态视频",
            List.of("mp4", "mov"), 50.0);

    private final String dir;
    private final String description;
    private final List<String> format;
    private final Double limitSize;
}
