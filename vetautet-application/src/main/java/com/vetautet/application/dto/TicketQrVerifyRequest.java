package com.vetautet.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketQrVerifyRequest {
    @NotBlank(message = "QR token is required")
    private String qrToken;
}
