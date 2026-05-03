package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayCreatePaymentResult {
    private String txnRef;
    private Long amount;
    private String orderInfo;
    private String paymentUrl;
}
