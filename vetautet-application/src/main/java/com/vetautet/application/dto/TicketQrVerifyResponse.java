package com.vetautet.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketQrVerifyResponse {
    private Boolean valid;
    private String code;
    private String message;
    private Long bookingId;
    private Long ticketId;
    private String ticketStatus;
    private String bookingStatus;
    private String passengerName;
    private String passengerIdCard;
    private String seatNumber;
    private String carriageNumber;
    private String carriageTypeName;
    private String trainCode;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
}
