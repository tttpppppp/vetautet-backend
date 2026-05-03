package com.vetautet.infrastructure.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaTestConsumer {

    @KafkaListener(topics = "test-topic", groupId = "vetautet-group", autoStartup = "${vetautet.kafka.listeners.enabled:true}")
    public void listen(String message) {
        System.out.println(">>> RECEIVED MESSAGE FROM KAFKA: " + message);
    }
}
