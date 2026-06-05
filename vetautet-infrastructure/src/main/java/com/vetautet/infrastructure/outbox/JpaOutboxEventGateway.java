package com.vetautet.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.domain.gateway.OutboxEventGateway;
import com.vetautet.infrastructure.persistence.entity.OutboxEventEntity;
import com.vetautet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class JpaOutboxEventGateway implements OutboxEventGateway {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;

    public JpaOutboxEventGateway(OutboxEventJpaRepository outboxEventJpaRepository,
                                 ObjectMapper objectMapper) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void enqueue(String topic,
                        String eventKey,
                        String eventType,
                        String aggregateType,
                        String aggregateId,
                        Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Outbox payload must not be null");
        }

        LocalDateTime now = LocalDateTime.now();
        OutboxEventEntity event = new OutboxEventEntity();
        event.setEventId(UUID.randomUUID().toString());
        event.setTopic(topic);
        event.setEventKey(eventKey);
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayloadType(payload.getClass().getName());
        event.setPayloadJson(toJson(payload));
        event.setStatus(OutboxEventEntity.OutboxStatus.PENDING);
        event.setPublished(false);
        event.setRetryCount(0);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        outboxEventJpaRepository.save(event);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("OUTBOX_PAYLOAD_SERIALIZATION_FAILED", ex);
        }
    }
}
