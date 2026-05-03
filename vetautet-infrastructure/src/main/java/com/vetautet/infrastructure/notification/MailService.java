package com.vetautet.infrastructure.notification;

import com.vetautet.domain.model.BookingMailEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${vetautet.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String senderAddress;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendBookingConfirmationMail(BookingMailEvent event) {
        if (event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()) {
            System.out.println(">>> [MAIL] Skip booking mail because recipient email is empty. bookingId=" + event.getBookingId());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            logMailFallback(event);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (senderAddress != null && !senderAddress.isBlank()) {
            message.setFrom(senderAddress);
        }
        message.setTo(event.getRecipientEmail());
        message.setSubject(event.getSubject());
        message.setText(event.getContent());

        mailSender.send(message);
        System.out.println(">>> [MAIL] Sent booking confirmation mail. bookingId=" + event.getBookingId()
                + ", to=" + event.getRecipientEmail());
    }

    private void logMailFallback(BookingMailEvent event) {
        System.out.println(">>> [MAIL MOCK] SMTP is disabled or not configured. bookingId=" + event.getBookingId()
                + ", to=" + event.getRecipientEmail()
                + ", subject=" + event.getSubject());
        System.out.println(event.getContent());
    }
}
