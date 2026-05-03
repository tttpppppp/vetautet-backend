package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {
    private String email;
    private String code;
    private String message;
    private LocalDateTime otpExpiresAt;
}
