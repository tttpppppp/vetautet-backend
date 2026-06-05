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
public class Notification {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String type; // BOOKING_CONFIRMED, BOOKING_CANCELLED, BOOKING_EXPIRED, BOOKING_FAILED, PAYMENT_SUCCESS, PAYMENT_FAILED, SYSTEM
    private Long referenceId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
