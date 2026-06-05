package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.BenchmarkOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BenchmarkOrderJpaRepository extends JpaRepository<BenchmarkOrderEntity, Long> {
    Optional<BenchmarkOrderEntity> findByRequestId(String requestId);
    long countByStatus(String status);

    @Query("select max(o.processedAt) from BenchmarkOrderEntity o")
    LocalDateTime findLatestProcessedAt();

    @Modifying
    @Query("delete from BenchmarkOrderEntity")
    void purgeAll();
}
