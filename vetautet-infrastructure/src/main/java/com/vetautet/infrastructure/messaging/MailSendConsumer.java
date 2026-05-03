package com.vetautet.infrastructure.messaging;

import com.vetautet.domain.model.BookingMailEvent;
import com.vetautet.infrastructure.config.KafkaConfig;
import com.vetautet.infrastructure.notification.MailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MailSendConsumer {

    private final MailService mailService;

    public MailSendConsumer(MailService mailService) {
        this.mailService = mailService;
    }

    @KafkaListener(topics = KafkaConfig.MAIL_SEND_REQUESTED_TOPIC, groupId = "vetautet-mail-group", autoStartup = "${vetautet.kafka.listeners.enabled:true}")
    public void handleMailSendRequested(BookingMailEvent event) {
        System.out.println(">>> [KAFKA] Received mail-send-requested for bookingId=" + event.getBookingId());
        mailService.sendBookingConfirmationMail(event);
    }
}
