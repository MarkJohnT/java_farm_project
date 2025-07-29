package com.example.services;

import com.example.models.PaymentMethod;
import com.example.models.Transaction;
import com.example.models.Transaction.TransactionStatus;
import com.example.models.Transaction.TransactionType;
import com.example.models.Transaction.TransactionItem;
import com.example.models.Transaction.PaymentDetails;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service class for handling payment operations.
 * Manages payment methods, processes transactions, and handles payment security.
 */
public class PaymentService {
    private static final String DB_URL = "jdbc:h2:./agro_db";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    // Tax rate (8.5%)
    private static final BigDecimal TAX_RATE = new BigDecimal("0.085");
    
    // Shipping rates
    private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("5.99");
    private static final BigDecimal EXPRESS_SHIPPING = new BigDecimal("12.99");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");
    
    public PaymentService() {
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        String createPaymentMethodsTable = """
            CREATE TABLE IF NOT EXISTS payment_methods (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                type VARCHAR(20) NOT NULL,
                display_name VARCHAR(100) NOT NULL,
                encrypted_data TEXT,
                is_default BOOLEAN DEFAULT FALSE,
                is_active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP NOT NULL,
                last_updated TIMESTAMP NOT NULL,
                last_used TIMESTAMP,
                card_holder_name VARCHAR(100),
                masked_card_number VARCHAR(20),
                card_type VARCHAR(20),
                expiry_month VARCHAR(2),
                expiry_year VARCHAR(4),
                wallet_provider VARCHAR(50),
                wallet_account_id VARCHAR(100),
                bank_name VARCHAR(100),
                account_holder_name VARCHAR(100),
                masked_account_number VARCHAR(20)
            )
        """;
        
        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id VARCHAR(36) PRIMARY KEY,
                order_id VARCHAR(36) NOT NULL,
                user_id VARCHAR(36) NOT NULL,
                payment_method_id VARCHAR(36),
                type VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount DECIMAL(10,2) NOT NULL,
                tax_amount DECIMAL(10,2),
                shipping_amount DECIMAL(10,2),
                discount_amount DECIMAL(10,2),
                total_amount DECIMAL(10,2) NOT NULL,
                currency VARCHAR(3) DEFAULT 'USD',
                description TEXT,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                processed_at TIMESTAMP,
                processor_transaction_id VARCHAR(100),
                processor_response TEXT,
                failure_reason TEXT,
                retry_count INTEGER DEFAULT 0,
                refund_reason TEXT,
                refund_transaction_id VARCHAR(36)
            )
        """;
        
        String createTransactionItemsTable = """
            CREATE TABLE IF NOT EXISTS transaction_items (
                id VARCHAR(36) PRIMARY KEY,
                transaction_id VARCHAR(36) NOT NULL,
                product_id VARCHAR(36) NOT NULL,
                product_name VARCHAR(100) NOT NULL,
                quantity INTEGER NOT NULL,
                unit_price DECIMAL(10,2) NOT NULL,
                total_price DECIMAL(10,2) NOT NULL,
                category VARCHAR(50),
                FOREIGN KEY (transaction_id) REFERENCES transactions(id)
            )
        """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.createStatement().execute(createPaymentMethodsTable);
            conn.createStatement().execute(createTransactionsTable);
            conn.createStatement().execute(createTransactionItemsTable);
        } catch (SQLException e) {
            System.err.println("Error initializing payment database: " + e.getMessage());
        }
    }
    
    // Payment Method Management
    public void savePaymentMethod(PaymentMethod paymentMethod) throws SQLException {
        String sql = """
            INSERT INTO payment_methods 
            (id, user_id, type, display_name, encrypted_data, is_default, is_active, 
             created_at, last_updated, last_used, card_holder_name, masked_card_number, 
             card_type, expiry_month, expiry_year, wallet_provider, wallet_account_id, 
             bank_name, account_holder_name, masked_account_number)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, paymentMethod.getId());
            stmt.setString(2, paymentMethod.getUserId());
            stmt.setString(3, paymentMethod.getType().name());
            stmt.setString(4, paymentMethod.getDisplayName());
            stmt.setString(5, paymentMethod.getEncryptedData());
            stmt.setBoolean(6, paymentMethod.isDefault());
            stmt.setBoolean(7, paymentMethod.isActive());
            stmt.setTimestamp(8, Timestamp.valueOf(paymentMethod.getCreatedAt()));
            stmt.setTimestamp(9, Timestamp.valueOf(paymentMethod.getLastUpdated()));
            stmt.setTimestamp(10, paymentMethod.getLastUsed() != null ? 
                             Timestamp.valueOf(paymentMethod.getLastUsed()) : null);
            stmt.setString(11, paymentMethod.getCardHolderName());
            stmt.setString(12, paymentMethod.getMaskedCardNumber());
            stmt.setString(13, paymentMethod.getCardType());
            stmt.setString(14, paymentMethod.getExpiryMonth());
            stmt.setString(15, paymentMethod.getExpiryYear());
            stmt.setString(16, paymentMethod.getWalletProvider());
            stmt.setString(17, paymentMethod.getWalletAccountId());
            stmt.setString(18, paymentMethod.getBankName());
            stmt.setString(19, paymentMethod.getAccountHolderName());
            stmt.setString(20, paymentMethod.getMaskedAccountNumber());
            
            stmt.executeUpdate();
        }
    }
    
    public List<PaymentMethod> getPaymentMethodsByUserId(String userId) {
        List<PaymentMethod> methods = new ArrayList<>();
        String sql = "SELECT * FROM payment_methods WHERE user_id = ? AND is_active = TRUE ORDER BY is_default DESC, last_used DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PaymentMethod method = createPaymentMethodFromResultSet(rs);
                methods.add(method);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching payment methods: " + e.getMessage());
        }
        
        return methods;
    }
    
    public Optional<PaymentMethod> getPaymentMethodById(String id) {
        String sql = "SELECT * FROM payment_methods WHERE id = ? AND is_active = TRUE";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(createPaymentMethodFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching payment method: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    public void setDefaultPaymentMethod(String userId, String paymentMethodId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);
            
            // Clear existing default
            String clearDefault = "UPDATE payment_methods SET is_default = FALSE WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(clearDefault)) {
                stmt.setString(1, userId);
                stmt.executeUpdate();
            }
            
            // Set new default
            String setDefault = "UPDATE payment_methods SET is_default = TRUE, last_updated = ? WHERE id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(setDefault)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(2, paymentMethodId);
                stmt.setString(3, userId);
                stmt.executeUpdate();
            }
            
            conn.commit();
        }
    }
    
    public void deletePaymentMethod(String id) throws SQLException {
        String sql = "UPDATE payment_methods SET is_active = FALSE, last_updated = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, id);
            stmt.executeUpdate();
        }
    }
    
    // Transaction Processing
    public CompletableFuture<Transaction> processPayment(Transaction transaction) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate payment method
                Optional<PaymentMethod> paymentMethod = getPaymentMethodById(transaction.getPaymentMethodId());
                if (paymentMethod.isEmpty()) {
                    transaction.setStatus(TransactionStatus.FAILED);
                    transaction.setFailureReason("Invalid payment method");
                    return transaction;
                }
                
                // Check if payment method is expired
                if (paymentMethod.get().isExpired()) {
                    transaction.setStatus(TransactionStatus.FAILED);
                    transaction.setFailureReason("Payment method has expired");
                    return transaction;
                }
                
                // Set status to processing
                transaction.setStatus(TransactionStatus.PROCESSING);
                saveTransaction(transaction);
                
                // Simulate payment processing
                Thread.sleep(2000 + ThreadLocalRandom.current().nextInt(3000)); // 2-5 seconds
                
                // Simulate payment gateway response
                boolean paymentSuccess = simulatePaymentGateway(transaction, paymentMethod.get());
                
                if (paymentSuccess) {
                    transaction.setStatus(TransactionStatus.COMPLETED);
                    transaction.setProcessorTransactionId("TXN_" + System.currentTimeMillis());
                    
                    // Update payment details
                    PaymentDetails details = transaction.getPaymentDetails();
                    details.setProcessorName("AgroPayments Gateway");
                    details.setAuthorizationCode("AUTH_" + ThreadLocalRandom.current().nextInt(100000, 999999));
                    details.setAuthorizationTime(LocalDateTime.now());
                    details.setGatewayResponse("SUCCESS");
                    
                    if (paymentMethod.get().getType() == PaymentMethod.PaymentType.CREDIT_CARD ||
                        paymentMethod.get().getType() == PaymentMethod.PaymentType.DEBIT_CARD) {
                        details.setCardLast4(paymentMethod.get().getMaskedCardNumber().replaceAll(".*", "").substring(0, 4));
                        details.setCardType(paymentMethod.get().getCardType());
                    }
                    
                    // Mark payment method as used
                    paymentMethod.get().markAsUsed();
                    updatePaymentMethodUsage(paymentMethod.get());
                    
                } else {
                    transaction.setStatus(TransactionStatus.FAILED);
                    transaction.setFailureReason("Payment declined by processor");
                }
                
                saveTransaction(transaction);
                return transaction;
                
            } catch (Exception e) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("System error: " + e.getMessage());
                try {
                    saveTransaction(transaction);
                } catch (SQLException sqlEx) {
                    System.err.println("Error saving failed transaction: " + sqlEx.getMessage());
                }
                return transaction;
            }
        });
    }
    
    private boolean simulatePaymentGateway(Transaction transaction, PaymentMethod paymentMethod) {
        // Simulate different success rates for different payment methods
        double successRate;
        switch (paymentMethod.getType()) {
            case CREDIT_CARD:
            case DEBIT_CARD:
                successRate = 0.92; // 92% success
                break;
            case PAYPAL:
            case APPLE_PAY:
            case GOOGLE_PAY:
                successRate = 0.96; // 96% success
                break;
            case BANK_TRANSFER:
                successRate = 0.88; // 88% success
                break;
            case CASH_ON_DELIVERY:
                successRate = 0.99; // 99% success (almost always works)
                break;
            default:
                successRate = 0.85;
        }
        
        // Additional validation checks
        if (transaction.getTotalAmount().compareTo(new BigDecimal("10000")) > 0) {
            successRate *= 0.8; // Reduce success rate for large amounts
        }
        
        return ThreadLocalRandom.current().nextDouble() < successRate;
    }
    
    public Transaction calculateTransactionAmounts(Transaction transaction, boolean expressShipping) {
        BigDecimal subtotal = transaction.getAmount();
        
        // Calculate tax
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        transaction.setTaxAmount(tax);
        
        // Calculate shipping
        BigDecimal shipping;
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            shipping = BigDecimal.ZERO; // Free shipping
        } else {
            shipping = expressShipping ? EXPRESS_SHIPPING : STANDARD_SHIPPING;
        }
        transaction.setShippingAmount(shipping);
        
        return transaction;
    }
    
    public void saveTransaction(Transaction transaction) throws SQLException {
        String sql = """
            MERGE INTO transactions 
            (id, order_id, user_id, payment_method_id, type, status, amount, tax_amount, 
             shipping_amount, discount_amount, total_amount, currency, description, 
             created_at, updated_at, processed_at, processor_transaction_id, processor_response, 
             failure_reason, retry_count, refund_reason, refund_transaction_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, transaction.getId());
            stmt.setString(2, transaction.getOrderId());
            stmt.setString(3, transaction.getUserId());
            stmt.setString(4, transaction.getPaymentMethodId());
            stmt.setString(5, transaction.getType().name());
            stmt.setString(6, transaction.getStatus().name());
            stmt.setBigDecimal(7, transaction.getAmount());
            stmt.setBigDecimal(8, transaction.getTaxAmount());
            stmt.setBigDecimal(9, transaction.getShippingAmount());
            stmt.setBigDecimal(10, transaction.getDiscountAmount());
            stmt.setBigDecimal(11, transaction.getTotalAmount());
            stmt.setString(12, transaction.getCurrency());
            stmt.setString(13, transaction.getDescription());
            stmt.setTimestamp(14, Timestamp.valueOf(transaction.getCreatedAt()));
            stmt.setTimestamp(15, Timestamp.valueOf(transaction.getUpdatedAt()));
            stmt.setTimestamp(16, transaction.getProcessedAt() != null ? 
                             Timestamp.valueOf(transaction.getProcessedAt()) : null);
            stmt.setString(17, transaction.getProcessorTransactionId());
            stmt.setString(18, transaction.getProcessorResponse());
            stmt.setString(19, transaction.getFailureReason());
            stmt.setInt(20, transaction.getRetryCount());
            stmt.setString(21, transaction.getRefundReason());
            stmt.setString(22, transaction.getRefundTransactionId());
            
            stmt.executeUpdate();
            
            // Save transaction items
            saveTransactionItems(transaction);
        }
    }
    
    private void saveTransactionItems(Transaction transaction) throws SQLException {
        // First delete existing items
        String deleteSql = "DELETE FROM transaction_items WHERE transaction_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, transaction.getId());
            stmt.executeUpdate();
        }
        
        // Insert new items
        String insertSql = """
            INSERT INTO transaction_items 
            (id, transaction_id, product_id, product_name, quantity, unit_price, total_price, category)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            
            for (TransactionItem item : transaction.getItems()) {
                stmt.setString(1, java.util.UUID.randomUUID().toString());
                stmt.setString(2, transaction.getId());
                stmt.setString(3, item.getProductId());
                stmt.setString(4, item.getProductName());
                stmt.setInt(5, item.getQuantity());
                stmt.setBigDecimal(6, item.getUnitPrice());
                stmt.setBigDecimal(7, item.getTotalPrice());
                stmt.setString(8, item.getCategory());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        }
    }
    
    public List<Transaction> getTransactionsByUserId(String userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Transaction transaction = createTransactionFromResultSet(rs);
                loadTransactionItems(transaction);
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching transactions: " + e.getMessage());
        }
        
        return transactions;
    }
    
    private void updatePaymentMethodUsage(PaymentMethod paymentMethod) throws SQLException {
        String sql = "UPDATE payment_methods SET last_used = ?, last_updated = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(paymentMethod.getLastUsed()));
            stmt.setTimestamp(2, Timestamp.valueOf(paymentMethod.getLastUpdated()));
            stmt.setString(3, paymentMethod.getId());
            stmt.executeUpdate();
        }
    }
    
    private PaymentMethod createPaymentMethodFromResultSet(ResultSet rs) throws SQLException {
        PaymentMethod.PaymentType type = PaymentMethod.PaymentType.valueOf(rs.getString("type"));
        PaymentMethod method = new PaymentMethod(rs.getString("user_id"), type, rs.getString("display_name"));
        
        // Set common fields
        method.setEncryptedData(rs.getString("encrypted_data"));
        method.setDefault(rs.getBoolean("is_default"));
        method.setActive(rs.getBoolean("is_active"));
        
        // Set type-specific fields
        method.setCardHolderName(rs.getString("card_holder_name"));
        method.setMaskedCardNumber(rs.getString("masked_card_number"));
        method.setCardType(rs.getString("card_type"));
        method.setExpiryMonth(rs.getString("expiry_month"));
        method.setExpiryYear(rs.getString("expiry_year"));
        method.setWalletProvider(rs.getString("wallet_provider"));
        method.setWalletAccountId(rs.getString("wallet_account_id"));
        method.setBankName(rs.getString("bank_name"));
        method.setAccountHolderName(rs.getString("account_holder_name"));
        method.setMaskedAccountNumber(rs.getString("masked_account_number"));
        
        return method;
    }
    
    private Transaction createTransactionFromResultSet(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction(
            rs.getString("order_id"),
            rs.getString("user_id"),
            rs.getString("payment_method_id"),
            TransactionType.valueOf(rs.getString("type")),
            rs.getBigDecimal("amount"),
            rs.getString("currency")
        );
        
        transaction.setStatus(TransactionStatus.valueOf(rs.getString("status")));
        transaction.setTaxAmount(rs.getBigDecimal("tax_amount"));
        transaction.setShippingAmount(rs.getBigDecimal("shipping_amount"));
        transaction.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        transaction.setDescription(rs.getString("description"));
        transaction.setProcessorTransactionId(rs.getString("processor_transaction_id"));
        transaction.setProcessorResponse(rs.getString("processor_response"));
        transaction.setFailureReason(rs.getString("failure_reason"));
        transaction.setRefundReason(rs.getString("refund_reason"));
        transaction.setRefundTransactionId(rs.getString("refund_transaction_id"));
        
        return transaction;
    }
    
    private void loadTransactionItems(Transaction transaction) throws SQLException {
        String sql = "SELECT * FROM transaction_items WHERE transaction_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, transaction.getId());
            ResultSet rs = stmt.executeQuery();
            
            List<TransactionItem> items = new ArrayList<>();
            while (rs.next()) {
                TransactionItem item = new TransactionItem(
                    rs.getString("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("unit_price")
                );
                item.setCategory(rs.getString("category"));
                items.add(item);
            }
            
            transaction.setItems(items);
        }
    }
    
    // Utility methods
    public BigDecimal getTaxRate() { return TAX_RATE; }
    public BigDecimal getStandardShipping() { return STANDARD_SHIPPING; }
    public BigDecimal getExpressShipping() { return EXPRESS_SHIPPING; }
    public BigDecimal getFreeShippingThreshold() { return FREE_SHIPPING_THRESHOLD; }
}
