package com.example.models;

import java.time.LocalDateTime;

/**
 * Represents a customer review and rating for a product.
 */
public class ProductReview {
    private String id;
    private String productId;
    private String customerId;
    private String customerName;
    private int rating; // 1-5 stars
    private String title;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private boolean isVerifiedPurchase;
    private int helpfulCount; // Number of users who found this review helpful
    private ReviewStatus status;
    
    public enum ReviewStatus {
        PENDING,
        APPROVED,
        REJECTED,
        FLAGGED
    }
    
    public ProductReview(String productId, String customerId, String customerName, 
                        int rating, String title, String comment) {
        this.id = java.util.UUID.randomUUID().toString();
        this.productId = productId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.isVerifiedPurchase = false;
        this.helpfulCount = 0;
        this.status = ReviewStatus.PENDING;
    }
    
    // Getters and setters
    public String getId() { return id; }
    
    public String getProductId() { return productId; }
    
    public String getCustomerId() { return customerId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public int getRating() { return rating; }
    public void setRating(int rating) {
        if (rating >= 1 && rating <= 5) {
            this.rating = rating;
            this.lastUpdated = LocalDateTime.now();
        }
    }
    
    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getComment() { return comment; }
    public void setComment(String comment) {
        this.comment = comment;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    
    public boolean isVerifiedPurchase() { return isVerifiedPurchase; }
    public void setVerifiedPurchase(boolean verifiedPurchase) { this.isVerifiedPurchase = verifiedPurchase; }
    
    public int getHelpfulCount() { return helpfulCount; }
    public void incrementHelpfulCount() { this.helpfulCount++; }
    public void decrementHelpfulCount() { 
        if (this.helpfulCount > 0) {
            this.helpfulCount--; 
        }
    }
    
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    
    // Utility methods
    public boolean isApproved() {
        return status == ReviewStatus.APPROVED;
    }
    
    public String getFormattedRating() {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        return stars.toString();
    }
    
    public String getTimeAgo() {
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(createdAt, now);
        
        long days = duration.toDays();
        if (days > 0) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }
        
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        }
        
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        }
        
        return "Just now";
    }
    
    @Override
    public String toString() {
        return String.format("ProductReview{id='%s', rating=%d, customer='%s', status=%s}", 
                id, rating, customerName, status);
    }
}
