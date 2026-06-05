package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.BenchmarkOrder;
import com.vetautet.domain.repository.BenchmarkOrderRepository;
import com.vetautet.infrastructure.persistence.entity.BenchmarkOrderEntity;
import com.vetautet.infrastructure.persistence.repository.BenchmarkOrderJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class BenchmarkOrderInfrasRepositoryImpl implements BenchmarkOrderRepository {

    private final BenchmarkOrderJpaRepository jpaRepository;

    public BenchmarkOrderInfrasRepositoryImpl(BenchmarkOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BenchmarkOrder save(BenchmarkOrder order) {
        BenchmarkOrderEntity entity = toEntity(order);
        BenchmarkOrderEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<BenchmarkOrder> findByRequestId(String requestId) {
        return jpaRepository.findByRequestId(requestId).map(this::toDomain);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }

    @Override
    public long countByStatus(String status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public Optional<LocalDateTime> findLatestProcessedAt() {
        return Optional.ofNullable(jpaRepository.findLatestProcessedAt());
    }

    @Override
    @Transactional
    public void deleteAll() {
        jpaRepository.purgeAll();
    }

    private BenchmarkOrderEntity toEntity(BenchmarkOrder order) {
        BenchmarkOrderEntity entity = new BenchmarkOrderEntity();
        entity.setId(order.getId());
        entity.setRequestId(order.getRequestId());
        entity.setUserRef(order.getUserRef());
        entity.setTicketRef(order.getTicketRef());
        entity.setQuantity(order.getQuantity());
        entity.setAmount(order.getAmount());
        entity.setStatus(order.getStatus());
        entity.setSource(order.getSource());
        entity.setNote(order.getNote());
        entity.setKafkaKey(order.getKafkaKey());
        entity.setReceivedAt(order.getReceivedAt());
        entity.setProcessedAt(order.getProcessedAt());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        return entity;
    }

    private BenchmarkOrder toDomain(BenchmarkOrderEntity entity) {
        return BenchmarkOrder.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .userRef(entity.getUserRef())
                .ticketRef(entity.getTicketRef())
                .quantity(entity.getQuantity())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .source(entity.getSource())
                .note(entity.getNote())
                .kafkaKey(entity.getKafkaKey())
                .receivedAt(entity.getReceivedAt())
                .processedAt(entity.getProcessedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
