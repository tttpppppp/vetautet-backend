package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.TripSegmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripSegmentJpaRepository extends JpaRepository<TripSegmentEntity, Long> {
    List<TripSegmentEntity> findByTrip_IdOrderBySegmentOrderAsc(Long tripId);
    void deleteByTrip_Id(Long tripId);
}
