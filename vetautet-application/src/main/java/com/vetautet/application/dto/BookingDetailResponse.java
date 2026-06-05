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
public class BookingDetailResponse {
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
    private LocalDateTime createdAt;
    private Long tripId;
    private String trainCode;
    private Long departureStationId;
    private String departureStation;
    private Long arrivalStationId;
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
        private String direction;
        private String ticketStatus;
        private String seatNumber;
        private String carriageNumber;
        private String carriageTypeName;
        private Long departureStationId;
        private String departureStation;
        private Long arrivalStationId;
        private String arrivalStation;
        private String segmentIds;
        private BigDecimal price;
        private String passengerName;
        private String passengerIdCard;
        private String passengerType;
    }
}
