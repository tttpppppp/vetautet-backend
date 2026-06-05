package com.vetautet.application.service.admin.impl;

import com.vetautet.application.dto.AdminStatsResponse;
import com.vetautet.application.dto.BookingResponse;
import com.vetautet.application.dto.PromotionResponse;
import com.vetautet.application.dto.TripResponse;
import com.vetautet.application.service.admin.AdminStatsAppService;
import com.vetautet.application.service.order.BookingAppService;
import com.vetautet.application.service.promotion.PromotionAppService;
import com.vetautet.application.service.station.StationAppService;
import com.vetautet.application.service.train.TrainAppService;
import com.vetautet.application.service.trip.TripAppService;
import com.vetautet.application.service.user.UserAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsAppServiceImpl implements AdminStatsAppService {

    private static final Set<String> REVENUE_STATUSES = Set.of("CONFIRMED", "PAID", "COMPLETED");
    private static final Set<String> PENDING_STATUSES = Set.of("PENDING", "AWAITING_PAYMENT", "QUEUED");
    private static final Set<String> CANCELLED_STATUSES = Set.of("CANCELLED", "FAILED");

    private final BookingAppService bookingAppService;
    private final TripAppService tripAppService;
    private final StationAppService stationAppService;
    private final TrainAppService trainAppService;
    private final UserAppService userAppService;
    private final PromotionAppService promotionAppService;

    @Override
    public AdminStatsResponse getStats() {
        List<BookingResponse> bookings = bookingAppService.getAllBookings();
        List<TripResponse> trips = tripAppService.getAllTrips();
        List<PromotionResponse> promotions = promotionAppService.getPromotions(
                null, null, null, null, null, null, null
        );

        long revenueBookings = bookings.stream()
                .filter(booking -> REVENUE_STATUSES.contains(normalizeStatus(booking.getStatus())))
                .count();
        long pendingBookings = bookings.stream()
                .filter(booking -> PENDING_STATUSES.contains(normalizeStatus(booking.getStatus())))
                .count();
        long cancelledBookings = bookings.stream()
                .filter(booking -> CANCELLED_STATUSES.contains(normalizeStatus(booking.getStatus())))
                .count();
        long expiredBookings = bookings.stream()
                .filter(booking -> "EXPIRED".equals(normalizeStatus(booking.getStatus())))
                .count();

        BigDecimal revenue = bookings.stream()
                .filter(booking -> REVENUE_STATUSES.contains(normalizeStatus(booking.getStatus())))
                .map(booking -> defaultMoney(booking.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageBookingValue = revenueBookings == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(revenueBookings), 0, RoundingMode.HALF_UP);

        long totalSeats = trips.stream()
                .mapToLong(trip -> defaultLong(trip.getTotalSeats()))
                .sum();
        long availableSeats = trips.stream()
                .mapToLong(trip -> defaultLong(trip.getAvailableSeats()))
                .sum();

        return AdminStatsResponse.builder()
                .totalBookings(bookings.size())
                .revenueBookings(revenueBookings)
                .pendingBookings(pendingBookings)
                .cancelledBookings(cancelledBookings)
                .expiredBookings(expiredBookings)
                .revenue(revenue)
                .averageBookingValue(averageBookingValue)
                .totalTrips(trips.size())
                .activeTrips(trips.stream().filter(trip -> "ACTIVE".equals(normalizeStatus(trip.getStatus()))).count())
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .occupiedSeats(Math.max(0, totalSeats - availableSeats))
                .totalStations(stationAppService.getAllStations().size())
                .totalTrains(trainAppService.getAllTrains().size())
                .totalUsers(userAppService.getAllUsers().size())
                .totalPromotions(promotions.size())
                .activePromotions(promotions.stream().filter(this::isActivePromotion).count())
                .bookingStatusCounts(buildBookingStatusCounts(bookings))
                .topRoutes(buildTopRoutes(trips))
                .build();
    }

    private List<AdminStatsResponse.StatusCount> buildBookingStatusCounts(List<BookingResponse> bookings) {
        return bookings.stream()
                .collect(Collectors.groupingBy(
                        booking -> normalizeStatus(booking.getStatus()),
                        LinkedHashMap::new,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> AdminStatsResponse.StatusCount.builder()
                        .status(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private List<AdminStatsResponse.RouteStats> buildTopRoutes(List<TripResponse> trips) {
        Map<String, RouteAggregate> routes = new LinkedHashMap<>();
        for (TripResponse trip : trips) {
            String departureStation = defaultText(trip.getDepartureStation(), "Unknown");
            String arrivalStation = defaultText(trip.getArrivalStation(), "Unknown");
            String key = departureStation + " -> " + arrivalStation;
            RouteAggregate aggregate = routes.computeIfAbsent(key, ignored -> new RouteAggregate(departureStation, arrivalStation));
            aggregate.tripsCount++;
            aggregate.availableSeats += defaultLong(trip.getAvailableSeats());
            BigDecimal tripPrice = firstPositive(trip.getMinPrice(), trip.getPrice());
            if (tripPrice != null && (aggregate.minPrice == null || tripPrice.compareTo(aggregate.minPrice) < 0)) {
                aggregate.minPrice = tripPrice;
            }
        }

        return routes.values()
                .stream()
                .sorted(Comparator.comparingLong((RouteAggregate route) -> route.tripsCount).reversed())
                .limit(5)
                .map(route -> AdminStatsResponse.RouteStats.builder()
                        .route(route.departureStation + " -> " + route.arrivalStation)
                        .departureStation(route.departureStation)
                        .arrivalStation(route.arrivalStation)
                        .tripsCount(route.tripsCount)
                        .availableSeats(route.availableSeats)
                        .minPrice(route.minPrice)
                        .build())
                .toList();
    }

    private boolean isActivePromotion(PromotionResponse promotion) {
        return promotion.isActive() || "ACTIVE".equals(normalizeStatus(promotion.getStatus()));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long defaultLong(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private BigDecimal firstPositive(BigDecimal first, BigDecimal second) {
        if (first != null && first.compareTo(BigDecimal.ZERO) > 0) {
            return first;
        }
        if (second != null && second.compareTo(BigDecimal.ZERO) > 0) {
            return second;
        }
        return null;
    }

    private static class RouteAggregate {
        private final String departureStation;
        private final String arrivalStation;
        private long tripsCount;
        private long availableSeats;
        private BigDecimal minPrice;

        private RouteAggregate(String departureStation, String arrivalStation) {
            this.departureStation = departureStation;
            this.arrivalStation = arrivalStation;
        }
    }
}
