package com.example;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import java.io.UnsupportedEncodingException;

public class EmailUtil {
    // ✅ Brevo SMTP server config
    private static final String SMTP_HOST = "smtp-relay.brevo.com";
    private static final String SMTP_PORT = "587";

    // ✅ Your Brevo SMTP login (not your Gmail!)
    private static final String USERNAME = "932f87001@smtp-brevo.com"; // Replace with your Brevo SMTP login
    private static final String PASSWORD = "fsYmgKwQEtOzUVAN"; // Replace with your Brevo SMTP password (keep secure!)

    public static void sendVerificationEmail(String to, String verificationCode) throws MessagingException, UnsupportedEncodingException {
        // Validate email address
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        
        // Clean and validate the email address
        String cleanEmail = to.trim();
        if (!cleanEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email address format: " + cleanEmail);
        }
        
        System.out.println("Sending email to: '" + cleanEmail + "'");
        System.out.println("Verification code: " + verificationCode);
        
        // SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST); // Trust the host
        props.put("mail.smtp.connectiontimeout", "10000"); // 10 seconds
        props.put("mail.smtp.timeout", "10000"); // 10 seconds

        // Create mail session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            // Compose the message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME, "Agro Stock Support")); // Friendly sender name
            
            // Use a more explicit way to set the recipient
            InternetAddress[] recipients = { new InternetAddress(cleanEmail) };
            message.setRecipients(Message.RecipientType.TO, recipients);
            
            message.setSubject("Verify your email address - Agro Stock");
            
            // Set additional headers for better deliverability
            message.setHeader("X-Priority", "1");
            message.setHeader("X-Mailer", "Agro Stock Application");

            // Email body (HTML with better formatting)
            String content = "<!DOCTYPE html>"
                    + "<html><head><meta charset='UTF-8'></head><body>"
                    + "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #2E8B57;'>Email Verification - Agro Stock</h2>"
                    + "<p>Hello,</p>"
                    + "<p>Thank you for registering with Agro Stock. Please use the verification code below to complete your registration:</p>"
                    + "<div style='background-color: #f0f0f0; padding: 20px; text-align: center; font-size: 24px; font-weight: bold; color: #2E8B57; border-radius: 5px; margin: 20px 0;'>"
                    + verificationCode
                    + "</div>"
                    + "<p>Please enter this code in the application to complete your email verification.</p>"
                    + "<p>If you didn't request this verification, please ignore this email.</p>"
                    + "<br>"
                    + "<p>Best regards,<br>Agro Stock Support Team</p>"
                    + "</div></body></html>";

            message.setContent(content, "text/html; charset=UTF-8");

            // Send the email
            Transport.send(message);
            System.out.println("Verification email sent successfully to: " + cleanEmail);
            
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Send a notification email
     * @param to Recipient email address
     * @param subject Email subject
     * @param body HTML email body
     * @throws MessagingException if email sending fails
     * @throws UnsupportedEncodingException if encoding fails
     */
    public static void sendNotificationEmail(String to, String subject, String body) 
            throws MessagingException, UnsupportedEncodingException {
        // Validate email address
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        
        // Clean and validate the email address
        String cleanEmail = to.trim();
        if (!cleanEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email address format: " + cleanEmail);
        }
        
        System.out.println("Sending notification email to: '" + cleanEmail + "'");
        System.out.println("Subject: " + subject);
        
        // SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        // Create mail session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            // Create the email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME, "FarmConnect Notifications"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(cleanEmail));
            message.setSubject(subject);
            
            // Set HTML content
            message.setContent(body, "text/html; charset=UTF-8");

            // Send the email
            Transport.send(message);
            System.out.println("Notification email sent successfully to: " + cleanEmail);
            
        } catch (Exception e) {
            System.err.println("Error sending notification email: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Send a password reset email
     * @param to Recipient email address
     * @param resetToken Password reset token
     * @throws MessagingException if email sending fails
     * @throws UnsupportedEncodingException if encoding fails
     */
    public static void sendPasswordResetEmail(String to, String resetToken) 
            throws MessagingException, UnsupportedEncodingException {
        String subject = "🔒 Password Reset Request - FarmConnect";
        
        String body = "<html><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 10px;'>" +
                "<h2 style='color: #22c55e; text-align: center;'>🔒 Password Reset Request</h2>" +
                "<p>Hello,</p>" +
                "<p>We received a request to reset your password for your FarmConnect account.</p>" +
                "<p>Please use the following token to reset your password:</p>" +
                "<div style='background-color: #e9ecef; padding: 15px; border-radius: 5px; " +
                "text-align: center; font-size: 24px; font-weight: bold; color: #495057; " +
                "letter-spacing: 2px; margin: 20px 0;'>" +
                resetToken +
                "</div>" +
                "<p style='color: #dc3545; font-weight: bold;'>⚠️ This token will expire in 15 minutes.</p>" +
                "<p>If you didn't request this password reset, please ignore this email and your " +
                "password will remain unchanged.</p>" +
                "<hr style='margin: 30px 0;'>" +
                "<p style='color: #6c757d; font-size: 12px;'>This is an automated message from FarmConnect. " +
                "Please do not reply to this email.</p>" +
                "</div>" +
                "</body></html>";
        
        sendNotificationEmail(to, subject, body);
    }
}
