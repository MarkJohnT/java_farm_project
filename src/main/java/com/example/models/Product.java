package com.example.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class Product {
    private String id;
    private String name;
    private double price;
    private String description;
    private String unit; // e.g., kg, piece, bag
    private int quantity;
    private int minStockLevel; // Minimum stock before warning
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String farmerId;
    private String imagePath;
    
    // Product categorization and search
    private String category; // e.g., "Vegetables", "Fruits", "Grains", "Dairy"
    private Set<String> tags; // e.g., "organic", "seasonal", "fresh"
    private boolean isOrganic;
    private boolean isSeasonal;
    
    // Product status and availability
    private ProductStatus status; // ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED
    private boolean isAvailable;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    
    // Product reviews and ratings
    private List<ProductReview> reviews;
    private double averageRating;
    private int totalReviews;
    
    // Inventory and sales tracking
    private int totalSold;
    private double totalRevenue;
    private LocalDateTime lastSold;
    
    // Product specifications
    private String origin; // Farm location or region
    private String harvestDate;
    private String shelfLife;
    private String storageInstructions;
    
    public enum ProductStatus {
        ACTIVE,
        INACTIVE,
        OUT_OF_STOCK,
        DISCONTINUED,
        PENDING_APPROVAL
    }

    public Product(String name, double price, String description, String unit, int quantity, String farmerId) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.price = price;
        this.description = description;
        this.unit = unit;
        this.quantity = quantity;
        this.minStockLevel = 10; // Default minimum stock level
        this.farmerId = farmerId;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        
        // Initialize collections and defaults
        this.tags = new HashSet<>();
        this.reviews = new ArrayList<>();
        this.category = "Other";
        this.isOrganic = false;
        this.isSeasonal = false;
        this.status = ProductStatus.ACTIVE;
        this.isAvailable = true;
        this.averageRating = 0.0;
        this.totalReviews = 0;
        this.totalSold = 0;
        this.totalRevenue = 0.0;
    }
    
    // Enhanced constructor with category and tags
    public Product(String name, double price, String description, String unit, int quantity, 
                   String farmerId, String category, boolean isOrganic) {
        this(name, price, description, unit, quantity, farmerId);
        this.category = category;
        this.isOrganic = isOrganic;
    }

    // Basic getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.lastUpdated = LocalDateTime.now();
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.lastUpdated = LocalDateTime.now();
        updateAvailabilityStatus();
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Category and classification methods
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public Set<String> getTags() {
        return new HashSet<>(tags);
    }
    
    public void addTag(String tag) {
        this.tags.add(tag.toLowerCase());
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void removeTag(String tag) {
        this.tags.remove(tag.toLowerCase());
        this.lastUpdated = LocalDateTime.now();
    }
    
    public boolean hasTag(String tag) {
        return this.tags.contains(tag.toLowerCase());
    }
    
    public boolean isOrganic() {
        return isOrganic;
    }
    
    public void setOrganic(boolean organic) {
        this.isOrganic = organic;
        this.lastUpdated = LocalDateTime.now();
        if (organic) {
            addTag("organic");
        } else {
            removeTag("organic");
        }
    }
    
    public boolean isSeasonal() {
        return isSeasonal;
    }
    
    public void setSeasonal(boolean seasonal) {
        this.isSeasonal = seasonal;
        this.lastUpdated = LocalDateTime.now();
        if (seasonal) {
            addTag("seasonal");
        } else {
            removeTag("seasonal");
        }
    }
    
    // Status and availability methods
    public ProductStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProductStatus status) {
        this.status = status;
        this.lastUpdated = LocalDateTime.now();
        updateAvailabilityStatus();
    }
    
    public boolean isAvailable() {
        return isAvailable && status == ProductStatus.ACTIVE && quantity > 0;
    }
    
    private void updateAvailabilityStatus() {
        this.isAvailable = (status == ProductStatus.ACTIVE && quantity > 0);
        if (quantity == 0 && status == ProductStatus.ACTIVE) {
            this.status = ProductStatus.OUT_OF_STOCK;
        }
    }
    
    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }
    
    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }
    
    public LocalDateTime getAvailableUntil() {
        return availableUntil;
    }
    
    public void setAvailableUntil(LocalDateTime availableUntil) {
        this.availableUntil = availableUntil;
    }
    
    // Review and rating methods
    public List<ProductReview> getReviews() {
        return new ArrayList<>(reviews);
    }
    
    public List<ProductReview> getApprovedReviews() {
        return reviews.stream()
                     .filter(ProductReview::isApproved)
                     .collect(java.util.stream.Collectors.toList());
    }
    
    public void addReview(ProductReview review) {
        this.reviews.add(review);
        calculateAverageRating();
    }
    
    public void removeReview(String reviewId) {
        this.reviews.removeIf(review -> review.getId().equals(reviewId));
        calculateAverageRating();
    }
    
    public double getAverageRating() {
        return averageRating;
    }
    
    public int getTotalReviews() {
        return totalReviews;
    }
    
    private void calculateAverageRating() {
        List<ProductReview> approvedReviews = getApprovedReviews();
        this.totalReviews = approvedReviews.size();
        
        if (totalReviews == 0) {
            this.averageRating = 0.0;
            return;
        }
        
        double sum = approvedReviews.stream()
                                   .mapToInt(ProductReview::getRating)
                                   .sum();
        this.averageRating = sum / totalReviews;
    }
    
    public String getFormattedRating() {
        if (totalReviews == 0) {
            return "No reviews yet";
        }
        return String.format("%.1f ★ (%d reviews)", averageRating, totalReviews);
    }
    
    // Inventory and sales methods
    public int getTotalSold() {
        return totalSold;
    }
    
    public double getTotalRevenue() {
        return totalRevenue;
    }
    
    public LocalDateTime getLastSold() {
        return lastSold;
    }
    
    public boolean reduceQuantity(int amount) {
        if (quantity >= amount) {
            this.quantity -= amount;
            this.totalSold += amount;
            this.totalRevenue += (amount * price);
            this.lastSold = LocalDateTime.now();
            this.lastUpdated = LocalDateTime.now();
            updateAvailabilityStatus();
            return true;
        }
        return false;
    }
    
    public void restockQuantity(int amount) {
        this.quantity += amount;
        this.lastUpdated = LocalDateTime.now();
        if (status == ProductStatus.OUT_OF_STOCK && quantity > 0) {
            this.status = ProductStatus.ACTIVE;
        }
        updateAvailabilityStatus();
    }
    
    public boolean isLowStock() {
        return quantity <= minStockLevel && quantity > 0;
    }
    
    public boolean isOutOfStock() {
        return quantity == 0;
    }
    
    // Product specifications
    public String getOrigin() {
        return origin;
    }
    
    public void setOrigin(String origin) {
        this.origin = origin;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getHarvestDate() {
        return harvestDate;
    }
    
    public void setHarvestDate(String harvestDate) {
        this.harvestDate = harvestDate;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getShelfLife() {
        return shelfLife;
    }
    
    public void setShelfLife(String shelfLife) {
        this.shelfLife = shelfLife;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getStorageInstructions() {
        return storageInstructions;
    }
    
    public void setStorageInstructions(String storageInstructions) {
        this.storageInstructions = storageInstructions;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Utility methods
    public String getAvailabilityMessage() {
        if (!isAvailable()) {
            if (status == ProductStatus.OUT_OF_STOCK) {
                return "Out of Stock";
            } else if (status == ProductStatus.INACTIVE) {
                return "Not Available";
            } else if (status == ProductStatus.DISCONTINUED) {
                return "Discontinued";
            }
        } else if (isLowStock()) {
            return "Low Stock (" + quantity + " remaining)";
        }
        return "In Stock (" + quantity + " available)";
    }
    
    public String getFormattedPrice() {
        return String.format("$%.2f/%s", price, unit);
    }
    
    public boolean matchesSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }
        
        String term = searchTerm.toLowerCase();
        return name.toLowerCase().contains(term) ||
               description.toLowerCase().contains(term) ||
               category.toLowerCase().contains(term) ||
               tags.stream().anyMatch(tag -> tag.contains(term));
    }
    
    @Override
    public String toString() {
        return String.format("Product{id='%s', name='%s', price=%.2f, category='%s', status=%s, quantity=%d}", 
                id, name, price, category, status, quantity);
    }
}