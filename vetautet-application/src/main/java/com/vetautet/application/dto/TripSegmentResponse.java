package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSegmentResponse {
    private Long id;
    private Integer segmentOrder;
    private Long fromStopId;
    private Long toStopId;
    private Long fromStationId;
    private String fromStationCode;
    private String fromStationName;
    private Long toStationId;
    private String toStationCode;
    private String toStationName;
    private LocalDateTime scheduledDepartureTime;
    private LocalDateTime scheduledArrivalTime;
    private BigDecimal distanceKm;
    private String status;
    private Long availableSeats;
    private List<TripSegmentPriceResponse> prices;
}
