package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStopResponse {
    private Long id;
    private Long stationId;
    private String stationCode;
    private String stationName;
    private Integer stopOrder;
    private LocalDateTime scheduledArrivalTime;
    private LocalDateTime scheduledDepartureTime;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime estimatedDepartureTime;
    private LocalDateTime actualArrivalTime;
    private LocalDateTime actualDepartureTime;
    private BigDecimal distanceFromOriginKm;
    private String status;
    private String platform;
    private String note;
}
