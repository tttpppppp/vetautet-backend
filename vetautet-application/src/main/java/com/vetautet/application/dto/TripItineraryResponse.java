package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripItineraryResponse {
    private Long tripId;
    private String trainCode;
    private String trainCategory;
    private LocalDate serviceDate;
    private Long originStationId;
    private String originStationName;
    private Long destinationStationId;
    private String destinationStationName;
    private LocalDateTime scheduledDepartureTime;
    private LocalDateTime scheduledArrivalTime;
    private String status;
    private List<TripStopResponse> stops;
    private List<TripSegmentResponse> segments;
}
