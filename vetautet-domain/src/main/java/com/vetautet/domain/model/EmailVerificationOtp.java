package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationOtp {
    private Long id;
    private Long userId;
    private String email;
    private String otp;
    private LocalDateTime expiresAt;
    private Boolean used;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
}
