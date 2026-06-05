package com.vetautet.domain.repository;

import com.vetautet.domain.model.BenchmarkOrder;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BenchmarkOrderRepository {
    BenchmarkOrder save(BenchmarkOrder order);
    Optional<BenchmarkOrder> findByRequestId(String requestId);
    long countAll();
    long countByStatus(String status);
    Optional<LocalDateTime> findLatestProcessedAt();
    void deleteAll();
}
