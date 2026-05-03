package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayCreatePaymentCommand {
    private Long bookingId;
    private Long amount;
    private String orderInfo;
    private String clientIp;
    private String bankCode;
}
