package com.vetautet.application.service.order.impl;

import com.vetautet.application.dto.BookingHistoryResponse;
import com.vetautet.application.dto.BookingDetailResponse;
import com.vetautet.application.dto.BookingRequest;
import com.vetautet.application.dto.BookingResponse;
import com.vetautet.application.cache.TripCacheKeys;
import com.vetautet.application.cache.TripJsonCacheService;
import com.vetautet.application.service.order.BookingAppService;
import com.vetautet.application.service.order.BookingInvoicePdfService;
import com.vetautet.domain.exception.BusinessException;
import com.vetautet.domain.gateway.OutboxEventGateway;
import com.vetautet.domain.gateway.SeatCacheGateway;
import com.vetautet.domain.model.*;
import com.vetautet.domain.repository.BookingRepository;
import com.vetautet.domain.repository.TripScheduleRepository;
import com.vetautet.domain.service.BookingDomainService;
import com.vetautet.domain.service.PassengerFareRuleDomainService;
import com.vetautet.domain.service.PaymentDomainService;
import com.vetautet.domain.service.PromotionDomainService;
import com.vetautet.domain.service.TicketDomainService;
import com.vetautet.domain.service.TripDomainService;
import com.vetautet.domain.service.UserDomainService;
import com.vetautet.domain.security.SensitiveDataCryptoService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BookingAppServiceImpl implements BookingAppService {

    @Autowired
    private TicketDomainService ticketDomainService;

    @Autowired
    private BookingDomainService bookingDomainService;

    @Autowired
    private PaymentDomainService paymentDomainService;

    @Autowired
    private PromotionDomainService promotionDomainService;

    @Autowired
    private PassengerFareRuleDomainService passengerFareRuleDomainService;

    @Autowired
    private TripDomainService tripDomainService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SeatCacheGateway seatCacheGateway;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TripJsonCacheService tripJsonCacheService;

    @Autowired
    private TripScheduleRepository tripScheduleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingInvoicePdfService bookingInvoicePdfService;

    @Autowired
    private OutboxEventGateway outboxEventGateway;

    @Autowired
    private SensitiveDataCryptoService sensitiveDataCryptoService;

    private static final int MAX_SEATS_PER_BOOKING = 10;
    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String BOOKING_CREATE_REQUESTED_TOPIC = "booking-create-requested";
    private static final String PAYMENT_CONFIRMED_TOPIC = "payment-confirmed";
    private static final String BOOKING_SAGA_EVENTS_TOPIC = "booking-saga-events";
    private static final String BOOKING_LOCK_PREFIX = "lock:ticket:";
    private static final String SEAT_HOLD_KEY_PREFIX = "seat:"; 
    private static final long SEAT_HOLD_TTL_MINUTES = 15;

    @Override
    @Transactional
    public BookingResponse enqueueCreateBooking(Long userId, BookingRequest request) {
        validateCreateBookingRequest(request);
        if (userId == null) {
            throw new RuntimeException("Ban can dang nhap de dat ve");
        }

        return createBookingForUser(userId, request, null);
    }

    private void queueBookingRequestSeats(Long userId, BookingRequest request) {
        List<BookingLegRequest> legRequests = resolveBookingLegRequests(request);
        List<Long> requestedTicketIds = allRequestedTicketIds(request);
        List<Long> sortedIds = requestedTicketIds.stream().distinct().sorted().collect(Collectors.toList());
        if (sortedIds.size() != requestedTicketIds.size()) {
            throw new RuntimeException("Duplicate ticket id in booking request");
        }

        RLock[] locks = sortedIds.stream()
                .map(id -> redissonClient.getLock(BOOKING_LOCK_PREFIX + id))
                .toArray(RLock[]::new);
        RLock multiLock = redissonClient.getMultiLock(locks);
        boolean isLocked = false;
        try {
            if (!multiLock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new RuntimeException("He thong dang ban xu ly ghe ban chon, vui long thu lai sau vai giay");
            }
            isLocked = true;

            List<Ticket> tickets = ticketDomainService.getTicketsByIds(requestedTicketIds);
            if (tickets.size() != requestedTicketIds.size()) {
                throw new RuntimeException("Mot so ghe khong ton tai hoac du lieu bi thay doi");
            }

            Map<Long, Ticket> ticketById = tickets.stream()
                    .collect(Collectors.toMap(Ticket::getId, ticket -> ticket));
            List<PreparedBookingLeg> preparedLegs = prepareBookingLegs(legRequests, ticketById, request);

            for (PreparedBookingLeg leg : preparedLegs) {
                for (Ticket ticket : leg.tickets()) {
                    if (ticket.getSeatId() == null) {
                        throw new RuntimeException("Cannot determine seat for ticket " + ticket.getId());
                    }
                    if (!tripScheduleRepository.areSeatSegmentsAvailable(
                            leg.routeSelection().tripId(),
                            ticket.getSeatId(),
                            leg.routeSelection().segmentIds())) {
                        throw new RuntimeException("Ghe " + ticket.getSeatNumber() + " khong con trong cho chang da chon");
                    }
                }
            }

            LocalDateTime queueExpiredAt = LocalDateTime.now().plusMinutes(SEAT_HOLD_TTL_MINUTES);
            for (Ticket ticket : tickets) {
                ticket.setStatus("QUEUED");
                ticket.setHoldExpiredAt(queueExpiredAt);
            }
            ticketDomainService.saveTickets(tickets);

            for (PreparedBookingLeg leg : preparedLegs) {
                for (Ticket ticket : leg.tickets()) {
                    tripScheduleRepository.queueSeatSegments(
                            leg.routeSelection().tripId(),
                            ticket.getSeatId(),
                            leg.routeSelection().segmentIds(),
                            queueExpiredAt
                    );
                }
            }

            afterCommit(() -> evictTripReadCachesForTickets(tickets, null));
            preparedLegs.forEach(leg -> leg.tickets().forEach(ticket -> afterCommit(() -> messagingTemplate.convertAndSend(
                    "/topic/trips/" + leg.routeSelection().tripId() + "/seats",
                    SeatStatusEvent.builder()
                            .tripId(leg.routeSelection().tripId())
                            .ticketId(ticket.getId())
                            .seatNumber(ticket.getSeatNumber())
                            .status("QUEUED")
                            .bookingId(null)
                            .departureStationId(leg.routeSelection().departureStationId())
                            .arrivalStationId(leg.routeSelection().arrivalStationId())
                            .segmentIds(leg.routeSelection().segmentIdsCsv())
                            .build()
            ))));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qua trinh dat ve bi gian doan");
        } finally {
            if (isLocked) {
                try {
                    multiLock.unlock();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @Transactional
    public BookingResponse createBookingForUser(Long userId, BookingRequest request, String asyncRequestId) {
        validateCreateBookingRequest(request);
        if (asyncRequestId != null && !asyncRequestId.isBlank()) {
            Booking existing = bookingDomainService.findBookingByAsyncRequestIdOrNull(asyncRequestId);
            if (existing != null) {
                return toResponse(existing);
            }
        }

        User user = userDomainService.getById(userId);
        return createBookingInternal(request, user, asyncRequestId);
    }

    @Override
    @Transactional
    public void releaseQueuedBooking(Long userId, BookingRequest request) {
        if (request == null) {
            return;
        }
        List<Long> requestedTicketIds = allRequestedTicketIds(request);
        if (requestedTicketIds.isEmpty()) {
            return;
        }

        List<Long> sortedIds = requestedTicketIds.stream().distinct().sorted().collect(Collectors.toList());
        RLock[] locks = sortedIds.stream()
                .map(id -> redissonClient.getLock(BOOKING_LOCK_PREFIX + id))
                .toArray(RLock[]::new);
        RLock multiLock = redissonClient.getMultiLock(locks);
        boolean isLocked = false;
        try {
            if (!multiLock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return;
            }
            isLocked = true;

            List<Ticket> tickets = ticketDomainService.getTicketsByIds(requestedTicketIds);
            Map<Long, Ticket> ticketById = tickets.stream()
                    .collect(Collectors.toMap(Ticket::getId, ticket -> ticket));
            List<PreparedBookingLeg> preparedLegs = prepareBookingLegs(resolveBookingLegRequests(request), ticketById, request);

            List<Ticket> queuedTickets = tickets.stream()
                    .filter(ticket -> "QUEUED".equalsIgnoreCase(ticket.getStatus()))
                    .collect(Collectors.toList());
            for (Ticket ticket : queuedTickets) {
                ticket.setStatus("AVAILABLE");
                ticket.setHoldExpiredAt(null);
            }
            if (!queuedTickets.isEmpty()) {
                ticketDomainService.saveTickets(queuedTickets);
            }

            for (PreparedBookingLeg leg : preparedLegs) {
                for (Ticket ticket : leg.tickets()) {
                    tripScheduleRepository.releaseQueuedSeatSegments(
                            leg.routeSelection().tripId(),
                            ticket.getSeatId(),
                            leg.routeSelection().segmentIds()
                    );
                }
            }

            afterCommit(() -> evictTripReadCachesForTickets(tickets, null));
            preparedLegs.forEach(leg -> leg.tickets().forEach(ticket -> afterCommit(() -> messagingTemplate.convertAndSend(
                    "/topic/trips/" + leg.routeSelection().tripId() + "/seats",
                    SeatStatusEvent.builder()
                            .tripId(leg.routeSelection().tripId())
                            .ticketId(ticket.getId())
                            .seatNumber(ticket.getSeatNumber())
                            .status("AVAILABLE")
                            .bookingId(null)
                            .departureStationId(leg.routeSelection().departureStationId())
                            .arrivalStationId(leg.routeSelection().arrivalStationId())
                            .segmentIds(leg.routeSelection().segmentIdsCsv())
                            .build()
            ))));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (isLocked) {
                try {
                    multiLock.unlock();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        validateCreateBookingRequest(request);

        // 1. Kiểm tra số lượng ghế (Tối đa 4 ghế)
        if (request.getTicketIds() == null || request.getTicketIds().isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 ghế");
        }

        if (request.getTicketIds().size() > MAX_SEATS_PER_BOOKING) {
            throw new RuntimeException("Bạn chỉ được phép đặt tối đa " + MAX_SEATS_PER_BOOKING + " ghế mỗi lần");
        }

        // 2. Lấy thông tin User hiện tại
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Bạn cần đăng nhập để đặt vé nhé!");
        }
        
        String email = auth.getName();
        User user = userDomainService.getByEmail(email);
        return createBookingInternal(request, user, null);
    }

    private BookingResponse createBookingInternal(BookingRequest request, User user, String asyncRequestId) {
        String tripType = resolveTripType(request);
        List<BookingLegRequest> legRequests = resolveBookingLegRequests(request);
        List<Long> requestedTicketIds = allRequestedTicketIds(request);
        List<Long> sortedIds = requestedTicketIds.stream().distinct().sorted().collect(Collectors.toList());
        if (sortedIds.size() != requestedTicketIds.size()) {
            throw new RuntimeException("Duplicate ticket id in booking request");
        }

        RLock[] locks = sortedIds.stream()
                .map(id -> redissonClient.getLock(BOOKING_LOCK_PREFIX + id))
                .toArray(RLock[]::new);
        RLock multiLock = redissonClient.getMultiLock(locks);

        boolean isLocked = false;
        List<Ticket> heldTickets = new ArrayList<>();
        Long savedBookingIdForSaga = null;
        Long userIdForSaga = user.getId();
        try {
            if (multiLock.tryLock(3, 10, TimeUnit.SECONDS)) {
                isLocked = true;

                List<Ticket> tickets = ticketDomainService.getTicketsByIds(requestedTicketIds);
                if (tickets.size() != requestedTicketIds.size()) {
                    throw new RuntimeException("Mot so ghe khong ton tai hoac du lieu bi thay doi");
                }

                Map<Long, Ticket> ticketById = tickets.stream()
                        .collect(Collectors.toMap(Ticket::getId, ticket -> ticket));
                List<PreparedBookingLeg> preparedLegs = prepareBookingLegs(legRequests, ticketById, request);

                for (PreparedBookingLeg leg : preparedLegs) {
                    for (Ticket ticket : leg.tickets()) {
                        if (ticket.getSeatId() == null) {
                            throw new RuntimeException("Cannot determine seat for ticket " + ticket.getId());
                        }
                        boolean seatBookable = asyncRequestId != null
                                ? tripScheduleRepository.areSeatSegmentsBookable(
                                        leg.routeSelection().tripId(),
                                        ticket.getSeatId(),
                                        leg.routeSelection().segmentIds())
                                : tripScheduleRepository.areSeatSegmentsAvailable(
                                        leg.routeSelection().tripId(),
                                        ticket.getSeatId(),
                                        leg.routeSelection().segmentIds());
                        if (!seatBookable) {
                            throw new RuntimeException("Ghe " + ticket.getSeatNumber() + " khong con trong cho chang da chon");
                        }
                    }
                }

                LocalDateTime holdExpiredAt = LocalDateTime.now().plusMinutes(SEAT_HOLD_TTL_MINUTES);
                for (Ticket ticket : tickets) {
                    ticket.setStatus("HOLD");
                    ticket.setHoldExpiredAt(holdExpiredAt);
                    heldTickets.add(ticket);
                }
                ticketDomainService.saveTickets(heldTickets);
                afterRollback(() -> cleanupSeatHoldsAfterRollback(heldTickets, null));

                BigDecimal originalPrice = preparedLegs.stream()
                        .flatMap(leg -> leg.segmentPricesByTicketId().values().stream())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                PromotionDiscount promotionDiscount = resolvePromotionDiscount(request.getPromoCode(), originalPrice);
                BigDecimal totalPrice = originalPrice.subtract(promotionDiscount.discountAmount()).max(BigDecimal.ZERO);

                Booking booking = Booking.builder()
                        .user(user)
                        .asyncRequestId(asyncRequestId)
                        .tripType(tripType)
                        .originalPrice(originalPrice)
                        .promoCode(promotionDiscount.promoCode())
                        .discountAmount(promotionDiscount.discountAmount())
                        .totalPrice(totalPrice)
                        .contactName(firstNonBlank(request.getContactName(), user.getName()))
                        .contactEmail(firstNonBlank(request.getContactEmail(), user.getEmail()))
                        .contactPhone(firstNonBlank(request.getContactPhone(), user.getPhone()))
                        .contactIdCard(encryptSensitiveTextOrNull(request.getContactIdCard()))
                        .status("PENDING")
                        .expiredAt(LocalDateTime.now().plusMinutes(15))
                        .build();

                List<BookingDetail> details = preparedLegs.stream()
                        .flatMap(leg -> leg.tickets().stream().map(ticket -> {
                            BookingRequest.PassengerDetails pDetails = findPassengerDetails(
                                    request,
                                    ticket.getId(),
                                    leg.direction()
                            );
                            return BookingDetail.builder()
                                    .ticket(ticket)
                                    .direction(leg.direction())
                                    .departureStationId(leg.routeSelection().departureStationId())
                                    .arrivalStationId(leg.routeSelection().arrivalStationId())
                                    .segmentIds(leg.routeSelection().segmentIdsCsv())
                                    .segmentPrice(leg.segmentPricesByTicketId().get(ticket.getId()))
                                    .passengerName(pDetails != null ? pDetails.getName() : "Khach hang")
                                    .passengerIdCard(encryptSensitiveText(pDetails != null ? pDetails.getIdCard() : "N/A"))
                                    .passengerType(passengerTypeOf(pDetails))
                                    .booking(booking)
                                    .build();
                        }))
                        .collect(Collectors.toList());
                booking.setDetails(details);

                Booking savedBooking = bookingDomainService.saveBooking(booking);
                savedBookingIdForSaga = savedBooking.getId();
                holdSegmentInventory(savedBooking, preparedLegs, holdExpiredAt);

                List<Ticket> cacheEvictTickets = List.copyOf(heldTickets);
                afterCommit(() -> evictTripReadCachesForTickets(cacheEvictTickets, null));

                Long savedBookingId = savedBooking.getId();
                Map<String, RouteSelection> routeByDirection = preparedLegs.stream()
                        .collect(Collectors.toMap(PreparedBookingLeg::direction, PreparedBookingLeg::routeSelection));
                savedBooking.getDetails().forEach(detail -> {
                    Ticket ticket = detail.getTicket();
                    RouteSelection routeSelection = routeByDirection.get(normalizeDirection(detail.getDirection()));
                    if (ticket == null || routeSelection == null) {
                        return;
                    }
                    Long currentTripId = routeSelection.tripId();
                    afterCommit(() -> messagingTemplate.convertAndSend(
                            "/topic/trips/" + currentTripId + "/seats",
                            SeatStatusEvent.builder()
                                    .tripId(currentTripId)
                                    .ticketId(ticket.getId())
                                    .seatNumber(ticket.getSeatNumber())
                                    .status("HOLD")
                                    .bookingId(savedBookingId)
                                    .departureStationId(routeSelection.departureStationId())
                                    .arrivalStationId(routeSelection.arrivalStationId())
                                    .segmentIds(routeSelection.segmentIdsCsv())
                                    .build()
                    ));
                });

                enqueueOutboxEvent(
                        ORDER_CREATED_TOPIC,
                        savedBooking.getId().toString(),
                        "ORDER_CREATED",
                        "BOOKING",
                        savedBooking.getId(),
                        savedBooking.getId(),
                        "ORDER_CREATED"
                );

                BookingResponse response = BookingResponse.builder()
                        .bookingId(savedBooking.getId())
                        .requestId(savedBooking.getAsyncRequestId())
                        .orderNumber(savedBooking.getOrderNumber())
                        .storageMonth(savedBooking.getStorageMonth())
                        .tripType(savedBooking.getTripType())
                        .status(savedBooking.getStatus())
                        .originalPrice(originalPriceOf(savedBooking))
                        .promoCode(savedBooking.getPromoCode())
                        .discountAmount(discountAmountOf(savedBooking))
                        .totalPrice(savedBooking.getTotalPrice())
                        .expiredAt(savedBooking.getExpiredAt())
                        .seatNumbers(tickets.stream().map(Ticket::getSeatNumber).collect(Collectors.toList()))
                        .ticketIds(tickets.stream().map(Ticket::getId).collect(Collectors.toList()))
                        .build();
                pushBookingRealtimeAfterCommit(user.getId(), response);
                return response;
            }
            throw new RuntimeException("He thong dang ban xu ly ghe ban chon, vui long thu lai sau vai giay");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            compensateCreateBookingFailure(heldTickets, null, savedBookingIdForSaga, userIdForSaga, e);
            throw new RuntimeException("Qua trinh dat ve bi gian doan");
        } catch (RuntimeException e) {
            compensateCreateBookingFailure(heldTickets, null, savedBookingIdForSaga, userIdForSaga, e);
            throw e;
        } finally {
            if (isLocked) {
                try {
                    multiLock.unlock();
                } catch (Exception e) {
                    // Lock might have automatically expired
                }
            }
        }
    }

    private BookingResponse createBookingInternalLegacy(BookingRequest request, User user, String asyncRequestId) {

        // 3. Sử dụng Redisson MultiLock để khóa các Ticket đang đặt
        // Sắp xếp ID ticket để tránh Deadlock
        List<Long> sortedIds = request.getTicketIds().stream().sorted().collect(Collectors.toList());
        RLock[] locks = sortedIds.stream()
                .map(id -> redissonClient.getLock(BOOKING_LOCK_PREFIX + id))
                .toArray(RLock[]::new);
        
        RLock multiLock = redissonClient.getMultiLock(locks);

        boolean isLocked = false;
        List<Ticket> heldTickets = new ArrayList<>();
        Long savedBookingIdForSaga = null;
        Long userIdForSaga = user.getId();
        try {
            // Chờ tối đa 3 giây để lấy lock cho toàn bộ ghế, giữ lock 10 giây
            if (multiLock.tryLock(3, 10, TimeUnit.SECONDS)) {
                isLocked = true;
                
                // 4. Lấy danh sách Ticket từ DB (Lúc này đã có lock nên an toàn)
                List<Ticket> tickets = ticketDomainService.getTicketsByIds(request.getTicketIds());
                
                if (tickets.size() != request.getTicketIds().size()) {
                    throw new RuntimeException("Một số ghế không tồn tại hoặc dữ liệu bị thay đổi");
                }

                Long bookingTripId = resolveBookingTripId(request, tickets);
                RouteSelection routeSelection = resolveRouteSelection(bookingTripId, request);
                Map<Long, BigDecimal> segmentPricesByTicketId = resolveSegmentPricesByTicket(tickets, routeSelection, request, "OUTBOUND");

                for (Ticket ticket : tickets) {
                    if (ticket.getTripId() == null && request.getTripId() == null) {
                        throw new RuntimeException("Ghế " + ticket.getSeatNumber() + " không thuộc về chuyến tàu nào hợp lệ.");
                    }
                    Long tripId = ticket.getTripId() != null ? ticket.getTripId() : request.getTripId();

                    if (!tripScheduleRepository.areSeatSegmentsAvailable(bookingTripId, ticket.getSeatId(), routeSelection.segmentIds())) {
                        throw new RuntimeException("Ghe " + ticket.getSeatNumber() + " khong con trong cho chang da chon");
                    }
                }

                // Segment inventory is the source of truth; ticket status remains for legacy screens and QR checks.
                LocalDateTime holdExpiredAt = LocalDateTime.now().plusMinutes(SEAT_HOLD_TTL_MINUTES);
                for (Ticket ticket : tickets) {
                    ticket.setStatus("HOLD");
                    ticket.setHoldExpiredAt(holdExpiredAt);
                    heldTickets.add(ticket);
                }
                ticketDomainService.saveTickets(heldTickets);
                afterRollback(() -> cleanupSeatHoldsAfterRollback(heldTickets, bookingTripId));

                BigDecimal originalPrice = segmentPricesByTicketId.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                PromotionDiscount promotionDiscount = resolvePromotionDiscount(request.getPromoCode(), originalPrice);
                BigDecimal totalPrice = originalPrice.subtract(promotionDiscount.discountAmount()).max(BigDecimal.ZERO);

                Booking booking = Booking.builder()
                        .user(user)
                        .asyncRequestId(asyncRequestId)
                        .originalPrice(originalPrice)
                        .promoCode(promotionDiscount.promoCode())
                        .discountAmount(promotionDiscount.discountAmount())
                        .totalPrice(totalPrice)
                        .contactName(firstNonBlank(request.getContactName(), user.getName()))
                        .contactEmail(firstNonBlank(request.getContactEmail(), user.getEmail()))
                        .contactPhone(firstNonBlank(request.getContactPhone(), user.getPhone()))
                        .contactIdCard(encryptSensitiveTextOrNull(request.getContactIdCard()))
                        .status("PENDING")
                        .expiredAt(LocalDateTime.now().plusMinutes(15))
                        .build();

                List<BookingDetail> details = tickets.stream()
                        .map(ticket -> {
                            // Tìm thông tin passenger khớp với ticketId (nếu có)
                            BookingRequest.PassengerDetails pDetails = null;
                            if (request.getPassengers() != null) {
                                pDetails = request.getPassengers().stream()
                                        .filter(p -> p.getTicketId().equals(ticket.getId()))
                                        .findFirst().orElse(null);
                            }

                            return BookingDetail.builder()
                                    .ticket(ticket)
                                    .departureStationId(routeSelection.departureStationId())
                                    .arrivalStationId(routeSelection.arrivalStationId())
                                    .segmentIds(routeSelection.segmentIdsCsv())
                                    .segmentPrice(segmentPricesByTicketId.get(ticket.getId()))
                                    .passengerName(pDetails != null ? pDetails.getName() : "Khách hàng")
                                    .passengerIdCard(encryptSensitiveText(pDetails != null ? pDetails.getIdCard() : "N/A"))
                                    .passengerType(passengerTypeOf(pDetails))
                                    .booking(booking)
                                    .build();
                        })
                        .collect(Collectors.toList());

                booking.setDetails(details);

                // 8. Lưu Booking
                Booking savedBooking = bookingDomainService.saveBooking(booking);
                savedBookingIdForSaga = savedBooking.getId();
                holdSegmentInventory(savedBooking, routeSelection, holdExpiredAt);

                // 9. Xóa cache sau commit để request khác không rebuild cache từ DB chưa commit.
                List<Ticket> cacheEvictTickets = List.copyOf(heldTickets);
                Long cacheEvictFallbackTripId = bookingTripId;
                afterCommit(() -> evictTripReadCachesForTickets(cacheEvictTickets, cacheEvictFallbackTripId));

                Long savedBookingId = savedBooking.getId();
                tickets.forEach(ticket -> {
                    Long currentTripId = bookingTripId;
                    afterCommit(() -> messagingTemplate.convertAndSend(
                            "/topic/trips/" + currentTripId + "/seats",
                            SeatStatusEvent.builder()
                                    .tripId(currentTripId)
                                    .ticketId(ticket.getId())
                                    .seatNumber(ticket.getSeatNumber())
                                    .status("HOLD")
                                    .bookingId(savedBookingId)
                                    .departureStationId(routeSelection.departureStationId())
                                    .arrivalStationId(routeSelection.arrivalStationId())
                                    .segmentIds(routeSelection.segmentIdsCsv())
                                    .build()
                    ));
                });

                // 10. Gửi message vào Kafka để xử lý timeout sau này nếu local đang bật Kafka.
                enqueueOutboxEvent(
                        ORDER_CREATED_TOPIC,
                        savedBooking.getId().toString(),
                        "ORDER_CREATED",
                        "BOOKING",
                        savedBooking.getId(),
                        savedBooking.getId(),
                        "ORDER_CREATED"
                );
                System.out.println(">>> [BOOKING LOCKED] Thành công giữ chỗ cho booking: " + savedBooking.getId());

                BookingResponse response = BookingResponse.builder()
                        .bookingId(savedBooking.getId())
                        .requestId(savedBooking.getAsyncRequestId())
                        .orderNumber(savedBooking.getOrderNumber())
                        .storageMonth(savedBooking.getStorageMonth())
                        .status(savedBooking.getStatus())
                        .originalPrice(originalPriceOf(savedBooking))
                        .promoCode(savedBooking.getPromoCode())
                        .discountAmount(discountAmountOf(savedBooking))
                        .totalPrice(savedBooking.getTotalPrice())
                        .expiredAt(savedBooking.getExpiredAt())
                        .seatNumbers(tickets.stream().map(Ticket::getSeatNumber).collect(Collectors.toList()))
                        .ticketIds(tickets.stream().map(Ticket::getId).collect(Collectors.toList()))
                        .build();
                pushBookingRealtimeAfterCommit(user.getId(), response);
                return response;
            } else {
                throw new RuntimeException("Hệ thống đang bận xử lý ghế bạn chọn, vui lòng thử lại sau vài giây!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            compensateCreateBookingFailure(heldTickets, request.getTripId(), savedBookingIdForSaga, userIdForSaga, e);
            throw new RuntimeException("Quá trình đặt vé bị gián đoạn");
        } catch (RuntimeException e) {
            compensateCreateBookingFailure(heldTickets, request.getTripId(), savedBookingIdForSaga, userIdForSaga, e);
            throw e;
        } finally {
            if (isLocked) {
                try {
                    multiLock.unlock();
                } catch (Exception e) {
                    // Lock might have automatically expired
                }
            }
        }
    }

    @Override
    @Transactional
    public BookingResponse updateBookingDetails(Long bookingId, BookingRequest request) {
        // 1. Tìm đơn hàng
        Booking booking = bookingDomainService.getBookingById(bookingId);

        if (!"PENDING".equals(booking.getStatus())) {
            throw new RuntimeException("Chỉ có thể cập nhật thông tin cho đơn hàng đang chờ thanh toán");
        }

        booking.setContactName(firstNonBlank(request.getContactName(), booking.getContactName()));
        booking.setContactEmail(firstNonBlank(request.getContactEmail(), booking.getContactEmail()));
        booking.setContactPhone(firstNonBlank(request.getContactPhone(), booking.getContactPhone()));
        if (request.getContactIdCard() != null) {
            booking.setContactIdCard(encryptSensitiveTextOrNull(request.getContactIdCard()));
        }

        // 2. Cập nhật thông tin chi tiết từng hành khách
        if (request.getPassengers() != null) {
            validateChildPassengersShareCarriageWithAdult(
                    null,
                    passengersForBookingValidation(booking, request.getPassengers()),
                    ticketByIdFromBooking(booking)
            );
            for (BookingRequest.PassengerDetails pDetails : request.getPassengers()) {
                booking.getDetails().stream()
                        .filter(detail -> detail.getTicket().getId().equals(pDetails.getTicketId()))
                        .findFirst()
                        .ifPresent(detail -> {
                            detail.setPassengerName(pDetails.getName());
                            detail.setPassengerIdCard(encryptSensitiveText(pDetails.getIdCard()));
                            detail.setPassengerType(passengerTypeOf(pDetails));
                        });
            }
        }

        // 3. Lưu lại
        Booking savedBooking = bookingDomainService.saveBooking(booking);

        return toResponse(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponse confirmPayment(Long bookingId) {
        System.out.println(">>> [PAYMENT] Xử lý xác nhận thanh toán cho Booking: " + bookingId);
        // 1. Cập nhật trạng thái
        BookingResponse response = updateBookingStatus(bookingId, "CONFIRMED");
        
        // 2. Gửi Kafka để thực hiện Notify (Email/SMS/Ticket PDF)
        enqueueOutboxEvent(
                PAYMENT_CONFIRMED_TOPIC,
                bookingId.toString(),
                "PAYMENT_CONFIRMED",
                "BOOKING",
                bookingId,
                bookingId,
                "PAYMENT_CONFIRMED"
        );
        
        return response;
    }

    @Override
    public List<BookingHistoryResponse> getMyBookings(Long userId) {
        Map<Long, Trip> tripCache = new HashMap<>();
        return bookingDomainService.getBookingsByUserId(userId).stream()
                .map(booking -> toHistoryResponse(booking, tripCache))
                .collect(Collectors.toList());
    }

    @Override
    public BookingDetailResponse getMyBookingDetail(Long userId, Long bookingId) {
        Booking booking = bookingDomainService.getBookingByIdFetched(bookingId);
        if (booking.getUser() == null || !userId.equals(booking.getUser().getId())) {
            throw new RuntimeException("Ban khong co quyen xem booking nay");
        }
        return toDetailResponse(booking);
    }

    @Override
    public BookingDetailResponse getMyBookingDetailByOrderNumber(Long userId, String orderNumber) {
        Booking booking = bookingDomainService.getBookingByOrderNumber(orderNumber);
        if (booking.getUser() == null || !userId.equals(booking.getUser().getId())) {
            throw new RuntimeException("Ban khong co quyen xem booking nay");
        }
        Booking fetchedBooking = bookingDomainService.getBookingByIdFetched(booking.getId());
        return toDetailResponse(fetchedBooking);
    }

    @Override
    public byte[] generateMyBookingInvoicePdf(Long userId, Long bookingId) {
        BookingDetailResponse booking = getMyBookingDetail(userId, bookingId);
        ensureInvoiceCanBeIssued(booking);
        return bookingInvoicePdfService.generate(booking);
    }

    @Override
    public List<BookingResponse> getAllBookings() {
        return bookingDomainService.getAllBookings().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponse getBookingById(Long id) {
        return toResponse(bookingDomainService.getBookingById(id));
    }

    @Override
    public BookingResponse getBookingByOrderNumber(String orderNumber) {
        return toResponse(bookingDomainService.getBookingByOrderNumber(orderNumber));
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(Long id, String status) {
        Booking booking = bookingDomainService.getBookingById(id);
        boolean shouldCountPromotionUse = "CONFIRMED".equalsIgnoreCase(status)
                && !"CONFIRMED".equalsIgnoreCase(booking.getStatus())
                && booking.getPromoCode() != null
                && !booking.getPromoCode().isBlank();
        
        booking.setStatus(status);
        
        // Theo quy trình: Hold -> PENDING -> (Payment) -> CONFIRMED -> Xóa lock Redis
        if ("CONFIRMED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
            booking.getDetails().forEach(detail -> {
                Ticket ticket = detail.getTicket();
                String newStatus = "AVAILABLE";
                if ("CONFIRMED".equalsIgnoreCase(status)) {
                    updateSegmentInventoryForBookingDetail(detail, "BOOKED", null);
                    ticket.setStatus("BOOKED"); // Trong DB vẫn lưu trạng thái ghế là BOOKED (Đã bán)
                    newStatus = "BOOKED";
                    ticket.setHoldExpiredAt(null);
                    ticketDomainService.saveTicket(ticket);
                } else {
                    updateSegmentInventoryForBookingDetail(detail, "AVAILABLE", null);
                    ticket.setStatus("AVAILABLE");
                    ticket.setHoldExpiredAt(null);
                    ticketDomainService.saveTicket(ticket);
                }
                
                Long tripId = ticket.getTripId();
                Long ticketId = ticket.getId();
                String seatNumber = ticket.getSeatNumber();
                String statusEvent = newStatus;
                Long departureStationId = detail.getDepartureStationId();
                Long arrivalStationId = detail.getArrivalStationId();
                String segmentIds = detail.getSegmentIds();
                String redisKey = SEAT_HOLD_KEY_PREFIX + tripId + ":" + ticketId;

                afterCommit(() -> {
                    redisTemplate.delete(redisKey);
                    evictTripReadCaches(tripId);
                    messagingTemplate.convertAndSend(
                            "/topic/trips/" + tripId + "/seats",
                            SeatStatusEvent.builder()
                                    .tripId(tripId)
                                    .ticketId(ticketId)
                                    .seatNumber(seatNumber)
                                    .status(statusEvent)
                                    .bookingId(id)
                                    .departureStationId(departureStationId)
                                    .arrivalStationId(arrivalStationId)
                                    .segmentIds(segmentIds)
                                    .build()
                    );
                });
            });
        }

        if (shouldCountPromotionUse) {
            incrementPromotionUsage(booking.getPromoCode());
        }
        
        Booking saved = bookingDomainService.saveBooking(booking);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(LocalDateTime.now());
        int expiredCount = 0;
        for (Booking booking : expiredBookings) {
            if (booking.getId() == null || !"PENDING".equalsIgnoreCase(booking.getStatus())) {
                continue;
            }
            updateBookingStatus(booking.getId(), "EXPIRED");
            expiredCount++;
        }
        return expiredCount;
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingDomainService.findBookingByIdOrNull(id);
        if (booking != null) {
            booking.getDetails().forEach(detail -> {
                updateSegmentInventoryForBookingDetail(detail, "AVAILABLE", null);
                Ticket ticket = detail.getTicket();
                ticket.setStatus("AVAILABLE");
                ticket.setHoldExpiredAt(null);
                ticketDomainService.saveTicket(ticket);
                
                Long tripId = ticket.getTripId();
                Long ticketId = ticket.getId();
                String seatNumber = ticket.getSeatNumber();
                Long departureStationId = detail.getDepartureStationId();
                Long arrivalStationId = detail.getArrivalStationId();
                String segmentIds = detail.getSegmentIds();
                String redisKey = SEAT_HOLD_KEY_PREFIX + tripId + ":" + ticketId;

                afterCommit(() -> {
                    redisTemplate.delete(redisKey);
                    evictTripReadCaches(tripId);
                    messagingTemplate.convertAndSend(
                            "/topic/trips/" + tripId + "/seats",
                            SeatStatusEvent.builder()
                                    .tripId(tripId)
                                    .ticketId(ticketId)
                                    .seatNumber(seatNumber)
                                    .status("AVAILABLE")
                                    .bookingId(id)
                                    .departureStationId(departureStationId)
                                    .arrivalStationId(arrivalStationId)
                                    .segmentIds(segmentIds)
                                    .build()
                    );
                });
            });
        }
        bookingDomainService.deleteBooking(id);
    }

    private void validateCreateBookingRequest(BookingRequest request) {
        if (request == null || request.getTicketIds() == null || request.getTicketIds().isEmpty()) {
            throw new RuntimeException("Vui long chon it nhat 1 ghe");
        }

        if (request.getTicketIds().size() > MAX_SEATS_PER_BOOKING) {
            throw new RuntimeException("Ban chi duoc phep dat toi da " + MAX_SEATS_PER_BOOKING + " ghe moi chieu");
        }
        if (request.getTicketIds().stream().anyMatch(Objects::isNull)) {
            throw new RuntimeException("Danh sach ghe chieu di khong hop le");
        }

        String tripType = resolveTripType(request);
        List<Long> returnTicketIds = returnTicketIds(request);
        if ("ROUND_TRIP".equals(tripType)) {
            if (returnTicketIds.isEmpty()) {
                throw new RuntimeException("Vui long chon ghe cho chieu ve");
            }
            if (request.getReturnTripId() == null) {
                throw new RuntimeException("Return trip id is required for round-trip booking");
            }
            if (returnTicketIds.size() > MAX_SEATS_PER_BOOKING) {
                throw new RuntimeException("Ban chi duoc phep dat toi da " + MAX_SEATS_PER_BOOKING + " ghe moi chieu");
            }
            if (returnTicketIds.stream().anyMatch(Objects::isNull)) {
                throw new RuntimeException("Danh sach ghe chieu ve khong hop le");
            }
            if (returnTicketIds.size() != request.getTicketIds().size()) {
                throw new RuntimeException("So ghe chieu ve phai bang so ghe chieu di");
            }
        } else if (!returnTicketIds.isEmpty()
                || request.getReturnTripId() != null
                || request.getReturnDepartureStationId() != null
                || request.getReturnArrivalStationId() != null) {
            throw new RuntimeException("Vui long chon tripType ROUND_TRIP khi dat ve khu hoi");
        }

        validatePassengerAssignments(request);
    }

    private void validatePassengerAssignments(BookingRequest request) {
        List<Long> requestedTicketIds = allRequestedTicketIds(request);
        List<BookingRequest.PassengerDetails> passengers = request.getPassengers() != null
                ? request.getPassengers()
                : List.of();

        if (passengers.size() != requestedTicketIds.size()) {
            throw new RuntimeException("So hanh khach phai bang so ghe da chon");
        }

        java.util.Set<Long> requestedTicketIdSet = new java.util.HashSet<>(requestedTicketIds);
        java.util.Set<Long> passengerTicketIdSet = new java.util.HashSet<>();

        for (BookingRequest.PassengerDetails passenger : passengers) {
            if (passenger == null || passenger.getTicketId() == null) {
                throw new RuntimeException("Moi hanh khach phai duoc gan voi mot ghe");
            }
            if (!requestedTicketIdSet.contains(passenger.getTicketId())) {
                throw new RuntimeException("Thong tin hanh khach khong khop voi ghe da chon");
            }
            if (!passengerTicketIdSet.add(passenger.getTicketId())) {
                throw new RuntimeException("Mot ghe chi duoc gan cho mot hanh khach");
            }
            if (blankToNull(passenger.getName()) == null) {
                throw new RuntimeException("Vui long nhap ho ten cho tat ca hanh khach");
            }
            if (blankToNull(passenger.getIdCard()) == null) {
                throw new RuntimeException("Vui long nhap CCCD cho tat ca hanh khach");
            }
            passenger.setPassengerType(passengerTypeOf(passenger));
        }

        if (!passengerTicketIdSet.containsAll(requestedTicketIdSet)) {
            throw new RuntimeException("Moi ghe da chon phai co thong tin hanh khach tuong ung");
        }

        validateChildPassengersShareCarriageWithAdult(request, passengers, requestedTicketIds);
    }

    private void validateChildPassengersShareCarriageWithAdult(BookingRequest request,
                                                               List<BookingRequest.PassengerDetails> passengers,
                                                               List<Long> requestedTicketIds) {
        if (passengers.stream().noneMatch(passenger -> "CHILD".equals(passengerTypeOf(passenger)))) {
            return;
        }

        List<Ticket> tickets = ticketDomainService.getTicketsByIds(requestedTicketIds);
        if (tickets.size() != requestedTicketIds.size()) {
            throw new RuntimeException("Mot so ghe khong ton tai hoac du lieu bi thay doi");
        }
        Map<Long, Ticket> ticketById = tickets.stream()
                .collect(Collectors.toMap(Ticket::getId, ticket -> ticket));
        validateChildPassengersShareCarriageWithAdult(request, passengers, ticketById);
    }

    private void validateChildPassengersShareCarriageWithAdult(BookingRequest request,
                                                               List<BookingRequest.PassengerDetails> passengers,
                                                               Map<Long, Ticket> ticketById) {
        Map<String, java.util.Set<String>> adultCarriagesByDirection = new HashMap<>();
        List<PassengerCarriageAssignment> childAssignments = new ArrayList<>();

        for (BookingRequest.PassengerDetails passenger : passengers) {
            Ticket ticket = ticketById.get(passenger.getTicketId());
            if (ticket == null) {
                throw new RuntimeException("Thong tin hanh khach khong khop voi ghe da chon");
            }

            String carriageNumber = normalizeCarriageNumber(ticket.getCarriageNumber());
            if (carriageNumber.isBlank()) {
                throw new RuntimeException("Khong xac dinh duoc toa cho ghe da chon");
            }

            String passengerType = passengerTypeOf(passenger);
            String direction = resolvePassengerDirection(request, passenger);
            if ("ADULT".equals(passengerType)) {
                adultCarriagesByDirection
                        .computeIfAbsent(direction, ignored -> new java.util.HashSet<>())
                        .add(carriageNumber);
            } else if ("CHILD".equals(passengerType)) {
                childAssignments.add(new PassengerCarriageAssignment(direction, carriageNumber));
            }
        }

        for (PassengerCarriageAssignment childAssignment : childAssignments) {
            if (!adultCarriagesByDirection
                    .getOrDefault(childAssignment.direction(), java.util.Set.of())
                    .contains(childAssignment.carriageNumber())) {
                throw new RuntimeException("Tre em phai ngoi cung toa voi nguoi lon");
            }
        }
    }

    private Map<Long, Ticket> ticketByIdFromBooking(Booking booking) {
        return booking.getDetails().stream()
                .filter(detail -> detail.getTicket() != null && detail.getTicket().getId() != null)
                .collect(Collectors.toMap(detail -> detail.getTicket().getId(), BookingDetail::getTicket, (left, right) -> left));
    }

    private List<BookingRequest.PassengerDetails> passengersForBookingValidation(Booking booking,
                                                                                 List<BookingRequest.PassengerDetails> requestedPassengers) {
        Map<Long, BookingRequest.PassengerDetails> requestedByTicketId = requestedPassengers.stream()
                .filter(passenger -> passenger != null && passenger.getTicketId() != null)
                .collect(Collectors.toMap(BookingRequest.PassengerDetails::getTicketId, passenger -> passenger, (left, right) -> right));

        return booking.getDetails().stream()
                .filter(detail -> detail.getTicket() != null && detail.getTicket().getId() != null)
                .map(detail -> {
                    Long ticketId = detail.getTicket().getId();
                    BookingRequest.PassengerDetails requested = requestedByTicketId.get(ticketId);
                    String requestedDirection = requested != null ? blankToNull(requested.getDirection()) : null;
                    String requestedName = requested != null ? blankToNull(requested.getName()) : null;
                    String requestedIdCard = requested != null ? blankToNull(requested.getIdCard()) : null;
                    String requestedPassengerType = requested != null ? blankToNull(requested.getPassengerType()) : null;
                    return new BookingRequest.PassengerDetails(
                            ticketId,
                            requestedDirection != null ? requestedDirection : detail.getDirection(),
                            requestedName != null ? requestedName : detail.getPassengerName(),
                            requestedIdCard != null ? requestedIdCard : detail.getPassengerIdCard(),
                            requestedPassengerType != null ? requestedPassengerType : detail.getPassengerType()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<BookingCreateRequestedEvent.PassengerDetails> toBookingCreateRequestedPassengers(BookingRequest request) {
        if (request.getPassengers() == null) {
            return List.of();
        }

        return request.getPassengers().stream()
                .map(passenger -> BookingCreateRequestedEvent.PassengerDetails.builder()
                        .ticketId(passenger.getTicketId())
                        .direction(normalizeDirection(passenger.getDirection()))
                        .name(passenger.getName())
                        .idCard(passenger.getIdCard())
                        .passengerType(passengerTypeOf(passenger))
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveTripType(BookingRequest request) {
        String tripType = request != null ? blankToNull(request.getTripType()) : null;
        if (tripType == null) {
            return hasReturnLeg(request) ? "ROUND_TRIP" : "ONE_WAY";
        }

        String normalized = tripType.trim().toUpperCase();
        if ("ROUNDTRIP".equals(normalized)) {
            normalized = "ROUND_TRIP";
        }
        if (!"ONE_WAY".equals(normalized) && !"ROUND_TRIP".equals(normalized)) {
            throw new RuntimeException("tripType must be ONE_WAY or ROUND_TRIP");
        }
        return normalized;
    }

    private boolean hasReturnLeg(BookingRequest request) {
        if (request == null) {
            return false;
        }
        return (request.getReturnTicketIds() != null && !request.getReturnTicketIds().isEmpty())
                || request.getReturnTripId() != null
                || request.getReturnDepartureStationId() != null
                || request.getReturnArrivalStationId() != null;
    }

    private String normalizeDirection(String direction) {
        String normalized = blankToNull(direction);
        if (normalized == null) {
            return "OUTBOUND";
        }

        normalized = normalized.trim().toUpperCase();
        if ("BACK".equals(normalized) || "INBOUND".equals(normalized) || "RETURN_TRIP".equals(normalized)) {
            return "RETURN";
        }
        if (!"OUTBOUND".equals(normalized) && !"RETURN".equals(normalized)) {
            throw new RuntimeException("direction must be OUTBOUND or RETURN");
        }
        return normalized;
    }

    private String passengerTypeOf(BookingRequest.PassengerDetails passenger) {
        String passengerType = passenger != null ? blankToNull(passenger.getPassengerType()) : null;
        return passengerType == null ? "ADULT" : passengerType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCarriageNumber(String carriageNumber) {
        String normalized = blankToNull(carriageNumber);
        return normalized == null ? "" : normalized.trim().toUpperCase(Locale.ROOT);
    }

    private String resolvePassengerDirection(BookingRequest request, BookingRequest.PassengerDetails passenger) {
        if (passenger != null && blankToNull(passenger.getDirection()) != null) {
            return normalizeDirection(passenger.getDirection());
        }

        Long ticketId = passenger != null ? passenger.getTicketId() : null;
        if (ticketId != null && returnTicketIds(request).contains(ticketId)
                && (request.getTicketIds() == null || !request.getTicketIds().contains(ticketId))) {
            return "RETURN";
        }
        return "OUTBOUND";
    }

    private List<Long> returnTicketIds(BookingRequest request) {
        return request != null && request.getReturnTicketIds() != null
                ? request.getReturnTicketIds()
                : List.of();
    }

    private List<Long> allRequestedTicketIds(BookingRequest request) {
        List<Long> result = new ArrayList<>();
        if (request != null && request.getTicketIds() != null) {
            result.addAll(request.getTicketIds());
        }
        result.addAll(returnTicketIds(request));
        return result;
    }

    private List<BookingLegRequest> resolveBookingLegRequests(BookingRequest request) {
        List<BookingLegRequest> legs = new ArrayList<>();
        legs.add(new BookingLegRequest(
                "OUTBOUND",
                request.getTripId(),
                request.getDepartureStationId(),
                request.getArrivalStationId(),
                request.getTicketIds()
        ));

        if ("ROUND_TRIP".equals(resolveTripType(request))) {
            legs.add(new BookingLegRequest(
                    "RETURN",
                    request.getReturnTripId(),
                    request.getReturnDepartureStationId(),
                    request.getReturnArrivalStationId(),
                    request.getReturnTicketIds()
            ));
        }
        return legs;
    }

    private List<PreparedBookingLeg> prepareBookingLegs(List<BookingLegRequest> legRequests, Map<Long, Ticket> ticketById, BookingRequest request) {
        List<PreparedBookingLeg> preparedLegs = new ArrayList<>();
        for (BookingLegRequest legRequest : legRequests) {
            List<Ticket> legTickets = legRequest.ticketIds().stream()
                    .map(ticketId -> {
                        Ticket ticket = ticketById.get(ticketId);
                        if (ticket == null) {
                            throw new RuntimeException("Mot so ghe khong ton tai hoac du lieu bi thay doi");
                        }
                        return ticket;
                    })
                    .collect(Collectors.toList());

            Long tripId = resolveBookingTripId(legRequest.tripId(), legTickets, legRequest.direction());
            RouteSelection routeSelection = resolveRouteSelection(
                    tripId,
                    legRequest.departureStationId(),
                    legRequest.arrivalStationId()
            );
            Map<Long, BigDecimal> segmentPricesByTicketId = resolveSegmentPricesByTicket(legTickets, routeSelection, request, legRequest.direction());
            preparedLegs.add(new PreparedBookingLeg(
                    legRequest.direction(),
                    tripId,
                    routeSelection,
                    legTickets,
                    segmentPricesByTicketId
            ));
        }
        return preparedLegs;
    }

    private BookingRequest.PassengerDetails findPassengerDetails(BookingRequest request, Long ticketId, String direction) {
        if (request == null || request.getPassengers() == null) {
            return null;
        }
        String normalizedDirection = normalizeDirection(direction);
        return request.getPassengers().stream()
                .filter(passenger -> Objects.equals(passenger.getTicketId(), ticketId))
                .filter(passenger -> normalizeDirection(passenger.getDirection()).equals(normalizedDirection))
                .findFirst()
                .or(() -> request.getPassengers().stream()
                        .filter(passenger -> Objects.equals(passenger.getTicketId(), ticketId))
                        .findFirst())
                .orElse(null);
    }

    private Long resolveBookingTripId(BookingRequest request, List<Ticket> tickets) {
        return resolveBookingTripId(request.getTripId(), tickets, "OUTBOUND");
    }

    private Long resolveBookingTripId(Long requestedTripId, List<Ticket> tickets, String direction) {
        Long tripId = requestedTripId;
        for (Ticket ticket : tickets) {
            Long ticketTripId = ticket.getTripId();
            if (ticketTripId == null) {
                continue;
            }
            if (tripId == null) {
                tripId = ticketTripId;
            } else if (!tripId.equals(ticketTripId)) {
                throw new RuntimeException("Selected seats do not belong to the same " + normalizeDirection(direction).toLowerCase() + " trip");
            }
        }
        if (tripId == null) {
            throw new RuntimeException("Trip id is required for " + normalizeDirection(direction).toLowerCase() + " booking");
        }
        return tripId;
    }

    private RouteSelection resolveRouteSelection(Long tripId, BookingRequest request) {
        return resolveRouteSelection(tripId, request.getDepartureStationId(), request.getArrivalStationId());
    }

    private RouteSelection resolveRouteSelection(Long tripId, Long departureStationId, Long arrivalStationId) {
        List<TripStop> stops = tripScheduleRepository.findStopsByTripId(tripId).stream()
                .sorted(Comparator.comparing(TripStop::getStopOrder))
                .collect(Collectors.toList());
        if (stops.size() < 2) {
            throw new RuntimeException("Trip itinerary is not configured");
        }

        Long resolvedDepartureStationId = departureStationId != null
                ? departureStationId
                : stationIdOf(stops.get(0));
        Long resolvedArrivalStationId = arrivalStationId != null
                ? arrivalStationId
                : stationIdOf(stops.get(stops.size() - 1));

        TripStop departureStop = findStopByStationId(stops, resolvedDepartureStationId, "Departure station is not in this trip");
        TripStop arrivalStop = findStopByStationId(stops, resolvedArrivalStationId, "Arrival station is not in this trip");
        if (departureStop.getStopOrder() >= arrivalStop.getStopOrder()) {
            throw new RuntimeException("Arrival station must be after departure station in trip schedule");
        }

        List<Long> segmentIds = tripScheduleRepository.findSegmentsByTripId(tripId).stream()
                .filter(segment -> segment.getSegmentOrder() >= departureStop.getStopOrder())
                .filter(segment -> segment.getSegmentOrder() < arrivalStop.getStopOrder())
                .sorted(Comparator.comparing(TripSegment::getSegmentOrder))
                .map(TripSegment::getId)
                .collect(Collectors.toList());
        if (segmentIds.isEmpty()) {
            throw new RuntimeException("No segment found for selected route");
        }

        String segmentIdsCsv = segmentIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return new RouteSelection(tripId, resolvedDepartureStationId, resolvedArrivalStationId, segmentIds, segmentIdsCsv);
    }

    private Map<Long, BigDecimal> resolveSegmentPricesByTicket(List<Ticket> tickets,
                                                               RouteSelection routeSelection,
                                                               BookingRequest request,
                                                               String direction) {
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Ticket ticket : tickets) {
            Long carriageTypeId = ticket.getCarriageTypeId();
            if (carriageTypeId == null) {
                throw new RuntimeException("Cannot determine carriage type for seat " + ticket.getSeatNumber());
            }
            BookingRequest.PassengerDetails passenger = findPassengerDetails(request, ticket.getId(), direction);
            String passengerType = passengerTypeOf(passenger);

            List<TripSegmentPrice> prices = tripScheduleRepository.findPricesBySegmentIds(
                    routeSelection.segmentIds(),
                    carriageTypeId,
                    "ADULT"
            );
            boolean usesAdultBase = true;
            if (prices.size() != routeSelection.segmentIds().size() && !"ADULT".equals(passengerType)) {
                prices = tripScheduleRepository.findPricesBySegmentIds(
                        routeSelection.segmentIds(),
                        carriageTypeId,
                        passengerType
                );
                usesAdultBase = false;
            }
            if (prices.size() != routeSelection.segmentIds().size()) {
                throw new RuntimeException("Fare table is not configured for all selected segments");
            }

            final boolean shouldApplyPassengerFareRule = usesAdultBase;
            BigDecimal price = prices.stream()
                    .map(TripSegmentPrice::getPrice)
                    .filter(Objects::nonNull)
                    .map(segmentPrice -> shouldApplyPassengerFareRule ? applyPassengerFareRule(segmentPrice, passengerType) : segmentPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(ticket.getId(), price);
        }
        return result;
    }

    private BigDecimal applyPassengerFareRule(BigDecimal basePrice, String passengerType) {
        BigDecimal price = basePrice == null ? BigDecimal.ZERO : basePrice;
        if ("ADULT".equals(passengerTypeOfType(passengerType))) {
            return price;
        }
        BigDecimal multiplier = passengerFareRuleDomainService.getActiveRule(passengerType)
                .map(PassengerFareRule::getFareMultiplier)
                .filter(Objects::nonNull)
                .orElse(BigDecimal.ONE);
        return price.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
    }

    private String passengerTypeOfType(String passengerType) {
        return passengerType == null || passengerType.isBlank()
                ? "ADULT"
                : passengerType.trim().toUpperCase(Locale.ROOT);
    }

    private void holdSegmentInventory(Booking booking, List<PreparedBookingLeg> preparedLegs, LocalDateTime holdExpiredAt) {
        if (booking.getDetails() == null) {
            return;
        }
        Map<String, RouteSelection> routeByDirection = preparedLegs.stream()
                .collect(Collectors.toMap(PreparedBookingLeg::direction, PreparedBookingLeg::routeSelection));
        for (BookingDetail detail : booking.getDetails()) {
            Ticket ticket = detail.getTicket();
            RouteSelection routeSelection = routeByDirection.get(normalizeDirection(detail.getDirection()));
            if (routeSelection == null) {
                throw new RuntimeException("Cannot determine route direction for booking detail");
            }
            if (detail.getId() == null || ticket == null || ticket.getSeatId() == null) {
                throw new RuntimeException("Cannot hold selected route segments for booking detail");
            }
            tripScheduleRepository.holdSeatSegments(
                    routeSelection.tripId(),
                    ticket.getSeatId(),
                    routeSelection.segmentIds(),
                    detail.getId(),
                    holdExpiredAt
            );
        }
    }

    private void holdSegmentInventory(Booking booking, RouteSelection routeSelection, LocalDateTime holdExpiredAt) {
        if (booking.getDetails() == null) {
            return;
        }
        for (BookingDetail detail : booking.getDetails()) {
            Ticket ticket = detail.getTicket();
            if (detail.getId() == null || ticket == null || ticket.getSeatId() == null) {
                throw new RuntimeException("Cannot hold selected route segments for booking detail");
            }
            tripScheduleRepository.holdSeatSegments(
                    routeSelection.tripId(),
                    ticket.getSeatId(),
                    routeSelection.segmentIds(),
                    detail.getId(),
                    holdExpiredAt
            );
        }
    }

    private void updateSegmentInventoryForBookingDetail(BookingDetail detail, String status, LocalDateTime holdExpiredAt) {
        if (detail == null || detail.getId() == null) {
            return;
        }
        tripScheduleRepository.updateSeatSegmentsForBookingDetail(detail.getId(), status, holdExpiredAt);
    }

    private TripStop findStopByStationId(List<TripStop> stops, Long stationId, String notFoundMessage) {
        return stops.stream()
                .filter(stop -> Objects.equals(stationIdOf(stop), stationId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(notFoundMessage));
    }

    private Long stationIdOf(TripStop stop) {
        return stop != null && stop.getStation() != null ? stop.getStation().getId() : null;
    }

    private void clearStaleSeatHold(String redisKey, Ticket ticket) {
        if (!"AVAILABLE".equalsIgnoreCase(ticket.getStatus()) || !Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            return;
        }

        Long ttlSeconds = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        redisTemplate.delete(redisKey);
        System.out.println(">>> [STALE HOLD] Removed Redis hold key " + redisKey
                + " because ticket " + ticket.getId()
                + " is AVAILABLE in DB. ttlSeconds=" + ttlSeconds);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            runSideEffect(action);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runSideEffect(action);
            }
        });
    }

    private void afterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    runSideEffect(action);
                }
            }
        });
    }

    private void afterRollbackOrNow(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            runSideEffect(action);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    runSideEffect(action);
                }
            }
        });
    }

    private void runSideEffect(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            System.err.println(">>> [SIDE_EFFECT_SKIPPED] " + ex.getMessage());
        }
    }

    private void pushBookingRealtimeAfterCommit(Long userId, BookingResponse response) {
        if (userId == null || response == null) {
            return;
        }

        afterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/users/" + userId + "/bookings", response);
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/bookings", response);
            System.out.println(">>> [BOOKING WS PUSHED] userId=" + userId
                    + " requestId=" + response.getRequestId()
                    + " bookingId=" + response.getBookingId());
        });
    }

    private void enqueueOutboxEvent(String topic,
                                    String key,
                                    String eventType,
                                    String aggregateType,
                                    Long aggregateId,
                                    Object payload,
                                    String logName) {
        outboxEventGateway.enqueue(
                topic,
                key,
                eventType,
                aggregateType,
                aggregateId != null ? aggregateId.toString() : null,
                payload
        );
        System.out.println(">>> [OUTBOX ENQUEUED] " + logName + " topic=" + topic + " key=" + key);
    }

    private void evictTripReadCaches(Long tripId) {
        redisTemplate.delete(TripCacheKeys.TRIPS_ALL);
        tripJsonCacheService.evictByPrefix(TripCacheKeys.HTTP_TRIPS_PREFIX);
        if (tripId != null) {
            redisTemplate.delete(TripCacheKeys.trip(tripId));
            tripJsonCacheService.evict(TripCacheKeys.httpTrip(tripId));
        }
    }

    private void evictTripReadCachesForTickets(List<Ticket> tickets, Long fallbackTripId) {
        evictTripReadCaches(null);
        if (fallbackTripId != null) {
            evictTripReadCaches(fallbackTripId);
        }
        if (tickets == null) {
            return;
        }
        tickets.stream()
                .map(ticket -> ticket.getTripId() != null ? ticket.getTripId() : fallbackTripId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::evictTripReadCaches);
    }

    private void compensateCreateBookingFailure(List<Ticket> heldTickets,
                                                Long fallbackTripId,
                                                Long bookingId,
                                                Long userId,
                                                Exception cause) {
        if (heldTickets == null || heldTickets.isEmpty()) {
            publishBookingSagaEventAfterRollback(bookingId, userId, fallbackTripId, List.of(), "CREATE_BOOKING", "FAILED",
                    safeMessage(cause));
            return;
        }

        heldTickets.forEach(ticket -> {
            Long tripId = ticket.getTripId() != null ? ticket.getTripId() : fallbackTripId;
            if (tripId == null || ticket.getId() == null) {
                return;
            }
            String redisKey = SEAT_HOLD_KEY_PREFIX + tripId + ":" + ticket.getId();
            redisTemplate.delete(redisKey);
            evictTripReadCaches(tripId);
            runSideEffect(() -> messagingTemplate.convertAndSend(
                    "/topic/trips/" + tripId + "/seats",
                    SeatStatusEvent.builder()
                            .tripId(tripId)
                            .ticketId(ticket.getId())
                            .seatNumber(ticket.getSeatNumber())
                            .status("AVAILABLE")
                            .bookingId(bookingId)
                            .build()
            ));
        });

        publishBookingSagaEventAfterRollback(
                bookingId,
                userId,
                firstTripId(heldTickets, fallbackTripId),
                heldTickets.stream().map(Ticket::getId).filter(Objects::nonNull).collect(Collectors.toList()),
                "CREATE_BOOKING",
                "COMPENSATED",
                safeMessage(cause)
        );
    }

    private void cleanupSeatHoldsAfterRollback(List<Ticket> heldTickets, Long fallbackTripId) {
        if (heldTickets == null || heldTickets.isEmpty()) {
            return;
        }

        heldTickets.forEach(ticket -> {
            Long tripId = ticket.getTripId() != null ? ticket.getTripId() : fallbackTripId;
            if (tripId == null || ticket.getId() == null) {
                return;
            }
            redisTemplate.delete(SEAT_HOLD_KEY_PREFIX + tripId + ":" + ticket.getId());
            evictTripReadCaches(tripId);
        });
    }

    private Long firstTripId(List<Ticket> tickets, Long fallbackTripId) {
        if (tickets == null) {
            return fallbackTripId;
        }
        return tickets.stream()
                .map(ticket -> ticket.getTripId() != null ? ticket.getTripId() : fallbackTripId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(fallbackTripId);
    }

    private void publishBookingSagaEvent(Long bookingId,
                                         Long userId,
                                         Long tripId,
                                          List<Long> ticketIds,
                                          String step,
                                          String status,
                                          String reason) {
        BookingSagaEvent event = buildBookingSagaEvent(bookingId, userId, tripId, ticketIds, step, status, reason);
        publishBookingSagaRealtime(event);
        enqueueBookingSagaOutbox(event, step, status);
    }

    private void publishBookingSagaEventAfterRollback(Long bookingId,
                                                      Long userId,
                                                      Long tripId,
                                                      List<Long> ticketIds,
                                                      String step,
                                                      String status,
                                                      String reason) {
        BookingSagaEvent event = buildBookingSagaEvent(bookingId, userId, tripId, ticketIds, step, status, reason);
        publishBookingSagaRealtime(event);
        afterRollbackOrNow(() -> enqueueBookingSagaOutbox(event, step, status));
    }

    private BookingSagaEvent buildBookingSagaEvent(Long bookingId,
                                                   Long userId,
                                                   Long tripId,
                                                   List<Long> ticketIds,
                                                   String step,
                                                   String status,
                                                   String reason) {
        BookingSagaEvent event = BookingSagaEvent.builder()
                .bookingId(bookingId)
                .userId(userId)
                .tripId(tripId)
                .ticketIds(ticketIds)
                .step(step)
                .status(status)
                .reason(reason)
                .occurredAt(LocalDateTime.now())
                .build();
        return event;
    }

    private void publishBookingSagaRealtime(BookingSagaEvent event) {
        runSideEffect(() -> messagingTemplate.convertAndSend("/topic/bookings/saga", event));
    }

    private void enqueueBookingSagaOutbox(BookingSagaEvent event, String step, String status) {
        String key = event.getBookingId() != null ? event.getBookingId().toString() : step;
        enqueueOutboxEvent(
                BOOKING_SAGA_EVENTS_TOPIC,
                key,
                "BOOKING_SAGA_" + step + "_" + status,
                "BOOKING",
                event.getBookingId(),
                event,
                "BOOKING_SAGA_" + step + "_" + status
        );
    }

    private String safeMessage(Exception cause) {
        return cause == null || cause.getMessage() == null ? "unknown" : cause.getMessage();
    }

    private List<String> toSeatNumbers(Booking booking) {
        if (booking.getDetails() == null) {
            return List.of();
        }
        return booking.getDetails().stream()
                .map(BookingDetail::getTicket)
                .filter(ticket -> ticket != null)
                .map(Ticket::getSeatNumber)
                .collect(Collectors.toList());
    }

    private List<Long> toTicketIds(Booking booking) {
        if (booking.getDetails() == null) {
            return List.of();
        }
        return booking.getDetails().stream()
                .map(BookingDetail::getTicket)
                .filter(ticket -> ticket != null)
                .map(Ticket::getId)
                .collect(Collectors.toList());
    }

    private BookingHistoryResponse toHistoryResponse(Booking booking, Map<Long, Trip> tripCache) {
        Ticket firstTicket = firstTicket(booking);
        Long tripId = firstTicket != null ? firstTicket.getTripId() : null;
        Trip trip = null;
        if (tripId != null) {
            trip = tripCache.computeIfAbsent(tripId, tripDomainService::getTripByIdFetched);
        }
        Payment payment = paymentDomainService.findLatestByBookingIdOrNull(booking.getId());
        BookingRouteView route = resolveBookingRoute(booking, trip);

        return BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .requestId(booking.getAsyncRequestId())
                .orderNumber(booking.getOrderNumber())
                .storageMonth(booking.getStorageMonth())
                .tripType(booking.getTripType())
                .status(booking.getStatus())
                .originalPrice(originalPriceOf(booking))
                .promoCode(booking.getPromoCode())
                .discountAmount(discountAmountOf(booking))
                .totalPrice(booking.getTotalPrice())
                .contactName(booking.getContactName())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .contactIdCard(decryptSensitiveTextOrNull(booking.getContactIdCard()))
                .expiredAt(booking.getExpiredAt())
                .createdAt(booking.getCreatedAt())
                .tripId(tripId)
                .trainCode(trip != null && trip.getTrain() != null ? trip.getTrain().getCode() : null)
                .departureStationId(route.departureStationId())
                .departureStation(route.departureStation())
                .arrivalStationId(route.arrivalStationId())
                .arrivalStation(route.arrivalStation())
                .departureTime(route.departureTime())
                .arrivalTime(route.arrivalTime())
                .duration(route.duration())
                .seatNumbers(toSeatNumbers(booking))
                .ticketIds(toTicketIds(booking))
                .passengerCount(booking.getDetails() != null ? booking.getDetails().size() : 0)
                .paymentMethod(payment != null ? payment.getMethod() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentTransactionId(payment != null ? payment.getTransactionId() : null)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .build();
    }

    private Ticket firstTicket(Booking booking) {
        if (booking.getDetails() == null) {
            return null;
        }
        return booking.getDetails().stream()
                .map(BookingDetail::getTicket)
                .filter(ticket -> ticket != null)
                .findFirst()
                .orElse(null);
    }

    private BookingDetail firstDetail(Booking booking) {
        if (booking.getDetails() == null) {
            return null;
        }
        return booking.getDetails().stream()
                .filter(detail -> detail != null)
                .findFirst()
                .orElse(null);
    }

    private BookingRouteView resolveBookingRoute(Booking booking, Trip trip) {
        BookingDetail detail = firstDetail(booking);
        Ticket firstTicket = firstTicket(booking);
        Long tripId = firstTicket != null ? firstTicket.getTripId() : (trip != null ? trip.getId() : null);
        Long departureStationId = detail != null ? detail.getDepartureStationId() : null;
        Long arrivalStationId = detail != null ? detail.getArrivalStationId() : null;

        if (tripId != null && departureStationId != null && arrivalStationId != null) {
            List<TripStop> stops = tripScheduleRepository.findStopsByTripId(tripId);
            TripStop departureStop = findStopOrNull(stops, departureStationId);
            TripStop arrivalStop = findStopOrNull(stops, arrivalStationId);
            LocalDateTime departureTime = resolveDepartureTime(departureStop, trip);
            LocalDateTime arrivalTime = resolveArrivalTime(arrivalStop, trip);
            return new BookingRouteView(
                    tripId,
                    departureStationId,
                    stationNameOf(departureStop, trip != null ? trip.getDepartureStation() : null),
                    arrivalStationId,
                    stationNameOf(arrivalStop, trip != null ? trip.getArrivalStation() : null),
                    departureTime,
                    arrivalTime,
                    resolveDuration(departureTime, arrivalTime, trip)
            );
        }

        return new BookingRouteView(
                tripId,
                trip != null && trip.getDepartureStation() != null ? trip.getDepartureStation().getId() : null,
                trip != null && trip.getDepartureStation() != null ? trip.getDepartureStation().getName() : null,
                trip != null && trip.getArrivalStation() != null ? trip.getArrivalStation().getId() : null,
                trip != null && trip.getArrivalStation() != null ? trip.getArrivalStation().getName() : null,
                trip != null ? trip.getDepartureTime() : null,
                trip != null ? trip.getArrivalTime() : null,
                trip != null ? trip.getDuration() : null
        );
    }

    private TripStop findStopOrNull(List<TripStop> stops, Long stationId) {
        if (stops == null) {
            return null;
        }
        return stops.stream()
                .filter(stop -> Objects.equals(stationIdOf(stop), stationId))
                .findFirst()
                .orElse(null);
    }

    private String stationNameOf(TripStop stop, Station fallback) {
        if (stop != null && stop.getStation() != null) {
            return stop.getStation().getName();
        }
        return fallback != null ? fallback.getName() : null;
    }

    private LocalDateTime resolveDepartureTime(TripStop stop, Trip trip) {
        if (stop != null) {
            if (stop.getScheduledDepartureTime() != null) {
                return stop.getScheduledDepartureTime();
            }
            if (stop.getScheduledArrivalTime() != null) {
                return stop.getScheduledArrivalTime();
            }
        }
        return trip != null ? trip.getDepartureTime() : null;
    }

    private LocalDateTime resolveArrivalTime(TripStop stop, Trip trip) {
        if (stop != null) {
            if (stop.getScheduledArrivalTime() != null) {
                return stop.getScheduledArrivalTime();
            }
            if (stop.getScheduledDepartureTime() != null) {
                return stop.getScheduledDepartureTime();
            }
        }
        return trip != null ? trip.getArrivalTime() : null;
    }

    private Integer resolveDuration(LocalDateTime departureTime, LocalDateTime arrivalTime, Trip trip) {
        if (departureTime != null && arrivalTime != null) {
            return Math.toIntExact(Duration.between(departureTime, arrivalTime).toMinutes());
        }
        return trip != null ? trip.getDuration() : null;
    }

    private BookingDetailResponse toDetailResponse(Booking booking) {
        Ticket firstTicket = firstTicket(booking);
        Trip trip = firstTicket != null && firstTicket.getTripId() != null
                ? tripDomainService.getTripByIdFetched(firstTicket.getTripId())
                : null;
        Payment payment = paymentDomainService.findLatestByBookingIdOrNull(booking.getId());
        BookingRouteView route = resolveBookingRoute(booking, trip);

        return BookingDetailResponse.builder()
                .bookingId(booking.getId())
                .requestId(booking.getAsyncRequestId())
                .orderNumber(booking.getOrderNumber())
                .storageMonth(booking.getStorageMonth())
                .tripType(booking.getTripType())
                .status(booking.getStatus())
                .originalPrice(originalPriceOf(booking))
                .promoCode(booking.getPromoCode())
                .discountAmount(discountAmountOf(booking))
                .totalPrice(booking.getTotalPrice())
                .contactName(booking.getContactName())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .contactIdCard(decryptSensitiveTextOrNull(booking.getContactIdCard()))
                .expiredAt(booking.getExpiredAt())
                .createdAt(booking.getCreatedAt())
                .tripId(firstTicket != null ? firstTicket.getTripId() : null)
                .trainCode(trip != null && trip.getTrain() != null ? trip.getTrain().getCode() : null)
                .departureStationId(route.departureStationId())
                .departureStation(route.departureStation())
                .arrivalStationId(route.arrivalStationId())
                .arrivalStation(route.arrivalStation())
                .departureTime(route.departureTime())
                .arrivalTime(route.arrivalTime())
                .duration(route.duration())
                .seatNumbers(toSeatNumbers(booking))
                .ticketIds(toTicketIds(booking))
                .passengerCount(booking.getDetails() != null ? booking.getDetails().size() : 0)
                .paymentMethod(payment != null ? payment.getMethod() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentTransactionId(payment != null ? payment.getTransactionId() : null)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .details(toTicketDetails(booking, route))
                .build();
    }

    private void ensureInvoiceCanBeIssued(BookingDetailResponse booking) {
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new BusinessException("BOOKING_NOT_CONFIRMED");
        }
        if (booking.getPaymentStatus() != null && !"SUCCESS".equalsIgnoreCase(booking.getPaymentStatus())) {
            throw new BusinessException("PAYMENT_NOT_SUCCESS");
        }
    }

    private List<BookingDetailResponse.TicketDetail> toTicketDetails(Booking booking, BookingRouteView route) {
        if (booking.getDetails() == null) {
            return List.of();
        }
        return booking.getDetails().stream()
                .map(detail -> {
                    Ticket ticket = detail.getTicket();
                    BookingRouteView detailRoute = resolveTicketDetailRoute(detail, route);
                    return BookingDetailResponse.TicketDetail.builder()
                            .bookingDetailId(detail.getId())
                            .ticketId(ticket != null ? ticket.getId() : null)
                            .direction(normalizeDirection(detail.getDirection()))
                            .ticketStatus(ticket != null ? ticket.getStatus() : null)
                            .seatNumber(ticket != null ? ticket.getSeatNumber() : null)
                            .carriageNumber(ticket != null ? ticket.getCarriageNumber() : null)
                            .carriageTypeName(ticket != null ? ticket.getCarriageTypeName() : null)
                            .departureStationId(detail.getDepartureStationId() != null ? detail.getDepartureStationId() : detailRoute.departureStationId())
                            .departureStation(detailRoute.departureStation())
                            .arrivalStationId(detail.getArrivalStationId() != null ? detail.getArrivalStationId() : detailRoute.arrivalStationId())
                            .arrivalStation(detailRoute.arrivalStation())
                            .segmentIds(detail.getSegmentIds())
                            .price(detail.getSegmentPrice() != null ? detail.getSegmentPrice() : (ticket != null ? ticket.getPrice() : null))
                            .passengerName(detail.getPassengerName())
                            .passengerIdCard(decryptSensitiveText(detail.getPassengerIdCard()))
                            .passengerType(detail.getPassengerType())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BookingRouteView resolveTicketDetailRoute(BookingDetail detail, BookingRouteView fallback) {
        Ticket ticket = detail.getTicket();
        Long tripId = ticket != null && ticket.getTripId() != null ? ticket.getTripId() : fallback.tripId();
        Long departureStationId = detail.getDepartureStationId() != null
                ? detail.getDepartureStationId()
                : fallback.departureStationId();
        Long arrivalStationId = detail.getArrivalStationId() != null
                ? detail.getArrivalStationId()
                : fallback.arrivalStationId();

        if (tripId == null || departureStationId == null || arrivalStationId == null) {
            return fallback;
        }

        List<TripStop> stops = tripScheduleRepository.findStopsByTripId(tripId);
        TripStop departureStop = findStopOrNull(stops, departureStationId);
        TripStop arrivalStop = findStopOrNull(stops, arrivalStationId);
        LocalDateTime departureTime = resolveDepartureTime(departureStop, null);
        LocalDateTime arrivalTime = resolveArrivalTime(arrivalStop, null);

        return new BookingRouteView(
                tripId,
                departureStationId,
                stationNameOf(departureStop, Objects.equals(departureStationId, fallback.departureStationId()) ? fallback.departureStation() : null),
                arrivalStationId,
                stationNameOf(arrivalStop, Objects.equals(arrivalStationId, fallback.arrivalStationId()) ? fallback.arrivalStation() : null),
                departureTime != null ? departureTime : fallback.departureTime(),
                arrivalTime != null ? arrivalTime : fallback.arrivalTime(),
                resolveDuration(departureTime, arrivalTime, null)
        );
    }

    private String stationNameOf(TripStop stop, String fallback) {
        if (stop != null && stop.getStation() != null) {
            return stop.getStation().getName();
        }
        return fallback;
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .requestId(booking.getAsyncRequestId())
                .orderNumber(booking.getOrderNumber())
                .storageMonth(booking.getStorageMonth())
                .tripType(booking.getTripType())
                .status(booking.getStatus())
                .originalPrice(originalPriceOf(booking))
                .promoCode(booking.getPromoCode())
                .discountAmount(discountAmountOf(booking))
                .totalPrice(booking.getTotalPrice())
                .contactName(booking.getContactName())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .contactIdCard(decryptSensitiveTextOrNull(booking.getContactIdCard()))
                .expiredAt(booking.getExpiredAt())
                .seatNumbers(toSeatNumbers(booking))
                .ticketIds(toTicketIds(booking))
                .build();
    }

    private PromotionDiscount resolvePromotionDiscount(String rawPromoCode, BigDecimal originalPrice) {
        String promoCode = normalizePromoCode(rawPromoCode);
        if (promoCode == null) {
            return new PromotionDiscount(null, BigDecimal.ZERO);
        }

        Promotion promotion = promotionDomainService.getActivePromotionByCode(promoCode, LocalDate.now());
        if (promotion.getMinOrderAmount() != null && originalPrice.compareTo(promotion.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Don hang chua dat gia tri toi thieu de dung ma " + promoCode);
        }
        if (promotion.getUsageLimit() != null
                && promotion.getUsedCount() != null
                && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new RuntimeException("Ma khuyen mai " + promoCode + " da het luot su dung");
        }

        BigDecimal discountAmount = calculatePromotionDiscount(promotion, originalPrice);
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Ma khuyen mai " + promoCode + " khong tao gia tri giam hop le");
        }
        return new PromotionDiscount(promotion.getCode(), discountAmount);
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

    private void incrementPromotionUsage(String promoCode) {
        try {
            Promotion promotion = promotionDomainService.getPromotionByCode(promoCode);
            promotion.setUsedCount((promotion.getUsedCount() == null ? 0 : promotion.getUsedCount()) + 1);
            promotionDomainService.updatePromotion(promotion.getId(), promotion);
        } catch (RuntimeException ex) {
            System.err.println(">>> [PROMOTION USAGE] Cannot increment usage for " + promoCode + ": " + ex.getMessage());
        }
    }

    private String normalizePromoCode(String rawPromoCode) {
        if (rawPromoCode == null || rawPromoCode.isBlank()) {
            return null;
        }
        return rawPromoCode.trim().toUpperCase();
    }

    private BigDecimal originalPriceOf(Booking booking) {
        return booking.getOriginalPrice() != null ? booking.getOriginalPrice() : booking.getTotalPrice();
    }

    private BigDecimal discountAmountOf(Booking booking) {
        return booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
    }

    private String encryptSensitiveText(String value) {
        return sensitiveDataCryptoService.encrypt(value);
    }

    private String decryptSensitiveText(String value) {
        return sensitiveDataCryptoService.decrypt(value);
    }

    private String encryptSensitiveTextOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : encryptSensitiveText(normalized);
    }

    private String decryptSensitiveTextOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : decryptSensitiveText(normalized);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record BookingLegRequest(String direction,
                                     Long tripId,
                                     Long departureStationId,
                                     Long arrivalStationId,
                                     List<Long> ticketIds) {
    }

    private record PreparedBookingLeg(String direction,
                                      Long tripId,
                                      RouteSelection routeSelection,
                                      List<Ticket> tickets,
                                      Map<Long, BigDecimal> segmentPricesByTicketId) {
    }

    private record RouteSelection(Long tripId,
                                  Long departureStationId,
                                  Long arrivalStationId,
                                  List<Long> segmentIds,
                                  String segmentIdsCsv) {
    }

    private record PassengerCarriageAssignment(String direction,
                                               String carriageNumber) {
    }

    private record BookingRouteView(Long tripId,
                                    Long departureStationId,
                                    String departureStation,
                                    Long arrivalStationId,
                                    String arrivalStation,
                                    LocalDateTime departureTime,
                                    LocalDateTime arrivalTime,
                                    Integer duration) {
    }

    private record PromotionDiscount(String promoCode, BigDecimal discountAmount) {
    }
}
