package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatJpaRepository extends JpaRepository<SeatEntity, Long> {
    @Query("SELECT s FROM SeatEntity s " +
            "JOIN FETCH s.carriage c " +
            "JOIN FETCH c.type " +
            "WHERE c.train.id = :trainId " +
            "AND s.deletedAt IS NULL " +
            "AND c.deletedAt IS NULL")
    List<SeatEntity> findActiveByTrainId(@Param("trainId") Long trainId);
}
