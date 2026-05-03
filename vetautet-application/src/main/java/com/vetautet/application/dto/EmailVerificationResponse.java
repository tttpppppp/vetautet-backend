package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationResponse {
    private String email;
    private Boolean isEmailVerified;
    private String code;
    private String message;
    private LocalDateTime otpExpiresAt;
}
