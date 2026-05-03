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
    private Long tripId;
    private List<Long> ticketIds;
    private List<PassengerDetails> passengers;
    private String promoCode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDetails {
        private Long ticketId;
        private String name;
        private String idCard;
    }
}
