package com.vetautet.infrastructure.persistence.order;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class BookingOrderNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicInteger sequence = new AtomicInteger(0);

    public String generate(LocalDateTime createdAt) {
        LocalDateTime effectiveCreatedAt = createdAt == null ? LocalDateTime.now() : createdAt;
        int next = sequence.updateAndGet(current -> current >= 999999 ? 1 : current + 1);
        return "ORD-" + effectiveCreatedAt.format(DATE_FORMAT) + "-" + String.format("%06d", next);
    }
}
