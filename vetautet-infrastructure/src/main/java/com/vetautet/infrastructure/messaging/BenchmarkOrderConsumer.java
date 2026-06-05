package com.vetautet.infrastructure.messaging;

import com.vetautet.domain.gateway.BenchmarkOrderStockGateway;
import com.vetautet.domain.model.BenchmarkOrder;
import com.vetautet.domain.model.BenchmarkOrderCreatedEvent;
import com.vetautet.domain.repository.BenchmarkOrderRepository;
import com.vetautet.infrastructure.config.KafkaConfig;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class BenchmarkOrderConsumer {

    private final BenchmarkOrderRepository benchmarkOrderRepository;
    private final BenchmarkOrderStockGateway benchmarkOrderStockGateway;

    public BenchmarkOrderConsumer(BenchmarkOrderRepository benchmarkOrderRepository,
                                  BenchmarkOrderStockGateway benchmarkOrderStockGateway) {
        this.benchmarkOrderRepository = benchmarkOrderRepository;
        this.benchmarkOrderStockGateway = benchmarkOrderStockGateway;
    }

    @KafkaListener(
            topics = KafkaConfig.BENCHMARK_ORDER_CREATED_TOPIC,
            groupId = "vetautet-benchmark-order-group",
            autoStartup = "${vetautet.kafka.listeners.enabled:true}"
    )
    @Transactional
    public void consume(BenchmarkOrderCreatedEvent event) {
        if (event == null || event.getRequestId() == null || event.getRequestId().isBlank()) {
            return;
        }
        BenchmarkOrder existing = benchmarkOrderRepository.findByRequestId(event.getRequestId()).orElse(null);
        if (existing != null && "CREATED".equalsIgnoreCase(existing.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        BenchmarkOrder order = existing != null ? existing : BenchmarkOrder.builder().build();
        order.setRequestId(event.getRequestId());
        order.setUserRef(event.getUserRef());
        order.setTicketRef(event.getTicketRef());
        order.setQuantity(event.getQuantity());
        order.setAmount(event.getAmount());
        order.setSource(event.getSource());
        order.setKafkaKey(event.getKafkaKey());
        order.setReceivedAt(event.getReceivedAt() != null ? event.getReceivedAt() : now);
        order.setCreatedAt(existing != null ? existing.getCreatedAt() : now);
        order.setUpdatedAt(now);

        try {
            order.setStatus("CREATED");
            order.setNote(event.getNote());
            order.setProcessedAt(now);
            benchmarkOrderRepository.save(order);
        } catch (DataIntegrityViolationException ignored) {
            // Duplicate requestId from at-least-once delivery. Ignore for benchmark flow.
        } catch (RuntimeException e) {
            if (event.getTicketRef() != null && event.getQuantity() != null && event.getQuantity() > 0) {
                benchmarkOrderStockGateway.increaseStock(event.getTicketRef(), event.getQuantity());
            }
            order.setStatus("FAILED");
            order.setNote("DB_INSERT_FAILED: " + e.getMessage());
            order.setProcessedAt(now);
            order.setUpdatedAt(now);
            benchmarkOrderRepository.save(order);
        }
    }
}
