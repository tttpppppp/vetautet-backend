package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.TripStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripStopJpaRepository extends JpaRepository<TripStopEntity, Long> {
    List<TripStopEntity> findByTrip_IdOrderByStopOrderAsc(Long tripId);
    void deleteByTrip_Id(Long tripId);
}
