package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSagaEvent {
    private Long bookingId;
    private Long userId;
    private Long tripId;
    private List<Long> ticketIds;
    private String step;
    private String status;
    private String reason;
    private LocalDateTime occurredAt;
}
