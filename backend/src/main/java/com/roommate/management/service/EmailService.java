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
     * Returns whether the send actually succeeded. Failures are logged here (safe: only the
     * recipient address, never the token/password) but NOT thrown - it's the caller's job to
     * decide what a failure means. For forgot-password specifically, a failure must still not
     * distinguish "this email isn't registered" from "delivery failed", so the caller only acts
     * on this return value from inside the branch where the account is already known to exist.
     */
    public boolean sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
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
            return true;
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
            return false;
        }
    }
}
