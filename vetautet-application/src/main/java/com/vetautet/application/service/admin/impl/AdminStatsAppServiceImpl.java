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
import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingDetail;
import com.vetautet.domain.model.Ticket;
import com.vetautet.domain.service.BookingDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsAppServiceImpl implements AdminStatsAppService {

    private static final Set<String> REVENUE_STATUSES = Set.of("CONFIRMED", "PAID", "COMPLETED");
    private static final Set<String> PENDING_STATUSES = Set.of("PENDING", "AWAITING_PAYMENT", "QUEUED");
    private static final Set<String> CANCELLED_STATUSES = Set.of("CANCELLED", "FAILED");
    private static final DateTimeFormatter DAY_KEY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

    private final BookingAppService bookingAppService;
    private final BookingDomainService bookingDomainService;
    private final TripAppService tripAppService;
    private final StationAppService stationAppService;
    private final TrainAppService trainAppService;
    private final UserAppService userAppService;
    private final PromotionAppService promotionAppService;

    @Override
    public AdminStatsResponse getStats() {
        List<BookingResponse> bookings = bookingAppService.getAllBookings();
        List<TripResponse> trips = tripAppService.getAllTrips();
        List<Booking> detailedBookings = loadDetailedBookings(bookings);
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
                .dailyBookings(buildBookingSeries(detailedBookings, "day", 14))
                .monthlyBookings(buildBookingSeries(detailedBookings, "month", 12))
                .yearlyBookings(buildBookingSeries(detailedBookings, "year", 5))
                .topPurchasedTrains(buildTopPurchasedTrains(detailedBookings, trips))
                .topPurchasedSeatTypes(buildTopPurchasedSeatTypes(detailedBookings))
                .build();
    }

    private List<Booking> loadDetailedBookings(List<BookingResponse> bookings) {
        return bookings.stream()
                .map(BookingResponse::getBookingId)
                .filter(Objects::nonNull)
                .map(bookingDomainService::getBookingByIdFetched)
                .toList();
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

    private List<AdminStatsResponse.TimeSeriesStats> buildBookingSeries(List<Booking> bookings, String grain, int limit) {
        Map<String, TimeSeriesAggregate> series = new LinkedHashMap<>();
        bookings.stream()
                .filter(this::isRevenueBooking)
                .filter(booking -> booking.getCreatedAt() != null)
                .sorted(Comparator.comparing(Booking::getCreatedAt))
                .forEach(booking -> {
                    LocalDateTime createdAt = booking.getCreatedAt();
                    String period = periodKey(createdAt, grain);
                    TimeSeriesAggregate aggregate = series.computeIfAbsent(period, ignored -> new TimeSeriesAggregate(period, periodLabel(createdAt, grain)));
                    aggregate.count++;
                    aggregate.revenue = aggregate.revenue.add(defaultMoney(booking.getTotalPrice()));
                });

        List<AdminStatsResponse.TimeSeriesStats> points = series.values()
                .stream()
                .map(point -> AdminStatsResponse.TimeSeriesStats.builder()
                        .period(point.period)
                        .label(point.label)
                        .count(point.count)
                        .revenue(point.revenue)
                        .build())
                .toList();

        if (points.size() <= limit) {
            return points;
        }
        return new ArrayList<>(points.subList(points.size() - limit, points.size()));
    }

    private List<AdminStatsResponse.PurchaseStats> buildTopPurchasedTrains(List<Booking> bookings, List<TripResponse> trips) {
        Map<Long, String> trainByTripId = trips.stream()
                .filter(trip -> trip.getId() != null)
                .collect(Collectors.toMap(
                        TripResponse::getId,
                        trip -> defaultText(trip.getTrainCode(), "Tau #" + trip.getId()),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        Map<String, PurchaseAggregate> trainStats = new LinkedHashMap<>();

        for (Booking booking : bookings) {
            if (!isRevenueBooking(booking)) {
                continue;
            }
            for (BookingDetail detail : detailsOf(booking)) {
                Ticket ticket = detail.getTicket();
                Long tripId = tripIdOf(ticket);
                String trainName = tripId == null ? "Tau khong ro" : trainByTripId.getOrDefault(tripId, "Tau #" + tripId);
                PurchaseAggregate aggregate = trainStats.computeIfAbsent(trainName, PurchaseAggregate::new);
                aggregate.count++;
                aggregate.revenue = aggregate.revenue.add(ticketPriceOf(detail));
            }
        }

        return toPurchaseStats(trainStats, 6);
    }

    private List<AdminStatsResponse.PurchaseStats> buildTopPurchasedSeatTypes(List<Booking> bookings) {
        Map<String, PurchaseAggregate> seatTypeStats = new LinkedHashMap<>();

        for (Booking booking : bookings) {
            if (!isRevenueBooking(booking)) {
                continue;
            }
            for (BookingDetail detail : detailsOf(booking)) {
                Ticket ticket = detail.getTicket();
                String seatType = ticket == null ? "Khong ro" : defaultText(ticket.getCarriageTypeName(), "Khong ro");
                PurchaseAggregate aggregate = seatTypeStats.computeIfAbsent(seatType, PurchaseAggregate::new);
                aggregate.count++;
                aggregate.revenue = aggregate.revenue.add(ticketPriceOf(detail));
            }
        }

        return toPurchaseStats(seatTypeStats, 6);
    }

    private List<AdminStatsResponse.PurchaseStats> toPurchaseStats(Map<String, PurchaseAggregate> aggregates, int limit) {
        return aggregates.values()
                .stream()
                .sorted(Comparator.comparingLong((PurchaseAggregate aggregate) -> aggregate.count).reversed())
                .limit(limit)
                .map(aggregate -> AdminStatsResponse.PurchaseStats.builder()
                        .name(aggregate.name)
                        .count(aggregate.count)
                        .revenue(aggregate.revenue)
                        .build())
                .toList();
    }

    private boolean isActivePromotion(PromotionResponse promotion) {
        return promotion.isActive() || "ACTIVE".equals(normalizeStatus(promotion.getStatus()));
    }

    private boolean isRevenueBooking(Booking booking) {
        return booking != null && REVENUE_STATUSES.contains(normalizeStatus(booking.getStatus()));
    }

    private List<BookingDetail> detailsOf(Booking booking) {
        return booking.getDetails() == null ? List.of() : booking.getDetails();
    }

    private Long tripIdOf(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        if (ticket.getTripId() != null) {
            return ticket.getTripId();
        }
        return ticket.getTrip() == null ? null : ticket.getTrip().getId();
    }

    private BigDecimal ticketPriceOf(BookingDetail detail) {
        if (detail == null) {
            return BigDecimal.ZERO;
        }
        if (detail.getSegmentPrice() != null && detail.getSegmentPrice().compareTo(BigDecimal.ZERO) > 0) {
            return detail.getSegmentPrice();
        }
        Ticket ticket = detail.getTicket();
        return ticket == null ? BigDecimal.ZERO : defaultMoney(ticket.getPrice());
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

    private String periodKey(LocalDateTime dateTime, String grain) {
        if ("day".equals(grain)) {
            return DAY_KEY_FORMATTER.format(dateTime);
        }
        if ("year".equals(grain)) {
            return YEAR_FORMATTER.format(dateTime);
        }
        return MONTH_KEY_FORMATTER.format(dateTime);
    }

    private String periodLabel(LocalDateTime dateTime, String grain) {
        if ("day".equals(grain)) {
            return DAY_LABEL_FORMATTER.format(dateTime);
        }
        if ("year".equals(grain)) {
            return YEAR_FORMATTER.format(dateTime);
        }
        return MONTH_LABEL_FORMATTER.format(dateTime);
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

    private static class TimeSeriesAggregate {
        private final String period;
        private final String label;
        private long count;
        private BigDecimal revenue = BigDecimal.ZERO;

        private TimeSeriesAggregate(String period, String label) {
            this.period = period;
            this.label = label;
        }
    }

    private static class PurchaseAggregate {
        private final String name;
        private long count;
        private BigDecimal revenue = BigDecimal.ZERO;

        private PurchaseAggregate(String name) {
            this.name = name;
        }
    }
}
