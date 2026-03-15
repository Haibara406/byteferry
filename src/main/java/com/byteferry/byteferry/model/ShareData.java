package com.byteferry.byteferry.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareData implements Serializable {

    public enum Type {
        TEXT, IMAGE, FILE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo implements Serializable {
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String mimeType;
    }

    private Type type;
    private String content;
    private List<FileInfo> files;
    private LocalDateTime createdAt;
    private int expireSeconds;
    private boolean deleteAfterDownload;
}
