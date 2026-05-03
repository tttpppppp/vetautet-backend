package com.vetautet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VnpayIpnResponse {
    @JsonProperty("RspCode")
    private String rspCode;

    @JsonProperty("Message")
    private String message;
}
