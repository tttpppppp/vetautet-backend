package com.vetautet.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingDetailResponse {
    private Long bookingId;
    private String status;
    private BigDecimal originalPrice;
    private String promoCode;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private Long tripId;
    private String trainCode;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer duration;
    private List<String> seatNumbers;
    private List<Long> ticketIds;
    private Integer passengerCount;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentTransactionId;
    private LocalDateTime paidAt;
    private List<TicketDetail> details;

    @Data
    @Builder
    public static class TicketDetail {
        private Long bookingDetailId;
        private Long ticketId;
        private String ticketStatus;
        private String seatNumber;
        private String carriageNumber;
        private String carriageTypeName;
        private BigDecimal price;
        private String passengerName;
        private String passengerIdCard;
        private String passengerType;
    }
}
