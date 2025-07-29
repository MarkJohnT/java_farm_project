package com.example.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an order in the agricultural marketplace
 */
public class Order {
    private String id;
    private String customerId;
    private String farmerId;
    private String customerName;
    private String farmerName;
    private List<OrderItem> items;
    private double total;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String deliveryAddress;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime deliveryDate;
    private String notes;
    
    public enum OrderStatus {
        PLACED("Order Placed"),
        PREPARING("Preparing"),
        PICKED_UP("Picked Up"),
        IN_TRANSIT("In Transit"),
        OUT_FOR_DELIVERY("Out for Delivery"),
        DELIVERED("Delivered"),
        CANCELLED("Cancelled"),
        DELAYED("Delayed");
        
        private final String displayName;
        
        OrderStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public static class OrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private double price;
        private double subtotal;
        
        public OrderItem(String productId, String productName, int quantity, double price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.subtotal = quantity * price;
        }
        
        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { 
            this.quantity = quantity;
            this.subtotal = quantity * price;
        }
        
        public double getPrice() { return price; }
        public void setPrice(double price) { 
            this.price = price;
            this.subtotal = quantity * price;
        }
        
        public double getSubtotal() { return subtotal; }
    }
    
    // Constructors
    public Order() {
        this.id = java.util.UUID.randomUUID().toString();
        this.items = new ArrayList<>();
        this.status = OrderStatus.PLACED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.paymentStatus = "PENDING";
    }
    
    public Order(String customerId, String farmerId) {
        this();
        this.customerId = customerId;
        this.farmerId = farmerId;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { 
        this.items = items;
        calculateTotal();
    }
    
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public LocalDateTime getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDateTime deliveryDate) { this.deliveryDate = deliveryDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    // Business methods
    public void addItem(String productId, String productName, int quantity, double price) {
        OrderItem item = new OrderItem(productId, productName, quantity, price);
        items.add(item);
        calculateTotal();
    }
    
    public void removeItem(String productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
        calculateTotal();
    }
    
    public void updateItemQuantity(String productId, int newQuantity) {
        for (OrderItem item : items) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(newQuantity);
                calculateTotal();
                break;
            }
        }
    }
    
    private void calculateTotal() {
        this.total = items.stream()
            .mapToDouble(OrderItem::getSubtotal)
            .sum();
        this.updatedAt = LocalDateTime.now();
    }
    
    public int getTotalItems() {
        return items.stream()
            .mapToInt(OrderItem::getQuantity)
            .sum();
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public boolean isDelivered() {
        return status == OrderStatus.DELIVERED;
    }
    
    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }
    
    public boolean canBeCancelled() {
        return status == OrderStatus.PLACED || status == OrderStatus.PREPARING;
    }
    
    public void cancel() {
        if (canBeCancelled()) {
            setStatus(OrderStatus.CANCELLED);
        } else {
            throw new IllegalStateException("Order cannot be cancelled in current status: " + status);
        }
    }
    
    public String getFormattedTotal() {
        return String.format("$%.2f", total);
    }
    
    public String getStatusDisplayName() {
        return status.getDisplayName();
    }
    
    @Override
    public String toString() {
        return String.format("Order{id='%s', customer='%s', total=%.2f, status=%s, items=%d}", 
            id, customerName, total, status, items.size());
    }
}
