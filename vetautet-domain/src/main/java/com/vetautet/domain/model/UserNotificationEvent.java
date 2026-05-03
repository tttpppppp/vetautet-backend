package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationEvent {
    private Long notificationId;
    private Long userId;
    private Long bookingId;
    private String title;
    private String content;
    private String type;
    private Long referenceId;
    private boolean read;
    private LocalDateTime createdAt;
}
