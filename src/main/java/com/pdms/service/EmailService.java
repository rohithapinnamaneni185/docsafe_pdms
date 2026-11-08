package com.pdms.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Dispatches OTP code via email using JavaMailSender with styled HTML email when SMTP is configured.
     * If SMTP is not configured or offline, safely logs to server console:
     * [DOCUSAFE OTP] Verification code for user@domain: 123456
     */
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        boolean sentViaSmtp = false;
        boolean hasCredentials = (mailUsername != null && !mailUsername.isBlank())
                && (mailPassword != null && !mailPassword.isBlank());

        if (mailSender != null && hasCredentials && mailHost != null && !mailHost.isBlank()) {
            try {
                if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl mailSenderImpl) {
                    if (mailSenderImpl.getPassword() != null && mailSenderImpl.getPassword().contains(" ")) {
                        mailSenderImpl.setPassword(mailSenderImpl.getPassword().replace(" ", ""));
                    }
                }

                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                String from = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername.trim() : "rohithapinnamaneni185@gmail.com";
                helper.setFrom(from, "DocuSafe Vault Security");
                helper.setReplyTo(from);
                helper.setTo(toEmail.trim());
                helper.setSubject("DocuSafe - Two-Factor Verification Code: " + otpCode);

                String plainText = "DocuSafe Security Verification\n\n"
                        + "Your 6-Digit Security Verification Code is: " + otpCode + "\n\n"
                        + "This code is valid for 5 minutes. Do not share this code with anyone.\n"
                        + "If you did not request this verification code, please ignore this email.\n\n"
                        + "© 2026 DocuSafe Systems. Encrypted & Secure Vault Protection.";

                String html = "<div style=\"max-width:540px;margin:20px auto;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;border:1px solid #e2e8f0;border-radius:12px;padding:36px;background:#ffffff;box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);\">"
                        + "<div style=\"text-align:center;margin-bottom:28px;\">"
                        + "<div style=\"display:inline-block;width:48px;height:48px;background:linear-gradient(135deg,#3b82f6,#4f46e5);border-radius:12px;line-height:48px;text-align:center;color:#ffffff;font-weight:bold;font-size:24px;\">D</div>"
                        + "<h2 style=\"color:#0f172a;margin:12px 0 4px;font-size:24px;font-weight:700;\">Docu<span style=\"color:#4f46e5;\">Safe</span></h2>"
                        + "<p style=\"color:#64748b;font-size:13px;margin:0;\">Multi-Member Personal & Family Document Vault</p>"
                        + "</div>"
                        + "<div style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:24px;text-align:center;margin-bottom:24px;\">"
                        + "<p style=\"color:#334155;font-size:14px;font-weight:500;margin:0 0 14px;\">Your 6-Digit Security Verification Code is:</p>"
                        + "<div style=\"font-size:34px;font-weight:800;letter-spacing:8px;color:#1e1b4b;font-family:'Courier New',monospace;padding:12px 0;background:#ffffff;border:1px dashed #cbd5e1;border-radius:8px;\">"
                        + otpCode
                        + "</div>"
                        + "<p style=\"color:#94a3b8;font-size:12px;margin:14px 0 0;\">⏱ Code is valid for <strong>5 minutes</strong>. Do not share this code.</p>"
                        + "</div>"
                        + "<p style=\"color:#64748b;font-size:13px;line-height:1.6;margin:0 0 16px;\">"
                        + "If you did not attempt to sign in or register with DocuSafe, please safely disregard this email or check your account security."
                        + "</p>"
                        + "<hr style=\"border:none;border-top:1px solid #f1f5f9;margin:20px 0;\">"
                        + "<p style=\"color:#94a3b8;font-size:11px;text-align:center;margin:0;\">"
                        + "© 2026 DocuSafe Systems. Encrypted & Secure Vault Protection."
                        + "</p>"
                        + "</div>";

                helper.setText(plainText, html);
                mailSender.send(mimeMessage);
                sentViaSmtp = true;
                log.info("Email OTP dispatched successfully via live Gmail SMTP to {}", toEmail);
            } catch (Exception ex) {
                log.warn("Gmail SMTP delivery attempt to {} failed (falling back to console simulation): {}", toEmail, ex.getMessage());
            }
        } else if (!hasCredentials) {
            log.info("Live Gmail SMTP credentials not fully configured (SPRING_MAIL_PASSWORD is blank). Using console simulation fallback.");
        }

        // Output console banner as required by project specification
        System.out.println();
        System.out.println("========================================================================");
        System.out.println("  [DOCUSAFE OTP] Verification code for " + toEmail + ": " + otpCode);
        System.out.println("  [VALIDITY] 5 Minutes | Email: " + toEmail + " | Status: " + (sentViaSmtp ? "LIVE GMAIL SMTP SENT" : "CONSOLE SIMULATION"));
        if (!sentViaSmtp && (mailPassword == null || mailPassword.isBlank())) {
            System.out.println("  [NOTE] Set SPRING_MAIL_PASSWORD to a 16-character Google App Password for live inbox delivery.");
        }
        System.out.println("========================================================================");
        System.out.println();

        return sentViaSmtp;
    }
}
