package com.vetautet.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.infrastructure.persistence.entity.OutboxEventEntity;
import com.vetautet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxEventPublisher {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${vetautet.kafka.producer.enabled:false}")
    private boolean kafkaProducerEnabled;

    @Value("${vetautet.outbox.publisher.enabled:true}")
    private boolean outboxPublisherEnabled;

    @Value("${vetautet.outbox.publisher.batch-size:200}")
    private int batchSize;

    @Value("${vetautet.outbox.publisher.retry-delay-seconds:5}")
    private long retryDelaySeconds;

    @Value("${vetautet.outbox.publisher.send-timeout-seconds:10}")
    private long sendTimeoutSeconds;

    @Value("${vetautet.outbox.cleanup.enabled:true}")
    private boolean outboxCleanupEnabled;

    @Value("${vetautet.outbox.cleanup.retention-hours:168}")
    private long cleanupRetentionHours;

    @Value("${vetautet.outbox.cleanup.batch-size:1000}")
    private int cleanupBatchSize;

    public OutboxEventPublisher(OutboxEventJpaRepository outboxEventJpaRepository,
                                KafkaTemplate<String, Object> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${vetautet.outbox.publisher.fixed-delay-ms:500}")
    @Transactional
    public void publishPendingEvents() {
        if (!outboxPublisherEnabled || !kafkaProducerEnabled) {
            return;
        }

        List<OutboxEventEntity> events = outboxEventJpaRepository.findPendingForPublish(
                LocalDateTime.now(),
                PageRequest.of(0, Math.max(1, batchSize))
        );

        for (OutboxEventEntity event : events) {
            publishOne(event);
        }
    }

    @Scheduled(fixedDelayString = "${vetautet.outbox.cleanup.fixed-delay-ms:300000}")
    @Transactional
    public void cleanupPublishedEvents() {
        if (!outboxCleanupEnabled) {
            return;
        }

        LocalDateTime before = LocalDateTime.now().minusHours(Math.max(1, cleanupRetentionHours));
        List<Long> ids = outboxEventJpaRepository.findPublishedIdsBefore(
                before,
                PageRequest.of(0, Math.max(1, cleanupBatchSize))
        );
        if (ids.isEmpty()) {
            return;
        }

        int deleted = outboxEventJpaRepository.deleteByIds(ids);
        if (deleted > 0) {
            System.out.println(">>> [OUTBOX CLEANUP] deleted=" + deleted + " publishedBefore=" + before);
        }
    }

    private void publishOne(OutboxEventEntity event) {
        try {
            Object payload = deserializePayload(event);
            kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            markPublished(event);
            System.out.println(">>> [OUTBOX PUBLISHED] eventType=" + event.getEventType()
                    + " topic=" + event.getTopic() + " key=" + event.getEventKey());
        } catch (Exception ex) {
            markFailed(event, ex);
            System.err.println(">>> [OUTBOX PUBLISH FAILED] eventType=" + event.getEventType()
                    + " topic=" + event.getTopic()
                    + " key=" + event.getEventKey()
                    + " error=" + rootMessage(ex));
        }
    }

    private Object deserializePayload(OutboxEventEntity event) throws Exception {
        Class<?> payloadClass = Class.forName(event.getPayloadType());
        return objectMapper.readValue(event.getPayloadJson(), payloadClass);
    }

    private void markPublished(OutboxEventEntity event) {
        LocalDateTime now = LocalDateTime.now();
        event.setPublished(true);
        event.setStatus(OutboxEventEntity.OutboxStatus.PUBLISHED);
        event.setPublishedAt(now);
        event.setUpdatedAt(now);
        event.setLastError(null);
        event.setNextRetryAt(null);
        outboxEventJpaRepository.save(event);
    }

    private void markFailed(OutboxEventEntity event, Exception ex) {
        LocalDateTime now = LocalDateTime.now();
        int retryCount = event.getRetryCount() + 1;
        event.setRetryCount(retryCount);
        event.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
        event.setLastError(truncate(rootMessage(ex), 1000));
        event.setNextRetryAt(now.plusSeconds(retryDelaySeconds));
        event.setUpdatedAt(now);
        outboxEventJpaRepository.save(event);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
