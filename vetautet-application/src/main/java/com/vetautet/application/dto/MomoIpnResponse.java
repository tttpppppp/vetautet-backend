package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MomoIpnResponse {
    private int resultCode;
    private String message;
}
