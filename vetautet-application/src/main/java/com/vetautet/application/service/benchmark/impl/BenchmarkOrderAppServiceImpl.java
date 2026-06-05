package com.vetautet.application.service.benchmark.impl;

import com.vetautet.application.dto.BenchmarkOrderAcceptedResponse;
import com.vetautet.application.dto.BenchmarkOrderRequest;
import com.vetautet.application.dto.BenchmarkOrderStatsResponse;
import com.vetautet.application.service.benchmark.BenchmarkOrderAppService;
import com.vetautet.domain.gateway.OutboxEventGateway;
import com.vetautet.domain.model.BenchmarkOrder;
import com.vetautet.domain.model.BenchmarkOrderCreatedEvent;
import com.vetautet.domain.gateway.BenchmarkOrderStockGateway;
import com.vetautet.domain.repository.BenchmarkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BenchmarkOrderAppServiceImpl implements BenchmarkOrderAppService {

    private static final String BENCHMARK_ORDER_CREATED_TOPIC = "benchmark-order-created";

    private final OutboxEventGateway outboxEventGateway;
    private final BenchmarkOrderRepository benchmarkOrderRepository;
    private final BenchmarkOrderStockGateway benchmarkOrderStockGateway;

    public BenchmarkOrderAppServiceImpl(OutboxEventGateway outboxEventGateway,
                                        BenchmarkOrderRepository benchmarkOrderRepository,
                                        BenchmarkOrderStockGateway benchmarkOrderStockGateway) {
        this.outboxEventGateway = outboxEventGateway;
        this.benchmarkOrderRepository = benchmarkOrderRepository;
        this.benchmarkOrderStockGateway = benchmarkOrderStockGateway;
    }

    @Override
    @Transactional
    public BenchmarkOrderAcceptedResponse enqueueAsyncOrder(BenchmarkOrderRequest request) {
        Long ticketRef = request.getTicketRef();
        if (ticketRef == null || ticketRef <= 0) {
            return rejected("INVALID_TICKET", "ticketRef là bắt buộc", null);
        }

        int quantity = normalizeQuantity(request.getQuantity());
        int stockResult = benchmarkOrderStockGateway.decreaseStockByLua(ticketRef, quantity);
        if (stockResult == -1) {
            return rejected("STOCK_NOT_PREPARED", "Chưa prepare stock benchmark", null);
        }
        if (stockResult == 0) {
            return rejected("OUT_OF_STOCK", "Hết suất benchmark", null);
        }

        String requestId = normalizeRequestId(request.getRequestId());
        String kafkaKey = request.getUserRef() != null ? request.getUserRef().toString() : requestId;
        LocalDateTime acceptedAt = LocalDateTime.now();
        BenchmarkOrder pendingOrder = BenchmarkOrder.builder()
                .requestId(requestId)
                .userRef(request.getUserRef() != null ? request.getUserRef() : 1L)
                .ticketRef(ticketRef)
                .quantity(quantity)
                .amount(normalizeAmount(request.getAmount()))
                .status("PENDING")
                .source("BENCHMARK_API")
                .note(request.getNote())
                .kafkaKey(kafkaKey)
                .receivedAt(acceptedAt)
                .processedAt(acceptedAt)
                .createdAt(acceptedAt)
                .updatedAt(acceptedAt)
                .build();

        benchmarkOrderRepository.save(pendingOrder);

        BenchmarkOrderCreatedEvent event = BenchmarkOrderCreatedEvent.builder()
                .requestId(requestId)
                .userRef(pendingOrder.getUserRef())
                .ticketRef(ticketRef)
                .quantity(quantity)
                .amount(pendingOrder.getAmount())
                .source("BENCHMARK_API")
                .note(request.getNote())
                .kafkaKey(kafkaKey)
                .receivedAt(acceptedAt)
                .build();

        try {
            outboxEventGateway.enqueue(
                    BENCHMARK_ORDER_CREATED_TOPIC,
                    kafkaKey,
                    "BENCHMARK_ORDER_CREATED",
                    "BENCHMARK_ORDER",
                    requestId,
                    event
            );
        } catch (RuntimeException e) {
            benchmarkOrderStockGateway.increaseStock(ticketRef, quantity);
            pendingOrder.setStatus("FAILED");
            pendingOrder.setNote("OUTBOX_ENQUEUE_FAILED: " + e.getMessage());
            pendingOrder.setUpdatedAt(LocalDateTime.now());
            benchmarkOrderRepository.save(pendingOrder);
            return rejected("OUTBOX_ENQUEUE_FAILED", "Khong ghi duoc outbox event", requestId);
        }

        return BenchmarkOrderAcceptedResponse.builder()
                .success(true)
                .code("QUEUED")
                .message("Đã vào hàng đợi xử lý")
                .placeOrderTaskId(requestId)
                .requestId(requestId)
                .kafkaTopic(BENCHMARK_ORDER_CREATED_TOPIC)
                .kafkaKey(kafkaKey)
                .status("PENDING")
                .acceptedAt(acceptedAt)
                .build();
    }

    @Override
    public BenchmarkOrderStatsResponse getStats(Long ticketRef) {
        return BenchmarkOrderStatsResponse.builder()
                .totalOrders(benchmarkOrderRepository.countAll())
                .createdOrders(benchmarkOrderRepository.countByStatus("CREATED"))
                .pendingOrders(benchmarkOrderRepository.countByStatus("PENDING"))
                .failedOrders(benchmarkOrderRepository.countByStatus("FAILED"))
                .remainingStock(ticketRef == null ? null : benchmarkOrderStockGateway.getStock(ticketRef))
                .latestProcessedAt(benchmarkOrderRepository.findLatestProcessedAt().orElse(null))
                .build();
    }

    @Override
    public void reset(Long ticketRef) {
        benchmarkOrderRepository.deleteAll();
        if (ticketRef != null) {
            benchmarkOrderStockGateway.clearStock(ticketRef);
        }
    }

    @Override
    public void prepare(Long ticketRef, long stock) {
        if (ticketRef == null || ticketRef <= 0) {
            throw new IllegalArgumentException("ticketRef là bắt buộc");
        }
        benchmarkOrderStockGateway.prepareStock(ticketRef, stock);
        benchmarkOrderRepository.deleteAll();
    }

    private String normalizeRequestId(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        return "BENCH-" + UUID.randomUUID();
    }

    private int normalizeQuantity(Integer quantity) {
        return quantity == null || quantity <= 0 ? 1 : quantity;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null || amount.signum() <= 0 ? BigDecimal.valueOf(100000) : amount;
    }

    private BenchmarkOrderAcceptedResponse rejected(String code, String message, String requestId) {
        return BenchmarkOrderAcceptedResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .requestId(requestId)
                .status("REJECTED")
                .acceptedAt(LocalDateTime.now())
                .build();
    }
}
