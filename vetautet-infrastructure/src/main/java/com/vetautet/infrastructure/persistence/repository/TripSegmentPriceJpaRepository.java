package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.TripSegmentPriceEntity.PriceStatus;
import com.vetautet.infrastructure.persistence.entity.TripSegmentPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripSegmentPriceJpaRepository extends JpaRepository<TripSegmentPriceEntity, Long> {
    @Query("SELECT p FROM TripSegmentPriceEntity p " +
            "JOIN FETCH p.segment s " +
            "JOIN FETCH p.carriageType ct " +
            "WHERE s.trip.id = :tripId " +
            "ORDER BY s.segmentOrder ASC, ct.id ASC, p.passengerType ASC")
    List<TripSegmentPriceEntity> findByTripId(@Param("tripId") Long tripId);

    @Query("SELECT p FROM TripSegmentPriceEntity p " +
            "JOIN FETCH p.segment s " +
            "JOIN FETCH p.carriageType ct " +
            "WHERE s.id IN :segmentIds " +
            "AND (:carriageTypeId IS NULL OR ct.id = :carriageTypeId) " +
            "AND p.passengerType = :passengerType " +
            "AND p.status = :status " +
            "ORDER BY s.segmentOrder ASC")
    List<TripSegmentPriceEntity> findActivePrices(
            @Param("segmentIds") List<Long> segmentIds,
            @Param("carriageTypeId") Long carriageTypeId,
            @Param("passengerType") String passengerType,
            @Param("status") PriceStatus status
    );

    Optional<TripSegmentPriceEntity> findBySegment_IdAndCarriageType_IdAndPassengerType(
            Long segmentId,
            Long carriageTypeId,
            String passengerType
    );

    void deleteBySegment_Trip_Id(Long tripId);
}
