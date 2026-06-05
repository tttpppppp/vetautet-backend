package com.vetautet.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BenchmarkOrderStatsResponse {
    private long totalOrders;
    private long createdOrders;
    private long pendingOrders;
    private long failedOrders;
    private Long remainingStock;
    private LocalDateTime latestProcessedAt;
}
