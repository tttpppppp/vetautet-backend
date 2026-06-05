package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripFareQuoteResponse {
    private Long tripId;
    private Long departureStationId;
    private String departureStationName;
    private Long arrivalStationId;
    private String arrivalStationName;
    private Long carriageTypeId;
    private String carriageTypeCode;
    private String carriageTypeName;
    private String passengerType;
    private BigDecimal totalPrice;
    private String currency;
    private Long availableSeats;
    private List<Long> segmentIds;
    private List<TripSegmentResponse> segments;
}
