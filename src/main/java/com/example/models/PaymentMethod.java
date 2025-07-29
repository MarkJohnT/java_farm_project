package com.example.models;

import java.time.LocalDateTime;

/**
 * Represents a saved payment method for a user.
 * Supports credit cards, debit cards, digital wallets, and bank transfers.
 */
public class PaymentMethod {
    private String id;
    private String userId;
    private PaymentType type;
    private String displayName; // e.g., "•••• 1234" or "PayPal Account"
    private String encryptedData; // Encrypted payment details
    private boolean isDefault;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private LocalDateTime lastUsed;
    
    // Card-specific fields (encrypted/tokenized)
    private String cardHolderName;
    private String maskedCardNumber; // e.g., "•••• •••• •••• 1234"
    private String cardType; // VISA, MASTERCARD, AMEX, etc.
    private String expiryMonth;
    private String expiryYear;
    
    // Digital wallet fields
    private String walletProvider; // PayPal, Apple Pay, Google Pay
    private String walletAccountId;
    
    // Bank transfer fields
    private String bankName;
    private String accountHolderName;
    private String maskedAccountNumber;
    
    public enum PaymentType {
        CREDIT_CARD("Credit Card"),
        DEBIT_CARD("Debit Card"),
        PAYPAL("PayPal"),
        APPLE_PAY("Apple Pay"),
        GOOGLE_PAY("Google Pay"),
        BANK_TRANSFER("Bank Transfer"),
        CASH_ON_DELIVERY("Cash on Delivery");
        
        private final String displayName;
        
        PaymentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public PaymentMethod(String userId, PaymentType type, String displayName) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.displayName = displayName;
        this.isDefault = false;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Constructor for credit/debit cards
    public PaymentMethod(String userId, PaymentType type, String cardHolderName, 
                        String maskedCardNumber, String cardType, String expiryMonth, String expiryYear) {
        this(userId, type, cardType + " " + maskedCardNumber);
        this.cardHolderName = cardHolderName;
        this.maskedCardNumber = maskedCardNumber;
        this.cardType = cardType;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
    }
    
    // Constructor for digital wallets
    public PaymentMethod(String userId, PaymentType type, String walletProvider, String walletAccountId) {
        this(userId, type, walletProvider + " Account");
        this.walletProvider = walletProvider;
        this.walletAccountId = walletAccountId;
    }
    
    // Getters and setters
    public String getId() { return id; }
    
    public String getUserId() { return userId; }
    
    public PaymentType getType() { return type; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { 
        this.displayName = displayName;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getEncryptedData() { return encryptedData; }
    public void setEncryptedData(String encryptedData) { 
        this.encryptedData = encryptedData;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { 
        this.isDefault = isDefault;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { 
        this.isActive = active;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public LocalDateTime getLastUsed() { return lastUsed; }
    
    public void markAsUsed() {
        this.lastUsed = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Card-specific getters and setters
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { 
        this.cardHolderName = cardHolderName;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getMaskedCardNumber() { return maskedCardNumber; }
    public void setMaskedCardNumber(String maskedCardNumber) { 
        this.maskedCardNumber = maskedCardNumber;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { 
        this.cardType = cardType;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String expiryMonth) { 
        this.expiryMonth = expiryMonth;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getExpiryYear() { return expiryYear; }
    public void setExpiryYear(String expiryYear) { 
        this.expiryYear = expiryYear;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Digital wallet getters and setters
    public String getWalletProvider() { return walletProvider; }
    public void setWalletProvider(String walletProvider) { 
        this.walletProvider = walletProvider;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getWalletAccountId() { return walletAccountId; }
    public void setWalletAccountId(String walletAccountId) { 
        this.walletAccountId = walletAccountId;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Bank transfer getters and setters
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { 
        this.bankName = bankName;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { 
        this.accountHolderName = accountHolderName;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { 
        this.maskedAccountNumber = maskedAccountNumber;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Utility methods
    public boolean isExpired() {
        if (type == PaymentType.CREDIT_CARD || type == PaymentType.DEBIT_CARD) {
            if (expiryMonth == null || expiryYear == null) return false;
            
            try {
                int expMonth = Integer.parseInt(expiryMonth);
                int expYear = Integer.parseInt(expiryYear);
                
                LocalDateTime now = LocalDateTime.now();
                int currentMonth = now.getMonthValue();
                int currentYear = now.getYear();
                
                return (expYear < currentYear) || (expYear == currentYear && expMonth < currentMonth);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
    
    public String getFormattedExpiry() {
        if (expiryMonth != null && expiryYear != null) {
            return String.format("%s/%s", expiryMonth, expiryYear);
        }
        return null;
    }
    
    public String getSecurityInfo() {
        switch (type) {
            case CREDIT_CARD:
            case DEBIT_CARD:
                return "Card details are encrypted and tokenized";
            case PAYPAL:
                return "PayPal handles payment security";
            case APPLE_PAY:
                return "Apple Pay uses biometric authentication";
            case GOOGLE_PAY:
                return "Google Pay uses tokenized payments";
            case BANK_TRANSFER:
                return "Bank transfer details are encrypted";
            case CASH_ON_DELIVERY:
                return "Pay when you receive your order";
            default:
                return "Payment details are securely stored";
        }
    }
    
    @Override
    public String toString() {
        return String.format("PaymentMethod{id='%s', type=%s, displayName='%s', isDefault=%s, isExpired=%s}", 
                id, type, displayName, isDefault, isExpired());
    }
}
