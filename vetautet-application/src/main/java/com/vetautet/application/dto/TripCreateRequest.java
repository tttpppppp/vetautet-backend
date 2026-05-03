package com.vetautet.application.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TripCreateRequest {
    private Long trainId;
    private Long departureStationId;
    private Long arrivalStationId;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String status;
}
