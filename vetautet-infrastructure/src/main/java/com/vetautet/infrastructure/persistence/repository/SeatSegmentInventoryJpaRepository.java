package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.SeatSegmentInventoryEntity.InventoryStatus;
import com.vetautet.infrastructure.persistence.entity.SeatSegmentInventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatSegmentInventoryJpaRepository extends JpaRepository<SeatSegmentInventoryEntity, Long> {
    void deleteByTrip_Id(Long tripId);

    @Query("SELECT i.segment.id, COUNT(i.id) FROM SeatSegmentInventoryEntity i " +
            "WHERE i.trip.id = :tripId " +
            "AND (i.status = :availableStatus " +
            "OR (i.status = :holdStatus AND i.holdExpiredAt IS NOT NULL AND i.holdExpiredAt <= :now)) " +
            "GROUP BY i.segment.id")
    List<Object[]> countAvailableByTripId(
            @Param("tripId") Long tripId,
            @Param("availableStatus") InventoryStatus availableStatus,
            @Param("holdStatus") InventoryStatus holdStatus,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT i.seat.id FROM SeatSegmentInventoryEntity i " +
            "JOIN i.seat s " +
            "JOIN s.carriage c " +
            "WHERE i.trip.id = :tripId " +
            "AND i.segment.id IN :segmentIds " +
            "AND (:carriageTypeId IS NULL OR c.type.id = :carriageTypeId) " +
            "AND (i.status = :availableStatus " +
            "OR (i.status = :holdStatus AND i.holdExpiredAt IS NOT NULL AND i.holdExpiredAt <= :now)) " +
            "GROUP BY i.seat.id " +
            "HAVING COUNT(DISTINCT i.segment.id) = :segmentCount")
    List<Long> findAvailableSeatIdsForSegments(
            @Param("tripId") Long tripId,
            @Param("segmentIds") List<Long> segmentIds,
            @Param("carriageTypeId") Long carriageTypeId,
            @Param("segmentCount") long segmentCount,
            @Param("availableStatus") InventoryStatus availableStatus,
            @Param("holdStatus") InventoryStatus holdStatus,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM SeatSegmentInventoryEntity i " +
            "WHERE i.trip.id = :tripId " +
            "AND i.seat.id = :seatId " +
            "AND i.segment.id IN :segmentIds")
    List<SeatSegmentInventoryEntity> findForUpdateByTripSeatAndSegments(
            @Param("tripId") Long tripId,
            @Param("seatId") Long seatId,
            @Param("segmentIds") List<Long> segmentIds
    );

    @Query("SELECT i FROM SeatSegmentInventoryEntity i " +
            "JOIN FETCH i.seat s " +
            "WHERE i.trip.id = :tripId " +
            "AND i.segment.id IN :segmentIds")
    List<SeatSegmentInventoryEntity> findByTripAndSegments(
            @Param("tripId") Long tripId,
            @Param("segmentIds") List<Long> segmentIds
    );

    List<SeatSegmentInventoryEntity> findByBookingDetail_Id(Long bookingDetailId);
}
