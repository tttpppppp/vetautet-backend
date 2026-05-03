package com.vetautet.application.service.trip.impl;

import com.vetautet.application.cache.TripCacheKeys;
import com.vetautet.application.cache.TripJsonCacheService;
import com.vetautet.application.dto.CarriageResponse;
import com.vetautet.application.dto.PopularDestinationResponse;
import com.vetautet.application.dto.PopularRouteResponse;
import com.vetautet.application.dto.TrainCategoryResponse;
import com.vetautet.application.dto.TripCreateRequest;
import com.vetautet.application.dto.TripResponse;
import com.vetautet.application.mapper.UserMapper;
import com.vetautet.application.service.trip.TripAppService;
import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingDetail;
import com.vetautet.domain.model.Promotion;
import com.vetautet.domain.model.Station;
import com.vetautet.domain.model.Trip;
import com.vetautet.domain.service.BookingDomainService;
import com.vetautet.domain.service.PromotionDomainService;
import com.vetautet.domain.service.TripDomainService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TripAppServiceImpl implements TripAppService {

    private static final Logger log = LoggerFactory.getLogger(TripAppServiceImpl.class);

    @Autowired
    private TripDomainService tripDomainService;

    @Autowired
    private BookingDomainService bookingDomainService;

    @Autowired
    private PromotionDomainService promotionDomainService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TripJsonCacheService tripJsonCacheService;

    private static final String TRIP_CACHE_KEY = TripCacheKeys.TRIPS_ALL;
    private static final String TRIP_LOCK_KEY = "lock:trips:all";

    @Override
    @SuppressWarnings("unchecked")
    public List<TripResponse> getAllTrips() {
        return getAllTripsWithRetry(0);
    }

    @Override
    public List<TripResponse> getAllTrips(String promoCode) {
        return applyPromotion(getAllTrips(), promoCode);
    }

    @Override
    public List<TrainCategoryResponse> getTrainCategories() {
        return List.of(
                new TrainCategoryResponse("ALL", "All", "All available train trips"),
                new TrainCategoryResponse("SE_TN", "SE/TN Trains", "Thong Nhat SE/TN train routes"),
                new TrainCategoryResponse("HIGH_QUALITY", "High-Quality", "High quality express train routes"),
                new TrainCategoryResponse("SUBURBAN", "Suburban", "Local and suburban train routes")
        );
    }

    @Override
    public List<TripResponse> getPopularTrips(int limit) {
        Comparator<Trip> popularComparator = Comparator
                .comparingInt(this::countBookedSeats)
                .reversed()
                .thenComparing(Comparator.comparingInt(this::countAvailableSeats).reversed())
                .thenComparing(this::minTicketPrice, Comparator.nullsLast(BigDecimal::compareTo))
                .thenComparing(Trip::getDepartureTime, Comparator.nullsLast(Comparator.naturalOrder()));

        return tripDomainService.getAllActiveTrips().stream()
                .sorted(popularComparator)
                .limit(normalizeLimit(limit))
                .map(trip -> userMapper.toTripResponse(trip, false))
                .collect(Collectors.toList());
    }

    @Override
    public List<PopularRouteResponse> getPopularRoutes(int limit) {
        Map<String, RouteAggregate> routes = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (Trip trip : tripDomainService.getAllActiveTrips()) {
            if (trip.getDepartureStation() == null || trip.getArrivalStation() == null) {
                continue;
            }

            String key = trip.getDepartureStation().getCode() + "->" + trip.getArrivalStation().getCode();
            RouteAggregate route = routes.computeIfAbsent(key, ignored ->
                    new RouteAggregate(trip.getDepartureStation(), trip.getArrivalStation()));
            route.apply(trip, now, countAvailableSeats(trip), minTicketPrice(trip));
        }

        return routes.values().stream()
                .sorted(Comparator.comparingInt(RouteAggregate::getTripsCount).reversed()
                        .thenComparing(RouteAggregate::getMinPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                .limit(normalizeLimit(limit))
                .map(RouteAggregate::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PopularDestinationResponse> getPopularDestinations(int limit) {
        Map<String, DestinationAggregate> destinations = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (Trip trip : tripDomainService.getAllActiveTrips()) {
            if (trip.getArrivalStation() == null) {
                continue;
            }

            String key = trip.getArrivalStation().getCode();
            DestinationAggregate destination = destinations.computeIfAbsent(key, ignored ->
                    new DestinationAggregate(trip.getArrivalStation()));
            destination.apply(trip, now, countAvailableSeats(trip), minTicketPrice(trip));
        }

        return destinations.values().stream()
                .sorted(Comparator.comparingInt(DestinationAggregate::getTripsCount).reversed()
                        .thenComparing(DestinationAggregate::getMinPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                .limit(normalizeLimit(limit))
                .map(DestinationAggregate::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripResponse> getUpcomingDepartures(int limit) {
        LocalDateTime now = LocalDateTime.now();

        return tripDomainService.getAllActiveTrips().stream()
                .filter(trip -> trip.getDepartureTime() != null)
                .filter(trip -> !trip.getDepartureTime().isBefore(now))
                .sorted(Comparator.comparing(Trip::getDepartureTime))
                .limit(normalizeLimit(limit))
                .map(trip -> userMapper.toTripResponse(trip, false))
                .collect(Collectors.toList());
    }

    private List<TripResponse> getAllTripsWithRetry(int retryCount) {
        if (retryCount > 3) {
            log.warn("Max retries reached for getAllTrips cache. Falling back to DB");
            return tripDomainService.getAllActiveTrips().stream()
                    .map(trip -> userMapper.toTripResponse(trip, false))
                    .collect(Collectors.toList());
        }

        List<TripResponse> cachedTrips = (List<TripResponse>) redisTemplate.opsForValue().get(TRIP_CACHE_KEY);
        if (cachedTrips != null) {
            log.debug("Trip list cache hit");
            return cachedTrips;
        }

        RLock lock = redissonClient.getLock(TRIP_LOCK_KEY);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                @SuppressWarnings("unchecked")
                List<TripResponse> secondCheck = (List<TripResponse>) redisTemplate.opsForValue().get(TRIP_CACHE_KEY);
                if (secondCheck != null) {
                    return secondCheck;
                }

                log.debug("Trip list cache miss. Querying DB");
                List<TripResponse> trips = tripDomainService.getAllActiveTrips().stream()
                        .map(trip -> userMapper.toTripResponse(trip, false))
                        .collect(Collectors.toList());

                redisTemplate.opsForValue().set(TRIP_CACHE_KEY, trips, Duration.ofMinutes(10));
                return trips;
            }

            Thread.sleep(1000);
            return getAllTripsWithRetry(retryCount + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("System interrupted during locking");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public TripResponse getTripById(Long id) {
        String cacheKey = TripCacheKeys.trip(id);
        String lockKey = "lock:trip:" + id;

        TripResponse cachedTrip = (TripResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedTrip != null) {
            return cachedTrip;
        }

        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                cachedTrip = (TripResponse) redisTemplate.opsForValue().get(cacheKey);
                if (cachedTrip != null) {
                    return cachedTrip;
                }

                log.debug("Trip detail cache miss. Querying DB for id={}", id);
                TripResponse trip = userMapper.toTripResponse(tripDomainService.getTripByIdFetched(id), true);

                redisTemplate.opsForValue().set(cacheKey, trip, Duration.ofMinutes(10));
                return trip;
            }

            Thread.sleep(300);
            return getTripById(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public TripResponse getTripById(Long id, Long bookingId) {
        if (bookingId == null) {
            return getTripById(id);
        }

        TripResponse trip = userMapper.toTripResponse(tripDomainService.getTripByIdFetched(id), true);
        markHeldSeatsByCurrentBooking(trip, bookingId);
        return trip;
    }

    private void markHeldSeatsByCurrentBooking(TripResponse trip, Long bookingId) {
        Booking booking = bookingDomainService.findBookingByIdOrNull(bookingId);
        if (booking == null
                || booking.getDetails() == null
                || !"PENDING".equalsIgnoreCase(booking.getStatus())
                || !belongsToCurrentUser(booking)) {
            return;
        }

        Set<Long> bookingTicketIds = booking.getDetails().stream()
                .map(BookingDetail::getTicket)
                .filter(ticket -> ticket != null && ticket.getId() != null)
                .map(ticket -> ticket.getId())
                .collect(Collectors.toSet());

        if (bookingTicketIds.isEmpty() || trip.getCarriages() == null) {
            return;
        }

        trip.getCarriages().stream()
                .map(CarriageResponse::getSeats)
                .filter(seats -> seats != null)
                .flatMap(List::stream)
                .filter(seat -> bookingTicketIds.contains(seat.getId()))
                .filter(seat -> "HOLD".equalsIgnoreCase(seat.getStatus()))
                .forEach(seat -> {
                    seat.setHeldByCurrentBooking(true);
                    seat.setHoldingBookingId(bookingId);
                });
    }

    private boolean belongsToCurrentUser(Booking booking) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || booking.getUser() == null || booking.getUser().getEmail() == null) {
            return false;
        }
        return booking.getUser().getEmail().equals(auth.getName());
    }

    @Override
    public List<TripResponse> searchTrips(String departure, String arrival, LocalDate date,
                                          List<String> trainTypes, String trainCategory,
                                          BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Searching trips with filters");
        return tripDomainService.searchTrips(
                        departure,
                        arrival,
                        date,
                        normalizeTrainTypes(trainTypes),
                        normalizeTrainCategory(trainCategory),
                        minPrice,
                        maxPrice
                ).stream()
                .map(trip -> userMapper.toTripResponse(trip, false))
                .collect(Collectors.toList());
    }

    @Override
    public List<TripResponse> searchTrips(String departure, String arrival, LocalDate date,
                                          List<String> trainTypes, String trainCategory,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          String promoCode) {
        return applyPromotion(
                searchTrips(departure, arrival, date, trainTypes, trainCategory, minPrice, maxPrice),
                promoCode
        );
    }

    private List<String> normalizeTrainTypes(List<String> trainTypes) {
        if (trainTypes == null || trainTypes.isEmpty()) {
            return null;
        }
        List<String> normalized = trainTypes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase())
                .collect(Collectors.toList());
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeTrainCategory(String trainCategory) {
        if (trainCategory == null || trainCategory.isBlank()) {
            return null;
        }
        String normalized = trainCategory.trim().toUpperCase();
        return "ALL".equals(normalized) ? null : normalized;
    }

    @Override
    @Transactional
    public TripResponse createTrip(TripCreateRequest request) {
        Trip trip = Trip.builder()
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .status(request.getStatus())
                .build();

        Trip saved = tripDomainService.createTrip(
                trip,
                request.getDepartureStationId(),
                request.getArrivalStationId(),
                request.getTrainId()
        );

        redisTemplate.delete(TRIP_CACHE_KEY);
        tripJsonCacheService.evict(TripCacheKeys.HTTP_TRIPS_ALL);
        tripJsonCacheService.evictByPrefix(TripCacheKeys.HTTP_TRIPS_SEARCH_PREFIX);
        return userMapper.toTripResponse(saved, false);
    }

    @Override
    @Transactional
    public TripResponse updateTrip(Long id, TripCreateRequest request) {
        Trip trip = Trip.builder()
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .status(request.getStatus())
                .build();

        Trip saved = tripDomainService.updateTrip(
                id,
                trip,
                request.getDepartureStationId(),
                request.getArrivalStationId(),
                request.getTrainId()
        );

        redisTemplate.delete(TRIP_CACHE_KEY);
        redisTemplate.delete(TripCacheKeys.trip(id));
        tripJsonCacheService.evict(TripCacheKeys.HTTP_TRIPS_ALL);
        tripJsonCacheService.evict(TripCacheKeys.httpTrip(id));
        tripJsonCacheService.evictByPrefix(TripCacheKeys.HTTP_TRIPS_SEARCH_PREFIX);

        return userMapper.toTripResponse(saved, false);
    }

    @Override
    @Transactional
    public void deleteTrip(Long id) {
        tripDomainService.deleteTrip(id);
        redisTemplate.delete(TRIP_CACHE_KEY);
        redisTemplate.delete(TripCacheKeys.trip(id));
        tripJsonCacheService.evict(TripCacheKeys.HTTP_TRIPS_ALL);
        tripJsonCacheService.evict(TripCacheKeys.httpTrip(id));
        tripJsonCacheService.evictByPrefix(TripCacheKeys.HTTP_TRIPS_SEARCH_PREFIX);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 6;
        }
        return Math.min(limit, 12);
    }

    private List<TripResponse> applyPromotion(List<TripResponse> trips, String rawPromoCode) {
        String promoCode = normalizePromoCode(rawPromoCode);
        if (promoCode == null || trips == null || trips.isEmpty()) {
            return trips;
        }

        Promotion promotion = null;
        String invalidMessage = null;
        try {
            promotion = promotionDomainService.getActivePromotionByCode(promoCode, LocalDate.now());
        } catch (RuntimeException ex) {
            invalidMessage = ex.getMessage() != null ? ex.getMessage() : "Promotion is not valid";
        }

        Promotion activePromotion = promotion;
        String message = invalidMessage;
        return trips.stream()
                .map(trip -> applyPromotionToTrip(trip, promoCode, activePromotion, message))
                .collect(Collectors.toList());
    }

    private TripResponse applyPromotionToTrip(TripResponse trip,
                                              String promoCode,
                                              Promotion promotion,
                                              String invalidMessage) {
        TripResponse response = copyTripResponse(trip);
        BigDecimal originalPrice = resolveTripPrice(response);
        response.setPromoCode(promoCode);
        response.setOriginalPrice(originalPrice);
        response.setDiscountAmount(BigDecimal.ZERO);
        response.setFinalPrice(originalPrice);
        response.setPromotionApplied(false);

        if (promotion == null) {
            response.setPromotionMessage(invalidMessage != null ? invalidMessage : "Promotion is not valid");
            return response;
        }

        response.setPromotionDiscountLabel(discountLabel(promotion));
        if (promotion.getMinOrderAmount() != null && originalPrice.compareTo(promotion.getMinOrderAmount()) < 0) {
            response.setPromotionMessage("Trip price does not meet promotion minimum");
            return response;
        }

        BigDecimal discountAmount = calculatePromotionDiscount(promotion, originalPrice);
        response.setDiscountAmount(discountAmount);
        response.setFinalPrice(originalPrice.subtract(discountAmount).max(BigDecimal.ZERO));
        response.setPromotionApplied(discountAmount.compareTo(BigDecimal.ZERO) > 0);
        response.setPromotionMessage(response.getPromotionApplied() ? "Promotion applied" : "Promotion has no discount value");
        return response;
    }

    private TripResponse copyTripResponse(TripResponse source) {
        TripResponse target = new TripResponse();
        target.setId(source.getId());
        target.setTrainCode(source.getTrainCode());
        target.setTrainCategory(source.getTrainCategory());
        target.setDepartureStation(source.getDepartureStation());
        target.setArrivalStation(source.getArrivalStation());
        target.setDepartureTime(source.getDepartureTime());
        target.setArrivalTime(source.getArrivalTime());
        target.setDuration(source.getDuration());
        target.setPrice(source.getPrice());
        target.setMinPrice(source.getMinPrice());
        target.setAvailableSeats(source.getAvailableSeats());
        target.setTotalSeats(source.getTotalSeats());
        target.setStatus(source.getStatus());
        target.setCarriages(source.getCarriages());
        return target;
    }

    private BigDecimal resolveTripPrice(TripResponse trip) {
        if (trip.getMinPrice() != null) {
            return trip.getMinPrice();
        }
        if (trip.getPrice() != null) {
            return trip.getPrice();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculatePromotionDiscount(Promotion promotion, BigDecimal originalPrice) {
        BigDecimal value = promotion.getDiscountValue() == null ? BigDecimal.ZERO : promotion.getDiscountValue();
        BigDecimal discountAmount;
        if ("percent".equalsIgnoreCase(promotion.getDiscountType())) {
            discountAmount = originalPrice.multiply(value)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discountAmount = value;
        }

        if (promotion.getMaxDiscountAmount() != null) {
            discountAmount = discountAmount.min(promotion.getMaxDiscountAmount());
        }
        return discountAmount.min(originalPrice).max(BigDecimal.ZERO);
    }

    private String discountLabel(Promotion promotion) {
        BigDecimal value = promotion.getDiscountValue();
        if ("percent".equalsIgnoreCase(promotion.getDiscountType())) {
            return "Giam " + formatNumber(value) + "%";
        }
        if ("serviceFee".equalsIgnoreCase(promotion.getDiscountType())) {
            return "Mien phi dich vu";
        }
        return "Giam " + formatNumber(value) + "d";
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String normalizePromoCode(String rawPromoCode) {
        if (rawPromoCode == null || rawPromoCode.isBlank()) {
            return null;
        }
        return rawPromoCode.trim().toUpperCase();
    }

    private int countBookedSeats(Trip trip) {
        return countTicketsByStatus(trip, "BOOKED");
    }

    private int countAvailableSeats(Trip trip) {
        return countTicketsByStatus(trip, "AVAILABLE");
    }

    private int countTicketsByStatus(Trip trip, String status) {
        if (trip.getTickets() == null) {
            return 0;
        }
        return (int) trip.getTickets().stream()
                .filter(ticket -> status.equalsIgnoreCase(ticket.getStatus()))
                .count();
    }

    private BigDecimal minTicketPrice(Trip trip) {
        if (trip.getTickets() == null) {
            return null;
        }
        return trip.getTickets().stream()
                .map(ticket -> ticket.getPrice())
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private static BigDecimal minPrice(BigDecimal current, BigDecimal candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        return current.min(candidate);
    }

    private static boolean isUpcoming(LocalDateTime departureTime, LocalDateTime now) {
        return departureTime != null && !departureTime.isBefore(now);
    }

    private static void applyNextDeparture(RouteAggregate aggregate, Trip trip, LocalDateTime now) {
        if (!isUpcoming(trip.getDepartureTime(), now)) {
            return;
        }
        if (aggregate.nextDepartureTime == null || trip.getDepartureTime().isBefore(aggregate.nextDepartureTime)) {
            aggregate.nextDepartureTime = trip.getDepartureTime();
        }
    }

    private static void applyNextDeparture(DestinationAggregate aggregate, Trip trip, LocalDateTime now) {
        if (!isUpcoming(trip.getDepartureTime(), now)) {
            return;
        }
        if (aggregate.nextDepartureTime == null || trip.getDepartureTime().isBefore(aggregate.nextDepartureTime)) {
            aggregate.nextDepartureTime = trip.getDepartureTime();
        }
    }

    private static class RouteAggregate {
        private final Station departureStation;
        private final Station arrivalStation;
        private final Set<String> trainCategories = new LinkedHashSet<>();
        private int tripsCount;
        private int availableSeats;
        private BigDecimal minPrice;
        private LocalDateTime nextDepartureTime;

        private RouteAggregate(Station departureStation, Station arrivalStation) {
            this.departureStation = departureStation;
            this.arrivalStation = arrivalStation;
        }

        private void apply(Trip trip, LocalDateTime now, int tripAvailableSeats, BigDecimal tripMinPrice) {
            tripsCount++;
            availableSeats += tripAvailableSeats;
            minPrice = TripAppServiceImpl.minPrice(minPrice, tripMinPrice);
            applyNextDeparture(this, trip, now);
            if (trip.getTrain() != null && trip.getTrain().getCategory() != null) {
                trainCategories.add(trip.getTrain().getCategory());
            }
        }

        private int getTripsCount() {
            return tripsCount;
        }

        private BigDecimal getMinPrice() {
            return minPrice;
        }

        private PopularRouteResponse toResponse() {
            return new PopularRouteResponse(
                    departureStation.getId(),
                    departureStation.getName(),
                    departureStation.getCode(),
                    arrivalStation.getId(),
                    arrivalStation.getName(),
                    arrivalStation.getCode(),
                    tripsCount,
                    availableSeats,
                    minPrice,
                    nextDepartureTime,
                    new ArrayList<>(trainCategories)
            );
        }
    }

    private static class DestinationAggregate {
        private final Station station;
        private int tripsCount;
        private int availableSeats;
        private BigDecimal minPrice;
        private LocalDateTime nextDepartureTime;

        private DestinationAggregate(Station station) {
            this.station = station;
        }

        private void apply(Trip trip, LocalDateTime now, int tripAvailableSeats, BigDecimal tripMinPrice) {
            tripsCount++;
            availableSeats += tripAvailableSeats;
            minPrice = TripAppServiceImpl.minPrice(minPrice, tripMinPrice);
            applyNextDeparture(this, trip, now);
        }

        private int getTripsCount() {
            return tripsCount;
        }

        private BigDecimal getMinPrice() {
            return minPrice;
        }

        private PopularDestinationResponse toResponse() {
            return new PopularDestinationResponse(
                    station.getId(),
                    station.getName(),
                    station.getCode(),
                    station.getLocation(),
                    tripsCount,
                    availableSeats,
                    minPrice,
                    nextDepartureTime,
                    null
            );
        }
    }
}
