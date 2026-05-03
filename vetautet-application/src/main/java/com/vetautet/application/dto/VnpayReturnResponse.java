package com.vetautet.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VnpayReturnResponse {
    private String code;
    private String message;
    private Long bookingId;
    private String vnpTxnRef;
    private String transactionNo;
    private String responseCode;
    private String transactionStatus;
    private Long amount;
}
