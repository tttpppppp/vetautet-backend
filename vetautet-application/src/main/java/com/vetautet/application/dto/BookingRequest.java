package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDetails {
        private Long ticketId;
        private String direction;
        private String name;
        private String idCard;
    }
}
