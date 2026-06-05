package com.vetautet.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {
    private Long bookingId;
    private String requestId;
    private String orderNumber;
    private String storageMonth;
    private String tripType;
    private String status;
    private BigDecimal originalPrice;
    private String promoCode;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String contactIdCard;
    private LocalDateTime expiredAt;
    private List<String> seatNumbers;
    private List<Long> ticketIds;
}
