package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {
    private Long id;
    private Ticket ticket;
    private Booking booking;
    private String passengerName;
    private String passengerIdCard;
    private String passengerType;
}
