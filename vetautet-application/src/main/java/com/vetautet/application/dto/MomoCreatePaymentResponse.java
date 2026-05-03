package com.vetautet.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MomoCreatePaymentResponse {
    private Long bookingId;
    private String momoOrderId;
    private String requestId;
    private Long amount;
    private Integer resultCode;
    private String message;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
    private String deeplinkMiniApp;
}
