package com.example.services;

import com.example.models.Notification;
import com.example.models.User;
import com.example.EmailUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * Service for managing and sending notifications
 */
public class NotificationService {
    private static NotificationService instance;
    private final Map<String, List<Notification>> userNotifications;
    
    private NotificationService() {
        this.userNotifications = new ConcurrentHashMap<>();
    }
    
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }
    
    /**
     * Send a notification to a user
     */
    public CompletableFuture<Boolean> sendNotification(User user, Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Store notification
                storeNotification(notification);
                
                // Send via different channels based on user preferences
                boolean emailSent = false;
                boolean smsSent = false;
                
                if (shouldSendEmail(user, notification)) {
                    emailSent = sendEmailNotification(user, notification);
                    notification.setEmailSent(emailSent);
                }
                
                if (shouldSendSms(user, notification)) {
                    smsSent = sendSmsNotification(user, notification);
                    notification.setSmsSent(smsSent);
                }
                
                // Mark as sent if at least one channel succeeded
                if (emailSent || smsSent || !needsExternalSending(user, notification)) {
                    notification.setStatus(Notification.Status.SENT);
                    notification.setSentAt(LocalDateTime.now());
                    return true;
                } else {
                    notification.setStatus(Notification.Status.FAILED);
                    return false;
                }
                
            } catch (Exception e) {
                notification.setStatus(Notification.Status.FAILED);
                System.err.println("Failed to send notification: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Send multiple notifications
     */
    public CompletableFuture<List<Boolean>> sendNotifications(User user, List<Notification> notifications) {
        List<CompletableFuture<Boolean>> futures = notifications.stream()
                .map(notification -> sendNotification(user, notification))
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * Get notifications for a user
     */
    public List<Notification> getUserNotifications(String userId) {
        return userNotifications.getOrDefault(userId, new ArrayList<>());
    }
    
    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(String userId) {
        return getUserNotifications(userId).stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }
    
    /**
     * Get notification count for a user
     */
    public int getUnreadCount(String userId) {
        return getUnreadNotifications(userId).size();
    }
    
    /**
     * Mark notification as read
     */
    public boolean markAsRead(String userId, String notificationId) {
        List<Notification> notifications = getUserNotifications(userId);
        return notifications.stream()
                .filter(n -> n.getId().equals(notificationId))
                .findFirst()
                .map(notification -> {
                    notification.markAsRead();
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(String userId) {
        getUserNotifications(userId).forEach(Notification::markAsRead);
    }
    
    /**
     * Delete a notification
     */
    public boolean deleteNotification(String userId, String notificationId) {
        List<Notification> notifications = getUserNotifications(userId);
        return notifications.removeIf(n -> n.getId().equals(notificationId));
    }
    
    /**
     * Clear all notifications for a user
     */
    public void clearAllNotifications(String userId) {
        userNotifications.remove(userId);
    }
    
    /**
     * Send order-related notifications
     */
    public void sendOrderNotification(User customer, User farmer, String orderId, 
                                    Notification.Type orderType, String productName) {
        // Notify customer
        Notification customerNotification = Notification.createOrderNotification(
                customer.getId(), orderId, orderType, farmer.getFullName(), productName);
        sendNotification(customer, customerNotification);
        
        // Notify farmer (for new orders)
        if (orderType == Notification.Type.ORDER_CREATED) {
            Notification farmerNotification = Notification.createOrderNotification(
                    farmer.getId(), orderId, orderType, customer.getFullName(), productName);
            sendNotification(farmer, farmerNotification);
        }
    }
    
    /**
     * Send security alert
     */
    public void sendSecurityAlert(User user, String action, String location) {
        Notification notification = Notification.createSecurityNotification(user.getId(), action, location);
        sendNotification(user, notification);
    }
    
    /**
     * Send low stock alert to farmer
     */
    public void sendLowStockAlert(User farmer, String productName, int currentStock) {
        Notification notification = Notification.createLowStockNotification(
                farmer.getId(), productName, currentStock);
        sendNotification(farmer, notification);
    }
    
    /**
     * Send promotional notification
     */
    public void sendPromotionalNotification(User user, String title, String details, String promoCode) {
        Notification notification = Notification.createPromotionalNotification(
                user.getId(), title, details, promoCode);
        sendNotification(user, notification);
    }
    
    // Private helper methods
    
    private void storeNotification(Notification notification) {
        userNotifications.computeIfAbsent(notification.getUserId(), k -> new ArrayList<>())
                .add(notification);
    }
    
    private boolean shouldSendEmail(User user, Notification notification) {
        if (!user.isEmailNotificationsEnabled()) return false;
        
        switch (notification.getType()) {
            case ORDER_CREATED:
            case ORDER_UPDATED:
            case ORDER_SHIPPED:
            case ORDER_DELIVERED:
            case PAYMENT_RECEIVED:
            case PAYMENT_FAILED:
                return user.isOrderNotificationsEnabled();
            case PROMOTIONAL:
                return user.isPromotionalNotificationsEnabled();
            case ACCOUNT_SECURITY:
                return true; // Always send security alerts via email
            default:
                return true;
        }
    }
    
    private boolean shouldSendSms(User user, Notification notification) {
        if (!user.isSmsNotificationsEnabled() || user.getPhoneNumber() == null) return false;
        
        // Only send SMS for high priority notifications
        return notification.getPriority() == Notification.Priority.HIGH || 
               notification.getPriority() == Notification.Priority.URGENT;
    }
    
    private boolean needsExternalSending(User user, Notification notification) {
        return shouldSendEmail(user, notification) || shouldSendSms(user, notification);
    }
    
    private boolean sendEmailNotification(User user, Notification notification) {
        try {
            String subject = notification.getType().getIcon() + " " + notification.getTitle();
            String body = buildEmailBody(notification);
            
            EmailUtil.sendNotificationEmail(user.getEmail(), subject, body);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
            return false;
        }
    }
    
    private boolean sendSmsNotification(User user, Notification notification) {
        try {
            // SMS implementation would go here
            // For now, just log that SMS would be sent
            System.out.println("SMS would be sent to " + user.getPhoneNumber() + 
                             ": " + notification.getTitle() + " - " + notification.getMessage());
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send SMS notification: " + e.getMessage());
            return false;
        }
    }
    
    private String buildEmailBody(Notification notification) {
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<h2>").append(notification.getType().getIcon()).append(" ")
            .append(notification.getTitle()).append("</h2>");
        body.append("<p>").append(notification.getMessage()).append("</p>");
        
        if (notification.getActionUrl() != null) {
            body.append("<p><a href=\"").append(notification.getActionUrl())
                .append("\" style=\"background-color: #22c55e; color: white; padding: 10px 20px; ")
                .append("text-decoration: none; border-radius: 5px;\">View Details</a></p>");
        }
        
        body.append("<hr>");
        body.append("<p><small>This notification was sent on ")
            .append(notification.getCreatedAt()).append("</small></p>");
        body.append("</body></html>");
        
        return body.toString();
    }
}
