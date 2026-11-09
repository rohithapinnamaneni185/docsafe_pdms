package com.pdms.service;

import com.pdms.exception.EmailDispatchException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String SMTP_ERROR_MESSAGE =
            "Failed to dispatch email. Please verify SMTP configuration.";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendOtpEmail(String toEmail, String otpCode) {
        return sendOtpEmail(toEmail, otpCode, null);
    }

    public boolean sendOtpEmail(String toEmail, String otpCode, String recipientName) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new EmailDispatchException(SMTP_ERROR_MESSAGE);
        }

        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new EmailDispatchException(SMTP_ERROR_MESSAGE);
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(username, "DocuSafe Security");
            helper.setTo(toEmail.trim());
            helper.setSubject("DocuSafe Vault: Your 6-Digit Verification Code");

            String plainText = "Your single-use verification code is: " + otpCode
                    + ". This code will expire in 5 minutes. If you did not request this code, please ignore this email.";
            helper.setText(plainText, buildOtpEmailHtml(otpCode, recipientName));
            mailSender.send(message);
            return true;
        } catch (EmailDispatchException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to dispatch DocuSafe verification email.", ex);
            throw new EmailDispatchException(SMTP_ERROR_MESSAGE, ex);
        }
    }

    private String buildOtpEmailHtml(String otpCode, String recipientName) {
        String safeName = escapeHtml(recipientName == null || recipientName.isBlank()
                ? "DocuSafe User" : recipientName);
        return "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;color:#172554;\">"
                + "<h2>DocuSafe Vault verification</h2>"
                + "<p>Hello " + safeName + ",</p>"
                + "<p>Your single-use verification code is: <strong>"
                + escapeHtml(otpCode)
                + "</strong>. This code will expire in 5 minutes. If you did not request this code, please ignore this email.</p>"
                + "</body></html>";
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
