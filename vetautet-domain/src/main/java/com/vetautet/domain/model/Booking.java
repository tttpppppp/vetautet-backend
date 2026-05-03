package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private Long id;
    private User user;
    private BigDecimal originalPrice;
    private String promoCode;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime expiredAt;
    private List<BookingDetail> details;
    private LocalDateTime createdAt;
}
