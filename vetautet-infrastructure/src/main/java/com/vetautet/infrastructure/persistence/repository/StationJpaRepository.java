package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface StationJpaRepository extends JpaRepository<StationEntity, Long> {
    @Query("SELECT s FROM StationEntity s WHERE s.deletedAt IS NULL")
    List<StationEntity> findAllActive();
}
