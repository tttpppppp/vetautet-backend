package com.vetautet.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order-created";
    public static final String ORDER_CANCELLED_TOPIC = "order-cancelled";
    public static final String PAYMENT_CONFIRMED_TOPIC = "payment-confirmed";
    public static final String MAIL_SEND_REQUESTED_TOPIC = "mail-send-requested";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(ORDER_CANCELLED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentConfirmedTopic() {
        return TopicBuilder.name(PAYMENT_CONFIRMED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic mailSendRequestedTopic() {
        return TopicBuilder.name(MAIL_SEND_REQUESTED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
