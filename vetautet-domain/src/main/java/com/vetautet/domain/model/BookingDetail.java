package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {
    private Long id;
    private Ticket ticket;
    private Booking booking;
    private String direction;
    private Long departureStationId;
    private Long arrivalStationId;
    private String segmentIds;
    private BigDecimal segmentPrice;
    private String passengerName;
    private String passengerIdCard;
    private String passengerType;
}
