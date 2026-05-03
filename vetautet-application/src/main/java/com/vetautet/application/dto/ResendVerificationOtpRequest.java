package com.vetautet.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendVerificationOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;
}
