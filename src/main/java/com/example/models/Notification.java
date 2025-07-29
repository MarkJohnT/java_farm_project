package com.example.models;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a notification in the system
 */
public class Notification {
    public enum Type {
        ORDER_CREATED("Order Created", "📦"),
        ORDER_UPDATED("Order Updated", "🔄"),
        ORDER_SHIPPED("Order Shipped", "🚚"),
        ORDER_DELIVERED("Order Delivered", "✅"),
        PAYMENT_RECEIVED("Payment Received", "💳"),
        PAYMENT_FAILED("Payment Failed", "❌"),
        PRODUCT_LOW_STOCK("Low Stock Alert", "⚠️"),
        NEW_MESSAGE("New Message", "💬"),
        ACCOUNT_SECURITY("Security Alert", "🔒"),
        PROMOTIONAL("Promotion", "🎉"),
        SYSTEM_MAINTENANCE("System Maintenance", "🔧");
        
        private final String displayName;
        private final String icon;
        
        Type(String displayName, String icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
    }
    
    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
    
    public enum Status {
        PENDING, SENT, DELIVERED, FAILED, READ
    }
    
    private String id;
    private String userId;
    private Type type;
    private Priority priority;
    private Status status;
    private String title;
    private String message;
    private String actionUrl; // Optional URL for clickable notifications
    private Map<String, String> metadata; // Additional data
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private boolean emailSent;
    private boolean smsSent;
    private boolean pushSent;
    
    public Notification(String userId, Type type, String title, String message) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.priority = Priority.MEDIUM;
        this.status = Status.PENDING;
        this.metadata = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.emailSent = false;
        this.smsSent = false;
        this.pushSent = false;
    }
    
    // Getters and setters
    public String getId() { return id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    
    public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
    public void setMetadata(Map<String, String> metadata) { this.metadata = new HashMap<>(metadata); }
    public void addMetadata(String key, String value) { this.metadata.put(key, value); }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public LocalDateTime getReadAt() { return readAt; }
    public void markAsRead() { 
        this.readAt = LocalDateTime.now();
        this.status = Status.READ;
    }
    
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
    
    public boolean isSmsSent() { return smsSent; }
    public void setSmsSent(boolean smsSent) { this.smsSent = smsSent; }
    
    public boolean isPushSent() { return pushSent; }
    public void setPushSent(boolean pushSent) { this.pushSent = pushSent; }
    
    public boolean isRead() { return readAt != null; }
    
    /**
     * Create an order notification
     */
    public static Notification createOrderNotification(String userId, String orderId, 
                                                     Type orderType, String customerName, String productName) {
        String title = orderType.getDisplayName();
        String message;
        
        switch (orderType) {
            case ORDER_CREATED:
                message = String.format("New order from %s for %s", customerName, productName);
                break;
            case ORDER_UPDATED:
                message = String.format("Order #%s has been updated", orderId);
                break;
            case ORDER_SHIPPED:
                message = String.format("Your order #%s has been shipped", orderId);
                break;
            case ORDER_DELIVERED:
                message = String.format("Your order #%s has been delivered", orderId);
                break;
            default:
                message = String.format("Order #%s status updated", orderId);
        }
        
        Notification notification = new Notification(userId, orderType, title, message);
        notification.addMetadata("orderId", orderId);
        notification.addMetadata("customerName", customerName);
        notification.addMetadata("productName", productName);
        notification.setActionUrl("/orders/" + orderId);
        
        return notification;
    }
    
    /**
     * Create a security notification
     */
    public static Notification createSecurityNotification(String userId, String action, String location) {
        String title = "Security Alert";
        String message = String.format("Account %s from %s", action, location);
        
        Notification notification = new Notification(userId, Type.ACCOUNT_SECURITY, title, message);
        notification.setPriority(Priority.HIGH);
        notification.addMetadata("action", action);
        notification.addMetadata("location", location);
        
        return notification;
    }
    
    /**
     * Create a low stock notification
     */
    public static Notification createLowStockNotification(String userId, String productName, int currentStock) {
        String title = "Low Stock Alert";
        String message = String.format("Product '%s' is running low (only %d left)", productName, currentStock);
        
        Notification notification = new Notification(userId, Type.PRODUCT_LOW_STOCK, title, message);
        notification.setPriority(Priority.HIGH);
        notification.addMetadata("productName", productName);
        notification.addMetadata("currentStock", String.valueOf(currentStock));
        notification.setActionUrl("/products/manage");
        
        return notification;
    }
    
    /**
     * Create a promotional notification
     */
    public static Notification createPromotionalNotification(String userId, String promotionTitle, 
                                                           String promotionDetails, String promoCode) {
        Notification notification = new Notification(userId, Type.PROMOTIONAL, promotionTitle, promotionDetails);
        notification.setPriority(Priority.LOW);
        notification.addMetadata("promoCode", promoCode);
        
        return notification;
    }
    
    @Override
    public String toString() {
        return String.format("Notification{id='%s', type=%s, title='%s', status=%s, created=%s}", 
                id, type, title, status, createdAt);
    }
}
