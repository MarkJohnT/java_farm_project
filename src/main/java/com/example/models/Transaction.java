package com.example.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a payment transaction in the system.
 * Tracks payment processing, order fulfillment, and financial records.
 */
public class Transaction {
    private String id;
    private String orderId;
    private String userId;
    private String paymentMethodId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
    private String processorTransactionId; // External payment processor ID
    private String processorResponse;
    private List<TransactionItem> items;
    private PaymentDetails paymentDetails;
    private String failureReason;
    private int retryCount;
    private String refundReason;
    private String refundTransactionId;
    
    public enum TransactionType {
        PURCHASE("Purchase"),
        REFUND("Refund"),
        PARTIAL_REFUND("Partial Refund"),
        AUTHORIZATION("Authorization"),
        CAPTURE("Capture"),
        VOID("Void");
        
        private final String displayName;
        
        TransactionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum TransactionStatus {
        PENDING("Pending", "⏳", "#FFA500"),
        PROCESSING("Processing", "🔄", "#2196F3"),
        AUTHORIZED("Authorized", "✅", "#4CAF50"),
        COMPLETED("Completed", "✅", "#4CAF50"),
        FAILED("Failed", "❌", "#F44336"),
        CANCELLED("Cancelled", "🚫", "#9E9E9E"),
        REFUNDED("Refunded", "↩️", "#FF9800"),
        PARTIALLY_REFUNDED("Partially Refunded", "↩️", "#FF9800"),
        DISPUTED("Disputed", "⚠️", "#FF5722"),
        EXPIRED("Expired", "⌛", "#795548");
        
        private final String displayName;
        private final String icon;
        private final String color;
        
        TransactionStatus(String displayName, String icon, String color) {
            this.displayName = displayName;
            this.icon = icon;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getColor() { return color; }
        
        public boolean isSuccessful() {
            return this == COMPLETED || this == AUTHORIZED;
        }
        
        public boolean isFinal() {
            return this == COMPLETED || this == FAILED || this == CANCELLED || 
                   this == REFUNDED || this == EXPIRED;
        }
    }
    
    public static class TransactionItem {
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String category;
        
        public TransactionItem(String productId, String productName, int quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        // Getters and setters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
    
    public static class PaymentDetails {
        private String cardLast4;
        private String cardType;
        private String authorizationCode;
        private String gatewayResponse;
        private String processorName;
        private LocalDateTime authorizationTime;
        private String riskScore;
        private String billingAddress;
        
        public PaymentDetails() {}
        
        // Getters and setters
        public String getCardLast4() { return cardLast4; }
        public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
        
        public String getCardType() { return cardType; }
        public void setCardType(String cardType) { this.cardType = cardType; }
        
        public String getAuthorizationCode() { return authorizationCode; }
        public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
        
        public String getGatewayResponse() { return gatewayResponse; }
        public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
        
        public String getProcessorName() { return processorName; }
        public void setProcessorName(String processorName) { this.processorName = processorName; }
        
        public LocalDateTime getAuthorizationTime() { return authorizationTime; }
        public void setAuthorizationTime(LocalDateTime authorizationTime) { this.authorizationTime = authorizationTime; }
        
        public String getRiskScore() { return riskScore; }
        public void setRiskScore(String riskScore) { this.riskScore = riskScore; }
        
        public String getBillingAddress() { return billingAddress; }
        public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    }
    
    public Transaction(String orderId, String userId, String paymentMethodId, 
                      TransactionType type, BigDecimal amount, String currency) {
        this.id = java.util.UUID.randomUUID().toString();
        this.orderId = orderId;
        this.userId = userId;
        this.paymentMethodId = paymentMethodId;
        this.type = type;
        this.status = TransactionStatus.PENDING;
        this.amount = amount;
        this.currency = currency != null ? currency : "USD";
        this.totalAmount = amount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.items = new ArrayList<>();
        this.paymentDetails = new PaymentDetails();
        this.retryCount = 0;
    }
    
    // Getters and setters
    public String getId() { return id; }
    
    public String getOrderId() { return orderId; }
    
    public String getUserId() { return userId; }
    
    public String getPaymentMethodId() { return paymentMethodId; }
    
    public TransactionType getType() { return type; }
    
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status.isSuccessful()) {
            this.processedAt = LocalDateTime.now();
        }
    }
    
    public BigDecimal getAmount() { return amount; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { 
        this.taxAmount = taxAmount;
        calculateTotalAmount();
    }
    
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { 
        this.shippingAmount = shippingAmount;
        calculateTotalAmount();
    }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { 
        this.discountAmount = discountAmount;
        calculateTotalAmount();
    }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    
    public String getCurrency() { return currency; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    
    public String getProcessorTransactionId() { return processorTransactionId; }
    public void setProcessorTransactionId(String processorTransactionId) { 
        this.processorTransactionId = processorTransactionId;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getProcessorResponse() { return processorResponse; }
    public void setProcessorResponse(String processorResponse) { 
        this.processorResponse = processorResponse;
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<TransactionItem> getItems() { return new ArrayList<>(items); }
    public void setItems(List<TransactionItem> items) { 
        this.items = new ArrayList<>(items);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addItem(TransactionItem item) {
        this.items.add(item);
        this.updatedAt = LocalDateTime.now();
    }
    
    public PaymentDetails getPaymentDetails() { return paymentDetails; }
    public void setPaymentDetails(PaymentDetails paymentDetails) { 
        this.paymentDetails = paymentDetails;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { 
        this.failureReason = failureReason;
        this.updatedAt = LocalDateTime.now();
    }
    
    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { 
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { 
        this.refundReason = refundReason;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getRefundTransactionId() { return refundTransactionId; }
    public void setRefundTransactionId(String refundTransactionId) { 
        this.refundTransactionId = refundTransactionId;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Utility methods
    private void calculateTotalAmount() {
        this.totalAmount = amount;
        
        if (taxAmount != null) {
            this.totalAmount = this.totalAmount.add(taxAmount);
        }
        
        if (shippingAmount != null) {
            this.totalAmount = this.totalAmount.add(shippingAmount);
        }
        
        if (discountAmount != null) {
            this.totalAmount = this.totalAmount.subtract(discountAmount);
        }
        
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean canBeRefunded() {
        return status == TransactionStatus.COMPLETED && 
               type == TransactionType.PURCHASE &&
               refundTransactionId == null;
    }
    
    public boolean canBeRetried() {
        return status == TransactionStatus.FAILED && retryCount < 3;
    }
    
    public String getFormattedAmount() {
        return String.format("$%.2f %s", totalAmount, currency);
    }
    
    public String getStatusDisplay() {
        return status.getIcon() + " " + status.getDisplayName();
    }
    
    public boolean isExpired() {
        if (status == TransactionStatus.PENDING || status == TransactionStatus.PROCESSING) {
            // Consider transactions older than 30 minutes as expired
            LocalDateTime expiryTime = createdAt.plusMinutes(30);
            return LocalDateTime.now().isAfter(expiryTime);
        }
        return false;
    }
    
    public int getItemCount() {
        return items.stream().mapToInt(TransactionItem::getQuantity).sum();
    }
    
    public String getSummary() {
        return String.format("%s - %s (%d items) - %s", 
                type.getDisplayName(), 
                getFormattedAmount(), 
                getItemCount(), 
                status.getDisplayName());
    }
    
    @Override
    public String toString() {
        return String.format("Transaction{id='%s', orderId='%s', type=%s, status=%s, amount=%s, currency='%s'}", 
                id, orderId, type, status, totalAmount, currency);
    }
}
