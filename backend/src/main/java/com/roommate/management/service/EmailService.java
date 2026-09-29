package com.roommate.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * Best-effort send: failures are logged, not thrown, so callers (e.g. forgot-password)
     * can keep responding identically regardless of whether the email actually went out -
     * otherwise a delivery failure would leak which emails are registered.
     */
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Reset your RoomieHub password");
            message.setText(
                    "Hi " + fullName + ",\n\n" +
                    "We received a request to reset your RoomieHub password. Click the link below to choose a new one:\n\n" +
                    resetLink + "\n\n" +
                    "This link expires in 1 hour and can only be used once. If you didn't request this, you can safely ignore this email.\n\n" +
                    "- RoomieHub"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
        }
    }
}
