package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Booking;
import com.vetautet.domain.repository.BookingRepository;
import com.vetautet.infrastructure.persistence.entity.BookingEntity;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.order.BookingMonthlyMirrorRepository;
import com.vetautet.infrastructure.persistence.order.BookingMonthlyTableRouter;
import com.vetautet.infrastructure.persistence.order.BookingOrderNumberGenerator;
import com.vetautet.infrastructure.persistence.order.BookingOrderNumberParser;
import com.vetautet.infrastructure.persistence.repository.BookingJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BookingInfrasRepositoryImpl implements BookingRepository {

    @Autowired
    private BookingJpaRepository jpaRepository;

    @Autowired
    private PersistenceMapper mapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BookingOrderNumberGenerator orderNumberGenerator;

    @Autowired
    private BookingOrderNumberParser orderNumberParser;

    @Autowired
    private BookingMonthlyTableRouter monthlyTableRouter;

    @Autowired
    private BookingMonthlyMirrorRepository monthlyMirrorRepository;

    @Override
    public Booking save(Booking booking) {
        if (booking.getCreatedAt() == null) {
            booking.setCreatedAt(LocalDateTime.now());
        }
        if (booking.getOrderNumber() == null || booking.getOrderNumber().isBlank()) {
            booking.setOrderNumber(orderNumberGenerator.generate(booking.getCreatedAt()));
        }
        if (booking.getStorageMonth() == null || booking.getStorageMonth().isBlank()) {
            booking.setStorageMonth(orderNumberParser.extractStorageMonth(booking.getOrderNumber()));
        }
        BookingEntity entity = mapper.toEntity(booking);
        // Fix for detached entity
        if (booking.getUser() != null && booking.getUser().getId() != null) {
            entity.setUser(entityManager.getReference(com.vetautet.infrastructure.persistence.entity.UserEntity.class, booking.getUser().getId()));
        }

        // MapStruct might not map the bidirectional relationship correctly if not
        // configured
        if (entity.getDetails() != null) {
            entity.getDetails().forEach(detail -> {
                detail.setBooking(entity);
                if (detail.getTicket() != null && detail.getTicket().getId() != null) {
                    detail.setTicket(entityManager.getReference(com.vetautet.infrastructure.persistence.entity.TicketEntity.class, detail.getTicket().getId()));
                }
            });
        }
        BookingEntity saved = jpaRepository.save(entity);
        monthlyMirrorRepository.upsert(saved);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByOrderNumber(String orderNumber) {
        String storageMonth = orderNumberParser.extractStorageMonth(orderNumber);
        String monthlyTable = monthlyTableRouter.resolveTableByStorageMonth(storageMonth);
        System.out.println(">>> [BOOKING ROUTE] orderNumber=" + orderNumber + " storageMonth=" + storageMonth + " table=" + monthlyTable);
        Optional<Long> bookingId = monthlyMirrorRepository.findBookingIdByOrderNumber(orderNumber, storageMonth);
        if (bookingId.isPresent()) {
            return jpaRepository.findById(bookingId.get()).map(mapper::toDomain);
        }
        return jpaRepository.findByOrderNumber(orderNumber).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByAsyncRequestId(String asyncRequestId) {
        if (asyncRequestId == null || asyncRequestId.isBlank()) {
            return Optional.empty();
        }
        return jpaRepository.findByAsyncRequestId(asyncRequestId).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByIdFetched(Long id) {
        return jpaRepository.findByIdFetched(id).map(mapper::toDomain);
    }

    @Override
    public List<Booking> findByUserId(Long userId) {
        List<Long> monthlyBookingIds = monthlyMirrorRepository.findRecentBookingIdsByUserId(userId);
        if (!monthlyBookingIds.isEmpty()) {
            Map<Long, Booking> bookingById = new HashMap<>();
            jpaRepository.findByIdsFetched(monthlyBookingIds).stream()
                    .map(mapper::toDomain)
                    .forEach(booking -> bookingById.put(booking.getId(), booking));

            return monthlyBookingIds.stream()
                    .map(bookingById::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return jpaRepository.findByUserIdFetched(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findExpiredPendingBookings(LocalDateTime now) {
        List<Long> bookingIds = jpaRepository.findIdsByStatusAndExpiredAtBefore(
                BookingEntity.BookingStatus.PENDING,
                now
        );
        if (bookingIds.isEmpty()) {
            return List.of();
        }

        return jpaRepository.findByIdsFetched(bookingIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.findById(id).ifPresent(entity ->
                monthlyMirrorRepository.deleteBy(entity.getStorageMonth(), entity.getId(), entity.getOrderNumber())
        );
        jpaRepository.deleteById(id);
    }

}
