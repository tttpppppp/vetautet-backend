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
import com.vetautet.domain.model.DestinationSummary;
import com.vetautet.domain.model.Promotion;
import com.vetautet.domain.model.RouteSummary;
import com.vetautet.domain.model.Ticket;
import com.vetautet.domain.model.Trip;
import com.vetautet.domain.model.TripSegment;
import com.vetautet.domain.model.TripSummary;
import com.vetautet.domain.model.TripStop;
import com.vetautet.domain.repository.TripScheduleRepository;
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
import java.util.Comparator;
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
    private TripScheduleRepository tripScheduleRepository;

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
        return tripDomainService.getPopularTripSummaries(normalizeLimit(limit)).stream()
                .map(this::toTripResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PopularRouteResponse> getPopularRoutes(int limit) {
        return tripDomainService.getPopularRouteSummaries(normalizeLimit(limit)).stream()
                .map(this::toPopularRouteResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PopularDestinationResponse> getPopularDestinations(int limit) {
        return tripDomainService.getPopularDestinationSummaries(normalizeLimit(limit)).stream()
                .map(this::toPopularDestinationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripResponse> getUpcomingDepartures(int limit) {
        return tripDomainService.getUpcomingTripSummaries(normalizeLimit(limit)).stream()
                .map(this::toTripResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripResponse> getSchedules(LocalDate date, String station, int limit) {
        LocalDate scheduleDate = date != null ? date : LocalDate.now();
        String stationFilter = normalizeStation(station);

        return tripDomainService.getScheduleTripSummaries(scheduleDate, stationFilter, normalizeLimit(limit)).stream()
                .map(this::toTripResponse)
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
            log.debug("[TRIP APP CACHE] REDIS_OBJECT_HIT key={}", TRIP_CACHE_KEY);
            return cachedTrips;
        }

        RLock lock = redissonClient.getLock(TRIP_LOCK_KEY);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                @SuppressWarnings("unchecked")
                List<TripResponse> secondCheck = (List<TripResponse>) redisTemplate.opsForValue().get(TRIP_CACHE_KEY);
                if (secondCheck != null) {
                    log.debug("[TRIP APP CACHE] REDIS_OBJECT_HIT_AFTER_LOCK key={}", TRIP_CACHE_KEY);
                    return secondCheck;
                }

                log.debug("[TRIP APP CACHE] REDIS_OBJECT_MISS key={} -> query DB", TRIP_CACHE_KEY);
                List<TripResponse> trips = tripDomainService.getAllActiveTrips().stream()
                        .map(trip -> userMapper.toTripResponse(trip, false))
                        .collect(Collectors.toList());

                redisTemplate.opsForValue().set(TRIP_CACHE_KEY, trips, Duration.ofMinutes(10));
                log.debug("[TRIP APP CACHE] REDIS_OBJECT_STORED key={} ttl=10m", TRIP_CACHE_KEY);
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
            log.debug("[TRIP APP CACHE] REDIS_OBJECT_HIT tripId={} key={}", id, cacheKey);
            return cachedTrip;
        }

        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                cachedTrip = (TripResponse) redisTemplate.opsForValue().get(cacheKey);
                if (cachedTrip != null) {
                    log.debug("[TRIP APP CACHE] REDIS_OBJECT_HIT_AFTER_LOCK tripId={} key={}", id, cacheKey);
                    return cachedTrip;
                }

                log.debug("[TRIP APP CACHE] REDIS_OBJECT_MISS tripId={} key={} -> query DB", id, cacheKey);
                TripResponse trip = userMapper.toTripResponse(tripDomainService.getTripByIdFetched(id), true);

                redisTemplate.opsForValue().set(cacheKey, trip, Duration.ofMinutes(10));
                log.debug("[TRIP APP CACHE] REDIS_OBJECT_STORED tripId={} key={} ttl=10m", id, cacheKey);
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
        return getTripById(id, bookingId, null, null);
    }

    @Override
    public TripResponse getTripById(Long id, Long bookingId, Long departureStationId, Long arrivalStationId) {
        if (bookingId == null) {
            if (departureStationId == null || arrivalStationId == null) {
                return getTripById(id);
            }
            Trip tripDomain = tripDomainService.getTripByIdFetched(id);
            TripResponse trip = userMapper.toTripResponse(tripDomain, true);
            applyRouteSeatState(tripDomain, trip, departureStationId, arrivalStationId);
            return trip;
        }

        log.debug("[TRIP APP CACHE] SKIP_CACHE tripId={} bookingId={} reason=booking-specific-seat-state", id, bookingId);
        Trip tripDomain = tripDomainService.getTripByIdFetched(id);
        TripResponse trip = userMapper.toTripResponse(tripDomain, true);
        applyRouteSeatState(tripDomain, trip, departureStationId, arrivalStationId);
        markHeldSeatsByCurrentBooking(trip, bookingId);
        return trip;
    }

    private void applyRouteSeatState(Trip tripDomain, TripResponse trip, Long departureStationId, Long arrivalStationId) {
        if (tripDomain == null || trip == null || trip.getCarriages() == null
                || departureStationId == null || arrivalStationId == null) {
            return;
        }

        RouteSegments routeSegments = resolveRouteSegments(tripDomain.getId(), departureStationId, arrivalStationId);
        Map<Long, String> statusBySeatId = tripScheduleRepository.findSeatStatusesForSegments(
                tripDomain.getId(),
                routeSegments.segmentIds()
        );
        if (statusBySeatId.isEmpty()) {
            return;
        }

        Map<Long, Long> seatIdByTicketId = tripDomain.getTickets() == null
                ? Map.of()
                : tripDomain.getTickets().stream()
                .filter(ticket -> ticket.getId() != null && ticket.getSeatId() != null)
                .collect(Collectors.toMap(Ticket::getId, Ticket::getSeatId, (left, right) -> left));

        trip.getCarriages().stream()
                .map(CarriageResponse::getSeats)
                .filter(seats -> seats != null)
                .flatMap(List::stream)
                .forEach(seat -> {
                    Long seatId = seatIdByTicketId.get(seat.getId());
                    String routeStatus = seatId != null ? statusBySeatId.get(seatId) : null;
                    if (routeStatus != null) {
                        seat.setStatus(routeStatus);
                    }
                });
    }

    private RouteSegments resolveRouteSegments(Long tripId, Long departureStationId, Long arrivalStationId) {
        List<TripStop> stops = tripScheduleRepository.findStopsByTripId(tripId).stream()
                .sorted(Comparator.comparing(TripStop::getStopOrder))
                .collect(Collectors.toList());
        TripStop departureStop = findStopByStationId(stops, departureStationId);
        TripStop arrivalStop = findStopByStationId(stops, arrivalStationId);
        if (departureStop == null || arrivalStop == null || departureStop.getStopOrder() >= arrivalStop.getStopOrder()) {
            return new RouteSegments(List.of());
        }

        List<Long> segmentIds = tripScheduleRepository.findSegmentsByTripId(tripId).stream()
                .filter(segment -> segment.getSegmentOrder() >= departureStop.getStopOrder())
                .filter(segment -> segment.getSegmentOrder() < arrivalStop.getStopOrder())
                .sorted(Comparator.comparing(TripSegment::getSegmentOrder))
                .map(TripSegment::getId)
                .collect(Collectors.toList());
        return new RouteSegments(segmentIds);
    }

    private TripStop findStopByStationId(List<TripStop> stops, Long stationId) {
        if (stops == null || stationId == null) {
            return null;
        }
        return stops.stream()
                .filter(stop -> stop.getStation() != null && stationId.equals(stop.getStation().getId()))
                .findFirst()
                .orElse(null);
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

    private String normalizeStation(String station) {
        if (station == null || station.isBlank()) {
            return null;
        }
        return station.trim().toLowerCase();
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
        tripJsonCacheService.evictByPrefix(TripCacheKeys.HTTP_TRIPS_PREFIX);
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
        tripJsonCacheService.evict(TripCacheKeys.httpTrip(id));
        tripJsonCacheService.evictByPrefix(TripCacheKeys.HTTP_TRIPS_PREFIX);

        return userMapper.toTripResponse(saved, false);
    }

    @Override
    @Transactional
    public void deleteTrip(Long id) {
        tripDomainService.deleteTrip(id);
        redisTemplate.delete(TRIP_CACHE_KEY);
        redisTemplate.delete(TripCacheKeys.trip(id));
        tripJsonCacheService.evict(TripCacheKeys.httpTrip(id));
        tripJsonCacheService.evictByPrefix(TripCacheKeys.HTTP_TRIPS_PREFIX);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 6;
        }
        return Math.min(limit, 12);
    }

    private TripResponse toTripResponse(TripSummary summary) {
        TripResponse response = new TripResponse();
        response.setId(summary.id());
        response.setTrainCode(summary.trainCode());
        response.setTrainCategory(summary.trainCategory());
        response.setDepartureStation(summary.departureStation());
        response.setArrivalStation(summary.arrivalStation());
        response.setDepartureTime(summary.departureTime());
        response.setArrivalTime(summary.arrivalTime());
        response.setDuration(summary.duration());
        response.setPrice(summary.minPrice());
        response.setMinPrice(summary.minPrice());
        response.setAvailableSeats(toInteger(summary.availableSeats()));
        response.setTotalSeats(toInteger(summary.totalSeats()));
        response.setStatus(summary.status());
        response.setCarriages(null);
        return response;
    }

    private PopularRouteResponse toPopularRouteResponse(RouteSummary summary) {
        return new PopularRouteResponse(
                summary.departureStationId(),
                summary.departureStation(),
                summary.departureStationCode(),
                summary.arrivalStationId(),
                summary.arrivalStation(),
                summary.arrivalStationCode(),
                toInteger(summary.tripsCount()),
                toInteger(summary.availableSeats()),
                summary.minPrice(),
                summary.nextDepartureTime(),
                splitCsv(summary.trainCategoriesCsv())
        );
    }

    private PopularDestinationResponse toPopularDestinationResponse(DestinationSummary summary) {
        return new PopularDestinationResponse(
                summary.stationId(),
                summary.stationName(),
                summary.stationCode(),
                summary.location(),
                toInteger(summary.tripsCount()),
                toInteger(summary.availableSeats()),
                summary.minPrice(),
                summary.nextDepartureTime(),
                null
        );
    }

    private Integer toInteger(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
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

    private record RouteSegments(List<Long> segmentIds) {
    }
}
