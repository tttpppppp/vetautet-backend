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
import com.vetautet.domain.gateway.BookingNotificationGateway;
import com.vetautet.domain.model.*;
import com.vetautet.domain.service.BookingDomainService;
import com.vetautet.domain.service.PaymentDomainService;
import com.vetautet.domain.service.PromotionDomainService;
import com.vetautet.domain.service.TicketDomainService;
import com.vetautet.domain.service.TripDomainService;
import com.vetautet.domain.service.UserDomainService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private TripDomainService tripDomainService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TripJsonCacheService tripJsonCacheService;

    @Autowired
    private BookingInvoicePdfService bookingInvoicePdfService;

    @Autowired
    private BookingNotificationGateway bookingNotificationGateway;

    @Value("${vetautet.kafka.producer.enabled:false}")
    private boolean kafkaProducerEnabled;

    private static final int MAX_SEATS_PER_BOOKING = 4;
    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String PAYMENT_CONFIRMED_TOPIC = "payment-confirmed";
    private static final String BOOKING_LOCK_PREFIX = "lock:ticket:";
    private static final String SEAT_HOLD_KEY_PREFIX = "seat:"; 
    private static final long SEAT_HOLD_TTL_MINUTES = 15;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
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

        // 3. Sử dụng Redisson MultiLock để khóa các Ticket đang đặt
        // Sắp xếp ID ticket để tránh Deadlock
        List<Long> sortedIds = request.getTicketIds().stream().sorted().collect(Collectors.toList());
        RLock[] locks = sortedIds.stream()
                .map(id -> redissonClient.getLock(BOOKING_LOCK_PREFIX + id))
                .toArray(RLock[]::new);
        
        RLock multiLock = redissonClient.getMultiLock(locks);

        boolean isLocked = false;
        try {
            // Chờ tối đa 3 giây để lấy lock cho toàn bộ ghế, giữ lock 10 giây
            if (multiLock.tryLock(3, 10, TimeUnit.SECONDS)) {
                isLocked = true;
                
                // 4. Lấy danh sách Ticket từ DB (Lúc này đã có lock nên an toàn)
                List<Ticket> tickets = ticketDomainService.getTicketsByIds(request.getTicketIds());
                
                if (tickets.size() != request.getTicketIds().size()) {
                    throw new RuntimeException("Một số ghế không tồn tại hoặc dữ liệu bị thay đổi");
                }

                for (Ticket ticket : tickets) {
                    if (!"AVAILABLE".equals(ticket.getStatus())) {
                        throw new RuntimeException("Ghế " + ticket.getSeatNumber() + " vừa mới có người đặt mất rồi. Vui lại chọn ghế khác nhé!");
                    }
                    
                    if (ticket.getTripId() == null && request.getTripId() == null) {
                        throw new RuntimeException("Ghế " + ticket.getSeatNumber() + " không thuộc về chuyến tàu nào hợp lệ.");
                    }
                    Long tripId = ticket.getTripId() != null ? ticket.getTripId() : request.getTripId();

                    // Check Redis Hold
                    String redisKey = SEAT_HOLD_KEY_PREFIX + tripId + ":" + ticket.getId();
                    clearStaleSeatHold(redisKey, ticket);
                    if (redisTemplate.hasKey(redisKey)) {
                        throw new RuntimeException("Ghế " + ticket.getSeatNumber() + " đang được người khác giữ chỗ. Vui lòng chọn ghế khác!");
                    }
                }

                // 5. Cập nhật trạng thái Ticket sang HOLD (DB) và Redis
                tickets.forEach(ticket -> {
                    ticket.setStatus("HOLD");
                    ticket.setHoldExpiredAt(LocalDateTime.now().plusMinutes(SEAT_HOLD_TTL_MINUTES));
                    
                    Long currentTripId = ticket.getTripId() != null ? ticket.getTripId() : request.getTripId();

                    // Set Redis Hold
                    String redisKey = SEAT_HOLD_KEY_PREFIX + currentTripId + ":" + ticket.getId();
                    redisTemplate.opsForValue().set(redisKey, user.getId(), java.time.Duration.ofMinutes(SEAT_HOLD_TTL_MINUTES));
                    
                    // Lưu trực tiếp vào DB từng vé
                    ticketDomainService.saveTicket(ticket);
                });

                // 6. Tính tổng tiền
                BigDecimal originalPrice = tickets.stream()
                        .map(Ticket::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                PromotionDiscount promotionDiscount = resolvePromotionDiscount(request.getPromoCode(), originalPrice);
                BigDecimal totalPrice = originalPrice.subtract(promotionDiscount.discountAmount()).max(BigDecimal.ZERO);

                // 7. Tạo Booking và BookingDetail
                Booking booking = Booking.builder()
                        .user(user)
                        .originalPrice(originalPrice)
                        .promoCode(promotionDiscount.promoCode())
                        .discountAmount(promotionDiscount.discountAmount())
                        .totalPrice(totalPrice)
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
                                    .passengerName(pDetails != null ? pDetails.getName() : "Khách hàng")
                                    .passengerIdCard(pDetails != null ? pDetails.getIdCard() : "N/A")
                                    .passengerType("ADULT")
                                    .booking(booking)
                                    .build();
                        })
                        .collect(Collectors.toList());

                booking.setDetails(details);

                // 8. Lưu Booking
                Booking savedBooking = bookingDomainService.saveBooking(booking);

                // 9. Xóa cache của chuyến tàu để cập nhật trạng thái ghế mới
                Long tripId = request.getTripId();
                evictTripReadCaches(tripId);

                Long savedBookingId = savedBooking.getId();
                tickets.forEach(ticket -> {
                    Long currentTripId = ticket.getTripId() != null ? ticket.getTripId() : request.getTripId();
                    afterCommit(() -> messagingTemplate.convertAndSend(
                            "/topic/trips/" + currentTripId + "/seats",
                            SeatStatusEvent.builder()
                                    .tripId(currentTripId)
                                    .ticketId(ticket.getId())
                                    .seatNumber(ticket.getSeatNumber())
                                    .status("HOLD")
                                    .bookingId(savedBookingId)
                                    .build()
                    ));
                });

                // 10. Gửi message vào Kafka để xử lý timeout sau này nếu local đang bật Kafka.
                afterCommit(() -> publishKafkaEvent(
                        ORDER_CREATED_TOPIC,
                        savedBooking.getId().toString(),
                        savedBooking.getId(),
                        "ORDER_CREATED"
                ));
                System.out.println(">>> [BOOKING LOCKED] Thành công giữ chỗ cho booking: " + savedBooking.getId());

                return BookingResponse.builder()
                        .bookingId(savedBooking.getId())
                        .status(savedBooking.getStatus())
                        .originalPrice(originalPriceOf(savedBooking))
                        .promoCode(savedBooking.getPromoCode())
                        .discountAmount(discountAmountOf(savedBooking))
                        .totalPrice(savedBooking.getTotalPrice())
                        .expiredAt(savedBooking.getExpiredAt())
                        .seatNumbers(tickets.stream().map(Ticket::getSeatNumber).collect(Collectors.toList()))
                        .ticketIds(tickets.stream().map(Ticket::getId).collect(Collectors.toList()))
                        .build();
            } else {
                throw new RuntimeException("Hệ thống đang bận xử lý ghế bạn chọn, vui lòng thử lại sau vài giây!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Quá trình đặt vé bị gián đoạn");
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

        // 2. Cập nhật thông tin chi tiết từng hành khách
        if (request.getPassengers() != null) {
            for (BookingRequest.PassengerDetails pDetails : request.getPassengers()) {
                booking.getDetails().stream()
                        .filter(detail -> detail.getTicket().getId().equals(pDetails.getTicketId()))
                        .findFirst()
                        .ifPresent(detail -> {
                            detail.setPassengerName(pDetails.getName());
                            detail.setPassengerIdCard(pDetails.getIdCard());
                        });
            }
        }

        // 3. Lưu lại
        Booking savedBooking = bookingDomainService.saveBooking(booking);

        return BookingResponse.builder()
                .bookingId(savedBooking.getId())
                .status(savedBooking.getStatus())
                .originalPrice(originalPriceOf(savedBooking))
                .promoCode(savedBooking.getPromoCode())
                .discountAmount(discountAmountOf(savedBooking))
                .totalPrice(savedBooking.getTotalPrice())
                .expiredAt(savedBooking.getExpiredAt())
                .seatNumbers(toSeatNumbers(savedBooking))
                .ticketIds(toTicketIds(savedBooking))
                .build();
    }

    @Override
    @Transactional
    public BookingResponse confirmPayment(Long bookingId) {
        System.out.println(">>> [PAYMENT] Xử lý xác nhận thanh toán cho Booking: " + bookingId);
        // 1. Cập nhật trạng thái
        BookingResponse response = updateBookingStatus(bookingId, "CONFIRMED");
        Booking confirmedBooking = bookingDomainService.getBookingByIdFetched(bookingId);
        
        // 2. Gửi Kafka để thực hiện Notify (Email/SMS/Ticket PDF)
        afterCommit(() -> publishPaymentConfirmedNotification(bookingId, confirmedBooking));
        
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
                    ticket.setStatus("BOOKED"); // Trong DB vẫn lưu trạng thái ghế là BOOKED (Đã bán)
                    newStatus = "BOOKED";
                    ticket.setHoldExpiredAt(null);
                    ticketDomainService.saveTicket(ticket);
                } else {
                    ticket.setStatus("AVAILABLE");
                    ticket.setHoldExpiredAt(null);
                    ticketDomainService.saveTicket(ticket);
                }
                
                Long tripId = ticket.getTripId();
                Long ticketId = ticket.getId();
                String seatNumber = ticket.getSeatNumber();
                String statusEvent = newStatus;
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
    public void deleteBooking(Long id) {
        Booking booking = bookingDomainService.findBookingByIdOrNull(id);
        if (booking != null) {
            booking.getDetails().forEach(detail -> {
                Ticket ticket = detail.getTicket();
                ticket.setStatus("AVAILABLE");
                ticket.setHoldExpiredAt(null);
                ticketDomainService.saveTicket(ticket);
                
                Long tripId = ticket.getTripId();
                Long ticketId = ticket.getId();
                String seatNumber = ticket.getSeatNumber();
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
                                    .build()
                    );
                });
            });
        }
        bookingDomainService.deleteBooking(id);
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

    private void runSideEffect(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            System.err.println(">>> [SIDE_EFFECT_SKIPPED] " + ex.getMessage());
        }
    }

    private void publishKafkaEvent(String topic, String key, Object payload, String eventName) {
        if (!kafkaProducerEnabled) {
            System.out.println(">>> [KAFKA SKIPPED] " + eventName + " topic=" + topic + " key=" + key);
            return;
        }

        try {
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            System.err.println(">>> [KAFKA SEND FAILED] " + eventName + " topic=" + topic
                                    + " key=" + key + " error=" + ex.getMessage());
                        } else {
                            System.out.println(">>> [KAFKA SENT] " + eventName + " topic=" + topic + " key=" + key);
                        }
                    });
        } catch (Exception ex) {
            System.err.println(">>> [KAFKA SEND SKIPPED] " + eventName + " topic=" + topic
                    + " key=" + key + " error=" + ex.getMessage());
        }
    }

    private void publishPaymentConfirmedNotification(Long bookingId, Booking booking) {
        if (kafkaProducerEnabled) {
            publishKafkaEvent(PAYMENT_CONFIRMED_TOPIC, bookingId.toString(), bookingId, "PAYMENT_CONFIRMED");
            return;
        }

        bookingNotificationGateway.sendBookingConfirmation(booking);
        System.out.println(">>> [NOTI DIRECT] Saved and pushed booking confirmation for Booking: " + bookingId);
    }

    private void evictTripReadCaches(Long tripId) {
        redisTemplate.delete(TripCacheKeys.TRIPS_ALL);
        tripJsonCacheService.evict(TripCacheKeys.HTTP_TRIPS_ALL);
        tripJsonCacheService.evictByPrefix(TripCacheKeys.HTTP_TRIPS_SEARCH_PREFIX);
        if (tripId != null) {
            redisTemplate.delete(TripCacheKeys.trip(tripId));
            tripJsonCacheService.evict(TripCacheKeys.httpTrip(tripId));
        }
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

        return BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .originalPrice(originalPriceOf(booking))
                .promoCode(booking.getPromoCode())
                .discountAmount(discountAmountOf(booking))
                .totalPrice(booking.getTotalPrice())
                .expiredAt(booking.getExpiredAt())
                .createdAt(booking.getCreatedAt())
                .tripId(tripId)
                .trainCode(trip != null && trip.getTrain() != null ? trip.getTrain().getCode() : null)
                .departureStation(trip != null && trip.getDepartureStation() != null ? trip.getDepartureStation().getName() : null)
                .arrivalStation(trip != null && trip.getArrivalStation() != null ? trip.getArrivalStation().getName() : null)
                .departureTime(trip != null ? trip.getDepartureTime() : null)
                .arrivalTime(trip != null ? trip.getArrivalTime() : null)
                .duration(trip != null ? trip.getDuration() : null)
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

    private BookingDetailResponse toDetailResponse(Booking booking) {
        Ticket firstTicket = firstTicket(booking);
        Trip trip = firstTicket != null && firstTicket.getTripId() != null
                ? tripDomainService.getTripByIdFetched(firstTicket.getTripId())
                : null;
        Payment payment = paymentDomainService.findLatestByBookingIdOrNull(booking.getId());

        return BookingDetailResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .originalPrice(originalPriceOf(booking))
                .promoCode(booking.getPromoCode())
                .discountAmount(discountAmountOf(booking))
                .totalPrice(booking.getTotalPrice())
                .expiredAt(booking.getExpiredAt())
                .createdAt(booking.getCreatedAt())
                .tripId(firstTicket != null ? firstTicket.getTripId() : null)
                .trainCode(trip != null && trip.getTrain() != null ? trip.getTrain().getCode() : null)
                .departureStation(trip != null && trip.getDepartureStation() != null ? trip.getDepartureStation().getName() : null)
                .arrivalStation(trip != null && trip.getArrivalStation() != null ? trip.getArrivalStation().getName() : null)
                .departureTime(trip != null ? trip.getDepartureTime() : null)
                .arrivalTime(trip != null ? trip.getArrivalTime() : null)
                .duration(trip != null ? trip.getDuration() : null)
                .seatNumbers(toSeatNumbers(booking))
                .ticketIds(toTicketIds(booking))
                .passengerCount(booking.getDetails() != null ? booking.getDetails().size() : 0)
                .paymentMethod(payment != null ? payment.getMethod() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentTransactionId(payment != null ? payment.getTransactionId() : null)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .details(toTicketDetails(booking))
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

    private List<BookingDetailResponse.TicketDetail> toTicketDetails(Booking booking) {
        if (booking.getDetails() == null) {
            return List.of();
        }
        return booking.getDetails().stream()
                .map(detail -> {
                    Ticket ticket = detail.getTicket();
                    return BookingDetailResponse.TicketDetail.builder()
                            .bookingDetailId(detail.getId())
                            .ticketId(ticket != null ? ticket.getId() : null)
                            .ticketStatus(ticket != null ? ticket.getStatus() : null)
                            .seatNumber(ticket != null ? ticket.getSeatNumber() : null)
                            .carriageNumber(ticket != null ? ticket.getCarriageNumber() : null)
                            .carriageTypeName(ticket != null ? ticket.getCarriageTypeName() : null)
                            .price(ticket != null ? ticket.getPrice() : null)
                            .passengerName(detail.getPassengerName())
                            .passengerIdCard(detail.getPassengerIdCard())
                            .passengerType(detail.getPassengerType())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .originalPrice(originalPriceOf(booking))
                .promoCode(booking.getPromoCode())
                .discountAmount(discountAmountOf(booking))
                .totalPrice(booking.getTotalPrice())
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

    private record PromotionDiscount(String promoCode, BigDecimal discountAmount) {
    }
}
