package com.vetautet.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VnpayCreatePaymentResponse {
    private Long bookingId;
    private String vnpTxnRef;
    private Long amount;
    private String orderInfo;
    private String paymentUrl;
}
