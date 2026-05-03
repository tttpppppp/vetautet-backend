package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String trainCode;
    private String trainCategory;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer duration;
    private BigDecimal price;
    private BigDecimal minPrice;
    private String promoCode;
    private String promotionDiscountLabel;
    private Boolean promotionApplied;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String promotionMessage;
    private Integer availableSeats;
    private Integer totalSeats;
    private String status;
    private List<CarriageResponse> carriages;
}
