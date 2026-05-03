package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String trainCode;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime departureTime;
    private String seatNumber;
    private BigDecimal price;
    private String status;
    private Boolean heldByCurrentBooking = false;
    private Long holdingBookingId;
}
