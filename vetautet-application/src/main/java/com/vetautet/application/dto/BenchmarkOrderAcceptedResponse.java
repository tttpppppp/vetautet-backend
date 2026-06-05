package com.vetautet.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BenchmarkOrderAcceptedResponse {
    private boolean success;
    private String code;
    private String message;
    private String placeOrderTaskId;
    private String requestId;
    private String kafkaTopic;
    private String kafkaKey;
    private String status;
    private LocalDateTime acceptedAt;
}
