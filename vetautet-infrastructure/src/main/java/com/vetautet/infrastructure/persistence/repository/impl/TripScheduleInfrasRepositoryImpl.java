package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Station;
import com.vetautet.domain.model.TripSegment;
import com.vetautet.domain.model.TripSegmentPrice;
import com.vetautet.domain.model.TripStop;
import com.vetautet.domain.repository.TripScheduleRepository;
import com.vetautet.infrastructure.persistence.entity.*;
import com.vetautet.infrastructure.persistence.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class TripScheduleInfrasRepositoryImpl implements TripScheduleRepository {

    @Autowired
    private TripJpaRepository tripJpaRepository;

    @Autowired
    private TripStopJpaRepository tripStopJpaRepository;

    @Autowired
    private TripSegmentJpaRepository tripSegmentJpaRepository;

    @Autowired
    private TripSegmentPriceJpaRepository tripSegmentPriceJpaRepository;

    @Autowired
    private SeatSegmentInventoryJpaRepository seatSegmentInventoryJpaRepository;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<TripStop> findStopsByTripId(Long tripId) {
        return tripStopJpaRepository.findByTrip_IdOrderByStopOrderAsc(tripId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripSegment> findSegmentsByTripId(Long tripId) {
        return tripSegmentJpaRepository.findByTrip_IdOrderBySegmentOrderAsc(tripId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripSegmentPrice> findPricesByTripId(Long tripId) {
        return tripSegmentPriceJpaRepository.findByTripId(tripId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripSegmentPrice> findPricesBySegmentIds(List<Long> segmentIds, Long carriageTypeId, String passengerType) {
        return tripSegmentPriceJpaRepository.findActivePrices(
                        segmentIds,
                        carriageTypeId,
                        normalizePassengerType(passengerType),
                        TripSegmentPriceEntity.PriceStatus.ACTIVE
                ).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, Long> countAvailableSeatsBySegment(Long tripId) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : seatSegmentInventoryJpaRepository.countAvailableByTripId(
                tripId,
                SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE,
                SeatSegmentInventoryEntity.InventoryStatus.HOLD,
                SeatSegmentInventoryEntity.InventoryStatus.QUEUED,
                LocalDateTime.now()
        )) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public long countAvailableSeatsForSegments(Long tripId, List<Long> segmentIds, Long carriageTypeId) {
        if (segmentIds == null || segmentIds.isEmpty()) {
            return 0;
        }
        return seatSegmentInventoryJpaRepository.findAvailableSeatIdsForSegments(
                tripId,
                segmentIds,
                carriageTypeId,
                segmentIds.size(),
                SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE,
                SeatSegmentInventoryEntity.InventoryStatus.HOLD,
                SeatSegmentInventoryEntity.InventoryStatus.QUEUED,
                LocalDateTime.now()
        ).size();
    }

    @Override
    public Map<Long, String> findSeatStatusesForSegments(Long tripId, List<Long> segmentIds) {
        Map<Long, String> statusesBySeatId = new HashMap<>();
        if (tripId == null || segmentIds == null || segmentIds.isEmpty()) {
            return statusesBySeatId;
        }

        Map<Long, List<SeatSegmentInventoryEntity>> itemsBySeatId = seatSegmentInventoryJpaRepository
                .findByTripAndSegments(tripId, segmentIds).stream()
                .filter(item -> item.getSeat() != null && item.getSeat().getId() != null)
                .collect(Collectors.groupingBy(item -> item.getSeat().getId()));

        itemsBySeatId.forEach((seatId, seatItems) ->
                statusesBySeatId.put(seatId, resolveSeatStatus(seatItems, segmentIds.size())));
        return statusesBySeatId;
    }

    @Override
    @Transactional
    public boolean areSeatSegmentsAvailable(Long tripId, Long seatId, List<Long> segmentIds) {
        if (tripId == null || seatId == null || segmentIds == null || segmentIds.isEmpty()) {
            return false;
        }
        List<SeatSegmentInventoryEntity> items = seatSegmentInventoryJpaRepository
                .findForUpdateByTripSeatAndSegments(tripId, seatId, segmentIds);
        releaseExpiredHolds(items, LocalDateTime.now());
        return items.size() == segmentIds.size()
                && items.stream().allMatch(item -> item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE);
    }

    @Override
    @Transactional
    public boolean areSeatSegmentsBookable(Long tripId, Long seatId, List<Long> segmentIds) {
        if (tripId == null || seatId == null || segmentIds == null || segmentIds.isEmpty()) {
            return false;
        }
        List<SeatSegmentInventoryEntity> items = seatSegmentInventoryJpaRepository
                .findForUpdateByTripSeatAndSegments(tripId, seatId, segmentIds);
        releaseExpiredHolds(items, LocalDateTime.now());
        return items.size() == segmentIds.size()
                && items.stream().allMatch(item -> item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE
                || item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.QUEUED);
    }

    @Override
    @Transactional
    public void queueSeatSegments(Long tripId, Long seatId, List<Long> segmentIds, LocalDateTime queueExpiredAt) {
        List<SeatSegmentInventoryEntity> items = seatSegmentInventoryJpaRepository
                .findForUpdateByTripSeatAndSegments(tripId, seatId, segmentIds);
        if (items.size() != segmentIds.size()) {
            throw new RuntimeException("Seat segment inventory is not configured for selected route");
        }
        LocalDateTime now = LocalDateTime.now();
        releaseExpiredHolds(items, now);
        if (items.stream().anyMatch(item -> item.getStatus() != SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE)) {
            throw new RuntimeException("Seat is no longer available for selected route segments");
        }

        for (SeatSegmentInventoryEntity item : items) {
            item.setStatus(SeatSegmentInventoryEntity.InventoryStatus.QUEUED);
            item.setHoldExpiredAt(queueExpiredAt);
            item.setBookingDetail(null);
            item.setUpdatedAt(now);
        }
        seatSegmentInventoryJpaRepository.saveAll(items);
    }

    @Override
    @Transactional
    public void releaseQueuedSeatSegments(Long tripId, Long seatId, List<Long> segmentIds) {
        if (tripId == null || seatId == null || segmentIds == null || segmentIds.isEmpty()) {
            return;
        }
        List<SeatSegmentInventoryEntity> items = seatSegmentInventoryJpaRepository
                .findForUpdateByTripSeatAndSegments(tripId, seatId, segmentIds);
        LocalDateTime now = LocalDateTime.now();
        for (SeatSegmentInventoryEntity item : items) {
            if (item.getStatus() != SeatSegmentInventoryEntity.InventoryStatus.QUEUED) {
                continue;
            }
            item.setStatus(SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE);
            item.setHoldExpiredAt(null);
            item.setBookingDetail(null);
            item.setUpdatedAt(now);
        }
        seatSegmentInventoryJpaRepository.saveAll(items);
    }

    @Override
    @Transactional
    public void holdSeatSegments(Long tripId,
                                 Long seatId,
                                 List<Long> segmentIds,
                                 Long bookingDetailId,
                                 LocalDateTime holdExpiredAt) {
        if (bookingDetailId == null) {
            throw new RuntimeException("Booking detail id is required to hold seat segments");
        }
        List<SeatSegmentInventoryEntity> items = seatSegmentInventoryJpaRepository
                .findForUpdateByTripSeatAndSegments(tripId, seatId, segmentIds);
        if (items.size() != segmentIds.size()) {
            throw new RuntimeException("Seat segment inventory is not configured for selected route");
        }
        LocalDateTime now = LocalDateTime.now();
        releaseExpiredHolds(items, now);
        if (items.stream().anyMatch(item -> item.getStatus() != SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE
                && item.getStatus() != SeatSegmentInventoryEntity.InventoryStatus.QUEUED)) {
            throw new RuntimeException("Seat is no longer available for selected route segments");
        }

        BookingDetailEntity bookingDetail = entityManager.getReference(BookingDetailEntity.class, bookingDetailId);
        for (SeatSegmentInventoryEntity item : items) {
            item.setStatus(SeatSegmentInventoryEntity.InventoryStatus.HOLD);
            item.setHoldExpiredAt(holdExpiredAt);
            item.setBookingDetail(bookingDetail);
            item.setUpdatedAt(now);
        }
        seatSegmentInventoryJpaRepository.saveAll(items);
    }

    @Override
    @Transactional
    public void updateSeatSegmentsForBookingDetail(Long bookingDetailId, String status, LocalDateTime holdExpiredAt) {
        if (bookingDetailId == null) {
            return;
        }
        List<SeatSegmentInventoryEntity> items = seatSegmentInventoryJpaRepository.findByBookingDetail_Id(bookingDetailId);
        if (items.isEmpty()) {
            return;
        }
        SeatSegmentInventoryEntity.InventoryStatus targetStatus = toInventoryStatus(status);
        LocalDateTime now = LocalDateTime.now();
        for (SeatSegmentInventoryEntity item : items) {
            item.setStatus(targetStatus);
            item.setHoldExpiredAt(holdExpiredAt);
            if (targetStatus == SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE) {
                item.setBookingDetail(null);
            }
            item.setUpdatedAt(now);
        }
        seatSegmentInventoryJpaRepository.saveAll(items);
    }

    @Override
    @Transactional
    public void replaceStops(Long tripId, List<TripStop> stops) {
        TripEntity trip = tripJpaRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

        seatSegmentInventoryJpaRepository.deleteByTrip_Id(tripId);
        tripSegmentPriceJpaRepository.deleteBySegment_Trip_Id(tripId);
        tripSegmentJpaRepository.deleteByTrip_Id(tripId);
        tripStopJpaRepository.deleteByTrip_Id(tripId);
        entityManager.flush();

        List<TripStopEntity> savedStops = stops.stream()
                .sorted(Comparator.comparing(TripStop::getStopOrder))
                .map(stop -> toEntity(stop, trip))
                .collect(Collectors.toList());
        savedStops = tripStopJpaRepository.saveAll(savedStops);

        List<TripSegmentEntity> savedSegments = buildSegments(trip, savedStops);
        savedSegments = tripSegmentJpaRepository.saveAll(savedSegments);
        seedInventory(trip, savedSegments);
    }

    @Override
    @Transactional
    public List<TripSegmentPrice> upsertSegmentPrices(Long tripId, List<TripSegmentPrice> prices) {
        for (TripSegmentPrice price : prices) {
            TripSegmentEntity segment = tripSegmentJpaRepository.findById(price.getSegmentId())
                    .orElseThrow(() -> new RuntimeException("Segment not found: " + price.getSegmentId()));
            if (!Objects.equals(segment.getTrip().getId(), tripId)) {
                throw new RuntimeException("Segment does not belong to trip: " + price.getSegmentId());
            }

            String passengerType = normalizePassengerType(price.getPassengerType());
            TripSegmentPriceEntity entity = tripSegmentPriceJpaRepository
                    .findBySegment_IdAndCarriageType_IdAndPassengerType(
                            price.getSegmentId(),
                            price.getCarriageTypeId(),
                            passengerType
                    )
                    .orElseGet(TripSegmentPriceEntity::new);

            entity.setSegment(segment);
            entity.setCarriageType(entityManager.getReference(CarriageTypeEntity.class, price.getCarriageTypeId()));
            entity.setPassengerType(passengerType);
            entity.setPrice(price.getPrice());
            entity.setCurrency(normalizeCurrency(price.getCurrency()));
            entity.setStatus(toPriceStatus(price.getStatus()));
            entity.setEffectiveFrom(price.getEffectiveFrom());
            entity.setEffectiveTo(price.getEffectiveTo());
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }
            entity.setUpdatedAt(LocalDateTime.now());
            tripSegmentPriceJpaRepository.save(entity);
        }
        return findPricesByTripId(tripId);
    }

    private TripStopEntity toEntity(TripStop stop, TripEntity trip) {
        TripStopEntity entity = new TripStopEntity();
        entity.setTrip(trip);
        entity.setStation(entityManager.getReference(StationEntity.class, stop.getStation().getId()));
        entity.setStopOrder(stop.getStopOrder());
        entity.setScheduledArrivalTime(stop.getScheduledArrivalTime());
        entity.setScheduledDepartureTime(stop.getScheduledDepartureTime());
        entity.setEstimatedArrivalTime(stop.getEstimatedArrivalTime());
        entity.setEstimatedDepartureTime(stop.getEstimatedDepartureTime());
        entity.setActualArrivalTime(stop.getActualArrivalTime());
        entity.setActualDepartureTime(stop.getActualDepartureTime());
        entity.setDistanceFromOriginKm(stop.getDistanceFromOriginKm());
        entity.setStatus(toStopStatus(stop.getStatus()));
        entity.setPlatform(stop.getPlatform());
        entity.setNote(stop.getNote());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private List<TripSegmentEntity> buildSegments(TripEntity trip, List<TripStopEntity> stops) {
        return java.util.stream.IntStream.range(0, stops.size() - 1)
                .mapToObj(index -> {
                    TripStopEntity from = stops.get(index);
                    TripStopEntity to = stops.get(index + 1);
                    TripSegmentEntity segment = new TripSegmentEntity();
                    segment.setTrip(trip);
                    segment.setFromStop(from);
                    segment.setToStop(to);
                    segment.setSegmentOrder(from.getStopOrder());
                    segment.setDistanceKm(distanceBetween(from, to));
                    segment.setStatus(TripSegmentEntity.TripSegmentStatus.SCHEDULED);
                    segment.setCreatedAt(LocalDateTime.now());
                    segment.setUpdatedAt(LocalDateTime.now());
                    return segment;
                })
                .collect(Collectors.toList());
    }

    private void seedInventory(TripEntity trip, List<TripSegmentEntity> segments) {
        if (trip.getTrain() == null || trip.getTrain().getId() == null || segments.isEmpty()) {
            return;
        }

        List<SeatEntity> seats = seatJpaRepository.findActiveByTrainId(trip.getTrain().getId());
        if (seats.isEmpty()) {
            return;
        }

        List<SeatSegmentInventoryEntity> inventory = segments.stream()
                .flatMap(segment -> seats.stream().map(seat -> {
                    SeatSegmentInventoryEntity item = new SeatSegmentInventoryEntity();
                    item.setTrip(trip);
                    item.setSegment(segment);
                    item.setSeat(seat);
                    item.setStatus(SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE);
                    item.setCreatedAt(LocalDateTime.now());
                    item.setUpdatedAt(LocalDateTime.now());
                    return item;
                }))
                .collect(Collectors.toList());
        seatSegmentInventoryJpaRepository.saveAll(inventory);
    }

    private BigDecimal distanceBetween(TripStopEntity from, TripStopEntity to) {
        if (from.getDistanceFromOriginKm() == null || to.getDistanceFromOriginKm() == null) {
            return BigDecimal.ZERO;
        }
        return to.getDistanceFromOriginKm().subtract(from.getDistanceFromOriginKm()).max(BigDecimal.ZERO);
    }

    private TripStop toDomain(TripStopEntity entity) {
        return TripStop.builder()
                .id(entity.getId())
                .tripId(entity.getTrip().getId())
                .station(toStationDomain(entity.getStation()))
                .stopOrder(entity.getStopOrder())
                .scheduledArrivalTime(entity.getScheduledArrivalTime())
                .scheduledDepartureTime(entity.getScheduledDepartureTime())
                .estimatedArrivalTime(entity.getEstimatedArrivalTime())
                .estimatedDepartureTime(entity.getEstimatedDepartureTime())
                .actualArrivalTime(entity.getActualArrivalTime())
                .actualDepartureTime(entity.getActualDepartureTime())
                .distanceFromOriginKm(entity.getDistanceFromOriginKm())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .platform(entity.getPlatform())
                .note(entity.getNote())
                .build();
    }

    private TripSegment toDomain(TripSegmentEntity entity) {
        return TripSegment.builder()
                .id(entity.getId())
                .tripId(entity.getTrip().getId())
                .fromStop(toDomain(entity.getFromStop()))
                .toStop(toDomain(entity.getToStop()))
                .segmentOrder(entity.getSegmentOrder())
                .distanceKm(entity.getDistanceKm())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .build();
    }

    private TripSegmentPrice toDomain(TripSegmentPriceEntity entity) {
        return TripSegmentPrice.builder()
                .id(entity.getId())
                .segmentId(entity.getSegment().getId())
                .carriageTypeId(entity.getCarriageType().getId())
                .carriageTypeCode(entity.getCarriageType().getCode())
                .carriageTypeName(entity.getCarriageType().getName())
                .passengerType(entity.getPassengerType())
                .price(entity.getPrice())
                .currency(entity.getCurrency())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .build();
    }

    private Station toStationDomain(StationEntity entity) {
        return Station.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .location(entity.getLocation())
                .build();
    }

    private String resolveSeatStatus(List<SeatSegmentInventoryEntity> seatItems, int expectedSegmentCount) {
        if (seatItems == null || seatItems.size() < expectedSegmentCount) {
            return SeatSegmentInventoryEntity.InventoryStatus.BLOCKED.name();
        }
        if (seatItems.stream().anyMatch(item -> item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.BOOKED
                || item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.BLOCKED)) {
            return SeatSegmentInventoryEntity.InventoryStatus.BOOKED.name();
        }
        LocalDateTime now = LocalDateTime.now();
        if (seatItems.stream().anyMatch(item -> item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.HOLD
                && !isExpiredHold(item, now))) {
            return SeatSegmentInventoryEntity.InventoryStatus.HOLD.name();
        }
        if (seatItems.stream().anyMatch(item -> item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.QUEUED
                && !isExpiredHold(item, now))) {
            return SeatSegmentInventoryEntity.InventoryStatus.QUEUED.name();
        }
        return SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE.name();
    }

    private void releaseExpiredHolds(List<SeatSegmentInventoryEntity> items, LocalDateTime now) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (SeatSegmentInventoryEntity item : items) {
            if (!isExpiredHold(item, now)) {
                continue;
            }
            item.setStatus(SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE);
            item.setHoldExpiredAt(null);
            item.setBookingDetail(null);
            item.setUpdatedAt(now);
        }
    }

    private boolean isExpiredHold(SeatSegmentInventoryEntity item, LocalDateTime now) {
        return item != null
                && (item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.HOLD
                || item.getStatus() == SeatSegmentInventoryEntity.InventoryStatus.QUEUED)
                && item.getHoldExpiredAt() != null
                && !item.getHoldExpiredAt().isAfter(now);
    }

    private TripStopEntity.TripStopStatus toStopStatus(String status) {
        if (status == null || status.isBlank()) {
            return TripStopEntity.TripStopStatus.SCHEDULED;
        }
        return TripStopEntity.TripStopStatus.valueOf(status.trim().toUpperCase());
    }

    private TripSegmentPriceEntity.PriceStatus toPriceStatus(String status) {
        if (status == null || status.isBlank()) {
            return TripSegmentPriceEntity.PriceStatus.ACTIVE;
        }
        return TripSegmentPriceEntity.PriceStatus.valueOf(status.trim().toUpperCase());
    }

    private SeatSegmentInventoryEntity.InventoryStatus toInventoryStatus(String status) {
        if (status == null || status.isBlank()) {
            return SeatSegmentInventoryEntity.InventoryStatus.AVAILABLE;
        }
        return SeatSegmentInventoryEntity.InventoryStatus.valueOf(status.trim().toUpperCase());
    }

    private String normalizePassengerType(String passengerType) {
        return passengerType == null || passengerType.isBlank()
                ? "ADULT"
                : passengerType.trim().toUpperCase();
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank()
                ? "VND"
                : currency.trim().toUpperCase();
    }
}
