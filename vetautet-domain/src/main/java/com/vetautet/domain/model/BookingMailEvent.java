package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingMailEvent {
    private Long bookingId;
    private Long userId;
    private String recipientEmail;
    private String subject;
    private String content;
}
