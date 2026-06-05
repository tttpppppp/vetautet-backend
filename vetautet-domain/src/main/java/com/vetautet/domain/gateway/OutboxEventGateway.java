package com.vetautet.domain.gateway;

public interface OutboxEventGateway {
    void enqueue(String topic,
                 String eventKey,
                 String eventType,
                 String aggregateType,
                 String aggregateId,
                 Object payload);
}
