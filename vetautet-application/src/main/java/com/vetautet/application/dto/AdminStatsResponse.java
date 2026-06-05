package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalBookings;
    private long revenueBookings;
    private long pendingBookings;
    private long cancelledBookings;
    private long expiredBookings;
    private BigDecimal revenue;
    private BigDecimal averageBookingValue;

    private long totalTrips;
    private long activeTrips;
    private long totalSeats;
    private long availableSeats;
    private long occupiedSeats;

    private long totalStations;
    private long totalTrains;
    private long totalUsers;
    private long totalPromotions;
    private long activePromotions;

    private List<StatusCount> bookingStatusCounts;
    private List<RouteStats> topRoutes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private String status;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStats {
        private String route;
        private String departureStation;
        private String arrivalStation;
        private long tripsCount;
        private long availableSeats;
        private BigDecimal minPrice;
    }
}
