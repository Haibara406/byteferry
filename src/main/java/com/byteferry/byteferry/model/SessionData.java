package com.byteferry.byteferry.model;

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
public class SessionData implements Serializable {

    public enum Status {
        ACTIVE, CLOSED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionItem implements Serializable {
        private int id;
        private ShareData.Type type;
        private String content;
        private List<ShareData.FileInfo> files;
        private LocalDateTime addedAt;
    }

    private String code;
    private Status status;
    private LocalDateTime createdAt;
    private int expireSeconds;

    @Builder.Default
    private List<SessionItem> items = new ArrayList<>();
    @Builder.Default
    private int nextItemId = 0;
}
