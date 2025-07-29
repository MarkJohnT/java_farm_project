package com.example.api;

import com.example.models.*;
import com.example.services.PaymentGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST API Service for third-party integrations
 * Provides API endpoints for mobile apps and external services
 */
public class RestApiService {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // API Response wrapper
    public static class ApiResponse<T> {
        private final boolean success;
        private final String message;
        private final T data;
        private final Map<String, Object> metadata;
        private final String timestamp;
        
        public ApiResponse(boolean success, String message, T data, Map<String, Object> metadata) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
        public Map<String, Object> getMetadata() { return metadata; }
        public String getTimestamp() { return timestamp; }
        
        public String toJson() {
            try {
                return objectMapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "{\"success\":false,\"message\":\"JSON serialization error\"}";
            }
        }
    }
    
    // Authentication token management
    public static class AuthToken {
        private final String token;
        private final String userId;
        private final String userType;
        private final LocalDateTime expiresAt;
        private final Set<String> permissions;
        
        public AuthToken(String token, String userId, String userType, LocalDateTime expiresAt, Set<String> permissions) {
            this.token = token;
            this.userId = userId;
            this.userType = userType;
            this.expiresAt = expiresAt;
            this.permissions = permissions;
        }
        
        public String getToken() { return token; }
        public String getUserId() { return userId; }
        public String getUserType() { return userType; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public Set<String> getPermissions() { return permissions; }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        public boolean hasPermission(String permission) {
            return permissions.contains(permission) || permissions.contains("*");
        }
    }
    
    // API endpoints simulation
    
    /**
     * POST /api/auth/login
     * User authentication endpoint
     */
    public static ApiResponse<Map<String, Object>> authenticateUser(String username, String password) {
        try {
            // Simulate user authentication
            // In real implementation, verify against database
            
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                return new ApiResponse<>(false, "Username and password are required", null, null);
            }
            
            // Mock user lookup
            User user = findUserByUsername(username);
            if (user == null || !user.checkPassword(password)) {
                return new ApiResponse<>(false, "Invalid credentials", null, null);
            }
            
            if (user.isAccountLocked()) {
                return new ApiResponse<>(false, "Account is locked", null, 
                    Map.of("lockoutUntil", user.getLockoutUntil().toString()));
            }
            
            // Generate auth token
            AuthToken authToken = generateAuthToken(user);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", authToken.getToken());
            responseData.put("userId", user.getId());
            responseData.put("userType", user.getUserType());
            responseData.put("fullName", user.getFullName());
            responseData.put("email", user.getEmail());
            responseData.put("expiresAt", authToken.getExpiresAt().toString());
            responseData.put("permissions", authToken.getPermissions());
            
            return new ApiResponse<>(true, "Authentication successful", responseData, null);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Authentication failed: " + e.getMessage(), null, null);
        }
    }
    
    /**
     * GET /api/products
     * Get products with filtering and pagination
     */
    public static ApiResponse<Map<String, Object>> getProducts(
            Map<String, String> filters, 
            int page, 
            int size, 
            String sortBy, 
            String sortOrder) {
        
        try {
            // Simulate product filtering and pagination
            List<Product> allProducts = getAllProducts(); // Mock method
            List<Product> filteredProducts = filterProducts(allProducts, filters);
            
            // Sort products
            filteredProducts = sortProducts(filteredProducts, sortBy, sortOrder);
            
            // Paginate
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, filteredProducts.size());
            List<Product> paginatedProducts = filteredProducts.subList(startIndex, endIndex);
            
            // Convert to API format
            List<Map<String, Object>> productData = new ArrayList<>();
            for (Product product : paginatedProducts) {
                productData.add(productToMap(product));
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("products", productData);
            responseData.put("totalCount", filteredProducts.size());
            responseData.put("page", page);
            responseData.put("size", size);
            responseData.put("totalPages", (int) Math.ceil((double) filteredProducts.size() / size));
            
            return new ApiResponse<>(true, "Products retrieved successfully", responseData, null);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Failed to retrieve products: " + e.getMessage(), null, null);
        }
    }
    
    /**
     * POST /api/orders
     * Create a new order
     */
    public static CompletableFuture<ApiResponse<Map<String, Object>>> createOrder(
            String authToken, 
            Map<String, Object> orderData) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                AuthToken token = validateToken(authToken);
                if (token == null || token.isExpired()) {
                    return new ApiResponse<>(false, "Invalid or expired token", null, null);
                }
                
                if (!token.hasPermission("create_order")) {
                    return new ApiResponse<>(false, "Insufficient permissions", null, null);
                }
                
                // Parse order data
                String customerId = token.getUserId();
                List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
                String deliveryAddress = (String) orderData.get("deliveryAddress");
                Map<String, Object> paymentMethodData = (Map<String, Object>) orderData.get("paymentMethod");
                
                // Validate order data
                if (items == null || items.isEmpty()) {
                    return new ApiResponse<>(false, "Order items are required", null, null);
                }
                
                // Calculate order total
                double total = calculateOrderTotal(items);
                
                // Process payment
                PaymentMethod paymentMethod = mapToPaymentMethod(paymentMethodData);
                PaymentGatewayService.PaymentResponse paymentResponse = 
                    PaymentGatewayService.processPayment(
                        paymentMethod, total, "USD", "Order payment", 
                        Map.of("customerId", customerId)
                    ).join();
                
                if (!paymentResponse.isSuccess()) {
                    return new ApiResponse<>(false, "Payment failed: " + paymentResponse.getMessage(), null, null);
                }
                
                // Create order
                String orderId = generateOrderId();
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("orderId", orderId);
                responseData.put("customerId", customerId);
                responseData.put("total", total);
                responseData.put("status", "CONFIRMED");
                responseData.put("paymentStatus", "PAID");
                responseData.put("transactionId", paymentResponse.getTransactionId());
                responseData.put("estimatedDelivery", LocalDateTime.now().plusDays(2).toString());
                
                return new ApiResponse<>(true, "Order created successfully", responseData, null);
                
            } catch (Exception e) {
                return new ApiResponse<>(false, "Failed to create order: " + e.getMessage(), null, null);
            }
        });
    }
    
    /**
     * GET /api/orders/{orderId}/track
     * Track order status
     */
    public static ApiResponse<Map<String, Object>> trackOrder(String authToken, String orderId) {
        try {
            AuthToken token = validateToken(authToken);
            if (token == null || token.isExpired()) {
                return new ApiResponse<>(false, "Invalid or expired token", null, null);
            }
            
            // Mock order tracking data
            Map<String, Object> trackingData = new HashMap<>();
            trackingData.put("orderId", orderId);
            trackingData.put("status", "IN_TRANSIT");
            trackingData.put("currentLocation", "Distribution Center - Downtown");
            trackingData.put("estimatedDelivery", LocalDateTime.now().plusHours(4).toString());
            
            List<Map<String, Object>> trackingHistory = new ArrayList<>();
            trackingHistory.add(Map.of(
                "status", "ORDER_PLACED",
                "timestamp", LocalDateTime.now().minusDays(1).toString(),
                "message", "Order confirmed and payment processed"
            ));
            trackingHistory.add(Map.of(
                "status", "PREPARING",
                "timestamp", LocalDateTime.now().minusHours(8).toString(),
                "message", "Farmer is preparing your order"
            ));
            trackingHistory.add(Map.of(
                "status", "PICKED_UP",
                "timestamp", LocalDateTime.now().minusHours(2).toString(),
                "message", "Order picked up for delivery"
            ));
            trackingHistory.add(Map.of(
                "status", "IN_TRANSIT",
                "timestamp", LocalDateTime.now().minusMinutes(30).toString(),
                "message", "Out for delivery"
            ));
            
            trackingData.put("history", trackingHistory);
            
            return new ApiResponse<>(true, "Tracking information retrieved", trackingData, null);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Failed to track order: " + e.getMessage(), null, null);
        }
    }
    
    /**
     * POST /api/webhooks/payment
     * Handle payment webhook notifications
     */
    public static ApiResponse<Map<String, Object>> handlePaymentWebhook(String payload, String signature) {
        try {
            // Verify webhook signature (in real implementation)
            if (!verifyWebhookSignature(payload, signature)) {
                return new ApiResponse<>(false, "Invalid webhook signature", null, null);
            }
            
            // Parse webhook payload
            JsonNode webhookData = objectMapper.readTree(payload);
            String eventType = webhookData.get("type").asText();
            String paymentId = webhookData.get("data").get("object").get("id").asText();
            
            // Handle different webhook events
            switch (eventType) {
                case "payment_intent.succeeded":
                    // Update order status to paid
                    updateOrderPaymentStatus(paymentId, "PAID");
                    break;
                case "payment_intent.payment_failed":
                    // Handle failed payment
                    updateOrderPaymentStatus(paymentId, "FAILED");
                    break;
                case "charge.dispute.created":
                    // Handle chargeback
                    handleChargeback(paymentId);
                    break;
            }
            
            return new ApiResponse<>(true, "Webhook processed successfully", 
                Map.of("eventType", eventType, "paymentId", paymentId), null);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Webhook processing failed: " + e.getMessage(), null, null);
        }
    }
    
    // Helper methods
    
    private static User findUserByUsername(String username) {
        // Mock user lookup - in real implementation, query database
        if ("farmer1".equals(username)) {
            return new Farmer("John Doe", "farmer1", "john@farm.com", "+1234567890", "Green Valley Farm", "California", "password123");
        } else if ("customer1".equals(username)) {
            return new Customer("Jane Smith", "customer1", "jane@email.com", "+0987654321", "New York", "password123");
        }
        return null;
    }
    
    private static AuthToken generateAuthToken(User user) {
        String token = "Bearer_" + Base64.getEncoder().encodeToString(
            (user.getId() + ":" + System.currentTimeMillis()).getBytes()
        );
        
        Set<String> permissions = new HashSet<>();
        if (user instanceof Farmer) {
            permissions.addAll(Arrays.asList("create_product", "update_product", "view_orders", "update_order_status"));
        } else if (user instanceof Customer) {
            permissions.addAll(Arrays.asList("create_order", "view_products", "track_order"));
        }
        
        return new AuthToken(token, user.getId(), user.getUserType(), 
            LocalDateTime.now().plusHours(24), permissions);
    }
    
    private static AuthToken validateToken(String token) {
        // Mock token validation - in real implementation, validate against database/cache
        if (token != null && token.startsWith("Bearer_")) {
            // Simple mock validation
            return new AuthToken(token, "user123", "Customer", 
                LocalDateTime.now().plusHours(1), Set.of("create_order", "view_products"));
        }
        return null;
    }
    
    private static List<Product> getAllProducts() {
        // Mock product list
        List<Product> products = new ArrayList<>();
        products.add(new Product("Fresh Tomatoes", 3.99, "Organic vine-ripened tomatoes", "lb", 100, "farmer1", "Vegetables", true));
        products.add(new Product("Farm Eggs", 4.50, "Free-range chicken eggs", "dozen", 50, "farmer1", "Dairy", true));
        return products;
    }
    
    private static List<Product> filterProducts(List<Product> products, Map<String, String> filters) {
        // Apply filters (category, price range, organic, etc.)
        return products.stream()
            .filter(p -> filters.get("category") == null || p.getCategory().equals(filters.get("category")))
            .filter(p -> filters.get("organic") == null || p.isOrganic() == Boolean.parseBoolean(filters.get("organic")))
            .collect(java.util.stream.Collectors.toList());
    }
    
    private static List<Product> sortProducts(List<Product> products, String sortBy, String sortOrder) {
        if ("price".equals(sortBy)) {
            products.sort((a, b) -> "desc".equals(sortOrder) ? 
                Double.compare(b.getPrice(), a.getPrice()) : 
                Double.compare(a.getPrice(), b.getPrice()));
        } else if ("name".equals(sortBy)) {
            products.sort((a, b) -> "desc".equals(sortOrder) ? 
                b.getName().compareTo(a.getName()) : 
                a.getName().compareTo(b.getName()));
        }
        return products;
    }
    
    private static Map<String, Object> productToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("price", product.getPrice());
        map.put("description", product.getDescription());
        map.put("unit", product.getUnit());
        map.put("quantity", product.getQuantity());
        map.put("category", product.getCategory());
        map.put("isOrganic", product.isOrganic());
        map.put("farmerId", product.getFarmerId());
        map.put("imagePath", product.getImagePath());
        return map;
    }
    
    private static double calculateOrderTotal(List<Map<String, Object>> items) {
        return items.stream()
            .mapToDouble(item -> ((Number) item.get("price")).doubleValue() * ((Number) item.get("quantity")).intValue())
            .sum();
    }
    
    private static PaymentMethod mapToPaymentMethod(Map<String, Object> data) {
        String type = (String) data.get("type");
        PaymentMethod.PaymentType paymentType = PaymentMethod.PaymentType.valueOf(type.toUpperCase());
        
        String userId = (String) data.getOrDefault("userId", "user123");
        
        // Create payment method based on type
        if (paymentType == PaymentMethod.PaymentType.CREDIT_CARD || 
            paymentType == PaymentMethod.PaymentType.DEBIT_CARD) {
            
            String cardNumber = (String) data.get("cardNumber");
            String maskedCardNumber = "•••• •••• •••• " + cardNumber.substring(cardNumber.length() - 4);
            String cardType = (String) data.getOrDefault("cardType", "VISA");
            String expiryDate = (String) data.get("expiryDate");
            String[] expiryParts = expiryDate.split("/");
            String expiryMonth = expiryParts[0];
            String expiryYear = expiryParts[1];
            String cardHolderName = (String) data.getOrDefault("cardHolderName", "");
            
            return new PaymentMethod(userId, paymentType, cardHolderName,
                maskedCardNumber, cardType, expiryMonth, expiryYear);
                
        } else if (paymentType == PaymentMethod.PaymentType.PAYPAL) {
            String paypalToken = (String) data.get("paypalToken");
            return new PaymentMethod(userId, paymentType, "PayPal", paypalToken);
            
        } else {
            return new PaymentMethod(userId, paymentType, paymentType.getDisplayName());
        }
    }
    
    private static String generateOrderId() {
        return "ORD_" + System.currentTimeMillis();
    }
    
    private static boolean verifyWebhookSignature(String payload, String signature) {
        // In real implementation, verify webhook signature using provider's secret
        return signature != null && !signature.isEmpty();
    }
    
    private static void updateOrderPaymentStatus(String paymentId, String status) {
        // Update order in database
        System.out.println("Updated payment " + paymentId + " status to " + status);
    }
    
    private static void handleChargeback(String paymentId) {
        // Handle chargeback logic
        System.out.println("Handling chargeback for payment " + paymentId);
    }
}
