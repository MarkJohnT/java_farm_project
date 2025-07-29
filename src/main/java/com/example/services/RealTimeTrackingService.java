package com.example.services;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Real-time Order Tracking Service
 * Provides live updates for order status and location tracking
 */
public class RealTimeTrackingService {
    
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final Map<String, TrackingSession> activeSessions = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    
    // Order status enumeration
    public enum OrderStatus {
        PLACED("Order Placed", "Your order has been confirmed"),
        PREPARING("Preparing", "Farmer is preparing your order"),
        PICKED_UP("Picked Up", "Order has been picked up for delivery"),
        IN_TRANSIT("In Transit", "Your order is on the way"),
        OUT_FOR_DELIVERY("Out for Delivery", "Driver is approaching your location"),
        DELIVERED("Delivered", "Order has been delivered successfully"),
        CANCELLED("Cancelled", "Order has been cancelled"),
        DELAYED("Delayed", "Delivery has been delayed");
        
        private final String displayName;
        private final String description;
        
        OrderStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // Location data for GPS tracking
    public static class LocationData {
        private final double latitude;
        private final double longitude;
        private final String address;
        private final LocalDateTime timestamp;
        
        public LocationData(double latitude, double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.timestamp = LocalDateTime.now();
        }
        
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getAddress() { return address; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    // Order tracking update
    public static class TrackingUpdate {
        private final String orderId;
        private final OrderStatus status;
        private final LocationData location;
        private final String message;
        private final LocalDateTime timestamp;
        private final Map<String, Object> metadata;
        
        public TrackingUpdate(String orderId, OrderStatus status, LocationData location, 
                            String message, Map<String, Object> metadata) {
            this.orderId = orderId;
            this.status = status;
            this.location = location;
            this.message = message;
            this.timestamp = LocalDateTime.now();
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        public String getOrderId() { return orderId; }
        public OrderStatus getStatus() { return status; }
        public LocationData getLocation() { return location; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    // Tracking session for managing real-time updates
    public static class TrackingSession {
        private final String orderId;
        private final String customerId;
        private final Consumer<TrackingUpdate> updateCallback;
        private final LocalDateTime startTime;
        private OrderStatus currentStatus;
        private LocationData lastKnownLocation;
        private ScheduledFuture<?> simulationTask;
        
        public TrackingSession(String orderId, String customerId, Consumer<TrackingUpdate> updateCallback) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.updateCallback = updateCallback;
            this.startTime = LocalDateTime.now();
            this.currentStatus = OrderStatus.PLACED;
        }
        
        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public OrderStatus getCurrentStatus() { return currentStatus; }
        public LocationData getLastKnownLocation() { return lastKnownLocation; }
        public LocalDateTime getStartTime() { return startTime; }
        
        public void updateStatus(OrderStatus status, LocationData location, String message, Map<String, Object> metadata) {
            this.currentStatus = status;
            if (location != null) {
                this.lastKnownLocation = location;
            }
            
            TrackingUpdate update = new TrackingUpdate(orderId, status, location, message, metadata);
            
            // Send update to UI thread
            Platform.runLater(() -> {
                try {
                    updateCallback.accept(update);
                } catch (Exception e) {
                    System.err.println("Error in tracking update callback: " + e.getMessage());
                }
            });
        }
        
        public void startSimulation() {
            // Simulate realistic order progression
            simulationTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    simulateOrderProgress();
                } catch (Exception e) {
                    System.err.println("Error in tracking simulation: " + e.getMessage());
                }
            }, 30, 60, TimeUnit.SECONDS); // Update every 60 seconds after initial 30 seconds
        }
        
        public void stopSimulation() {
            if (simulationTask != null && !simulationTask.isCancelled()) {
                simulationTask.cancel(false);
            }
        }
        
        private void simulateOrderProgress() {
            // Simulate realistic order status progression with locations
            switch (currentStatus) {
                case PLACED:
                    // After 2 minutes, move to preparing
                    if (LocalDateTime.now().isAfter(startTime.plusMinutes(2))) {
                        LocationData farmLocation = new LocationData(40.7128, -74.0060, "Green Valley Farm, California");
                        updateStatus(OrderStatus.PREPARING, farmLocation, 
                            "Farmer John is preparing your fresh produce order", 
                            Map.of("farmerName", "John Doe", "preparationTime", "15-20 minutes"));
                    }
                    break;
                    
                case PREPARING:
                    // After 15 minutes total, move to picked up
                    if (LocalDateTime.now().isAfter(startTime.plusMinutes(15))) {
                        LocationData pickupLocation = new LocationData(40.7589, -73.9851, "Farm Gate - Ready for Pickup");
                        updateStatus(OrderStatus.PICKED_UP, pickupLocation,
                            "Order has been picked up by our delivery partner",
                            Map.of("driverName", "Mike Johnson", "vehicleType", "Refrigerated Truck", "driverPhone", "+1-555-0123"));
                    }
                    break;
                    
                case PICKED_UP:
                    // After 25 minutes total, move to in transit
                    if (LocalDateTime.now().isAfter(startTime.plusMinutes(25))) {
                        LocationData transitLocation = new LocationData(40.7614, -73.9776, "Highway 101 - En Route");
                        updateStatus(OrderStatus.IN_TRANSIT, transitLocation,
                            "Your order is in transit to the distribution center",
                            Map.of("estimatedArrival", LocalDateTime.now().plusMinutes(30).format(DateTimeFormatter.ofPattern("HH:mm")),
                                   "currentSpeed", "45 mph", "distance", "12 miles"));
                    }
                    break;
                    
                case IN_TRANSIT:
                    // After 45 minutes total, move to out for delivery
                    if (LocalDateTime.now().isAfter(startTime.plusMinutes(45))) {
                        LocationData deliveryLocation = new LocationData(40.7505, -73.9934, "Downtown Distribution Center");
                        updateStatus(OrderStatus.OUT_FOR_DELIVERY, deliveryLocation,
                            "Your order is out for final delivery",
                            Map.of("deliveryWindow", "Next 30 minutes", "driverContact", "+1-555-0123",
                                   "trackingCode", "DLV" + System.currentTimeMillis()));
                    }
                    break;
                    
                case OUT_FOR_DELIVERY:
                    // After 60 minutes total, deliver
                    if (LocalDateTime.now().isAfter(startTime.plusMinutes(60))) {
                        LocationData deliveredLocation = new LocationData(40.7831, -73.9712, "123 Customer Street, New York");
                        updateStatus(OrderStatus.DELIVERED, deliveredLocation,
                            "Order delivered successfully! Thank you for your purchase.",
                            Map.of("deliveryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                                   "signature", "Electronic signature received", "rating", "Please rate your experience"));
                        stopSimulation(); // Stop updates after delivery
                    } else {
                        // Update location during delivery
                        double lat = 40.7505 + (Math.random() - 0.5) * 0.01; // Simulate movement
                        double lng = -73.9934 + (Math.random() - 0.5) * 0.01;
                        LocationData movingLocation = new LocationData(lat, lng, "Approaching delivery address");
                        updateStatus(OrderStatus.OUT_FOR_DELIVERY, movingLocation,
                            "Driver is approaching your location",
                            Map.of("estimatedArrival", "5-10 minutes", "driverContact", "+1-555-0123"));
                    }
                    break;
                    
                case DELIVERED:
                    // Order complete - no more updates
                    stopSimulation();
                    break;
                    
                case CANCELLED:
                    // Order cancelled - no more updates
                    stopSimulation();
                    break;
                    
                case DELAYED:
                    // Handle delayed orders - extend delivery time
                    updateStatus(OrderStatus.IN_TRANSIT, lastKnownLocation,
                        "Delivery has been delayed due to traffic conditions. New ETA provided.",
                        Map.of("delayReason", "Heavy traffic", "newETA", LocalDateTime.now().plusMinutes(30).format(DateTimeFormatter.ofPattern("HH:mm"))));
                    break;
            }
        }
    }
    
    /**
     * Start tracking an order
     */
    public static TrackingSession startTracking(String orderId, String customerId, Consumer<TrackingUpdate> updateCallback) {
        // Stop any existing session for this order
        stopTracking(orderId);
        
        TrackingSession session = new TrackingSession(orderId, customerId, updateCallback);
        activeSessions.put(orderId, session);
        
        // Send initial update
        session.updateStatus(OrderStatus.PLACED, null, "Order confirmed and processing started", 
            Map.of("orderTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm MM/dd/yyyy"))));
        
        // Start simulation
        session.startSimulation();
        
        return session;
    }
    
    /**
     * Stop tracking an order
     */
    public static void stopTracking(String orderId) {
        TrackingSession session = activeSessions.remove(orderId);
        if (session != null) {
            session.stopSimulation();
        }
    }
    
    /**
     * Get current tracking status
     */
    public static TrackingSession getTrackingSession(String orderId) {
        return activeSessions.get(orderId);
    }
    
    /**
     * Update order status manually (for real integrations)
     */
    public static void updateOrderStatus(String orderId, OrderStatus status, LocationData location, 
                                       String message, Map<String, Object> metadata) {
        TrackingSession session = activeSessions.get(orderId);
        if (session != null) {
            session.updateStatus(status, location, message, metadata);
        }
    }
    
    /**
     * Get all active tracking sessions
     */
    public static Collection<TrackingSession> getAllActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    /**
     * Create a JavaFX Task for tracking updates
     */
    public static Task<Void> createTrackingTask(String orderId, String customerId, Consumer<TrackingUpdate> updateCallback) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                TrackingSession session = startTracking(orderId, customerId, updateCallback);
                
                // Keep task alive until order is delivered or cancelled
                while (!isCancelled() && 
                       session.getCurrentStatus() != OrderStatus.DELIVERED && 
                       session.getCurrentStatus() != OrderStatus.CANCELLED) {
                    Thread.sleep(5000); // Check every 5 seconds
                }
                
                return null;
            }
            
            @Override
            protected void cancelled() {
                stopTracking(orderId);
            }
        };
    }
    
    /**
     * Generate delivery time estimate based on distance and current traffic
     */
    public static LocalDateTime estimateDeliveryTime(double distance, String trafficCondition) {
        int baseMinutes = (int) (distance * 2); // 2 minutes per mile base time
        
        // Adjust for traffic
        switch (trafficCondition.toLowerCase()) {
            case "heavy":
                baseMinutes = (int) (baseMinutes * 1.5);
                break;
            case "moderate":
                baseMinutes = (int) (baseMinutes * 1.2);
                break;
            case "light":
            default:
                // No adjustment
                break;
        }
        
        return LocalDateTime.now().plusMinutes(baseMinutes);
    }
    
    /**
     * Calculate distance between two GPS coordinates (Haversine formula)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // convert to km
        
        return distance * 0.621371; // convert to miles
    }
    
    /**
     * Cleanup resources
     */
    public static void shutdown() {
        // Stop all active sessions
        activeSessions.values().forEach(TrackingSession::stopSimulation);
        activeSessions.clear();
        
        // Shutdown executors
        scheduler.shutdown();
        executorService.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
