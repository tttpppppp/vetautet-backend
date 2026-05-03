package com.vetautet.infrastructure.notification;

import com.vetautet.domain.gateway.EmailVerificationMailGateway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailGatewayImpl implements EmailVerificationMailGateway {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${vetautet.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String senderAddress;

    public EmailVerificationMailGatewayImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void sendVerificationOtp(String recipientEmail, String recipientName, String otp, int expiresInMinutes) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            System.out.println(">>> [MAIL MOCK] Email verification OTP to=" + recipientEmail + ", otp=" + otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (senderAddress != null && !senderAddress.isBlank()) {
            message.setFrom(senderAddress);
        }
        message.setTo(recipientEmail);
        message.setSubject("VeTau email verification code");
        message.setText(buildContent(recipientName, otp, expiresInMinutes));

        mailSender.send(message);
        System.out.println(">>> [MAIL] Sent email verification OTP to=" + recipientEmail);
    }

    @Override
    public void sendPasswordResetOtp(String recipientEmail, String recipientName, String otp, int expiresInMinutes) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            System.out.println(">>> [MAIL MOCK] Password reset OTP to=" + recipientEmail + ", otp=" + otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (senderAddress != null && !senderAddress.isBlank()) {
            message.setFrom(senderAddress);
        }
        message.setTo(recipientEmail);
        message.setSubject("VeTau password reset code");
        message.setText(buildPasswordResetContent(recipientName, otp, expiresInMinutes));

        mailSender.send(message);
        System.out.println(">>> [MAIL] Sent password reset OTP to=" + recipientEmail);
    }

    private String buildContent(String recipientName, String otp, int expiresInMinutes) {
        String displayName = (recipientName == null || recipientName.isBlank()) ? "ban" : recipientName;
        return "Xin chao " + displayName + ",\n\n"
                + "Ma xac thuc email VeTau cua ban la: " + otp + "\n"
                + "Ma nay co hieu luc trong " + expiresInMinutes + " phut.\n\n"
                + "Neu ban khong yeu cau dang ky tai khoan, vui long bo qua email nay.\n\n"
                + "VeTau";
    }

    private String buildPasswordResetContent(String recipientName, String otp, int expiresInMinutes) {
        String displayName = (recipientName == null || recipientName.isBlank()) ? "ban" : recipientName;
        return "Xin chao " + displayName + ",\n\n"
                + "Ma dat lai mat khau VeTau cua ban la: " + otp + "\n"
                + "Ma nay co hieu luc trong " + expiresInMinutes + " phut.\n\n"
                + "Neu ban khong yeu cau dat lai mat khau, vui long bo qua email nay.\n\n"
                + "VeTau";
    }
}
