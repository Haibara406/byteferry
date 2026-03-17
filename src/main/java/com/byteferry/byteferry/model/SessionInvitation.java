package com.byteferry.byteferry.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInvitation implements Serializable {

    public enum Status { PENDING, ACCEPTED, DECLINED }

    private String invitationId;
    private String sessionId;
    private String sessionName;  // Custom session name
    private Long fromUserId;
    private String fromUsername;
    private Long toUserId;
    // proposed session duration
    private int expireSeconds;
    private Status status;
    private LocalDateTime createdAt;
}