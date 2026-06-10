package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateRequestedEvent {
    private String requestId;
    private Long userId;
    private String tripType;
    private Long tripId;
    private Long departureStationId;
    private Long arrivalStationId;
    private List<Long> ticketIds;
    private Long returnTripId;
    private Long returnDepartureStationId;
    private Long returnArrivalStationId;
    private List<Long> returnTicketIds;
    private List<PassengerDetails> passengers;
    private String promoCode;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String contactIdCard;
    private LocalDateTime requestedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDetails {
        private Long ticketId;
        private String direction;
        private String name;
        private String idCard;
        private String passengerType;
    }
}
