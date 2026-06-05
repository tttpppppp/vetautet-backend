package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusEvent {
    private Long tripId;
    private Long ticketId;
    private String seatNumber;
    private String status; // AVAILABLE, HOLD, BOOKED
    private Long bookingId;
    private Long departureStationId;
    private Long arrivalStationId;
    private String segmentIds;
}
