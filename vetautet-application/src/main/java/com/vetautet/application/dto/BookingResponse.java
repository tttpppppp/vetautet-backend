package com.vetautet.application.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;

@Data
@Builder
public class BookingResponse {
    private Long bookingId;
    private String status;
    private BigDecimal originalPrice;
    private String promoCode;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private LocalDateTime expiredAt;
    private List<String> seatNumbers;
    private List<Long> ticketIds;
}
