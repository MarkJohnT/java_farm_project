package com.example.models;

import java.util.*;

/**
 * Utility class for managing product categories and tags.
 */
public class ProductCategory {
    
    // Predefined product categories
    public static final String VEGETABLES = "Vegetables";
    public static final String FRUITS = "Fruits";
    public static final String GRAINS = "Grains";
    public static final String DAIRY = "Dairy";
    public static final String HERBS = "Herbs";
    public static final String SPICES = "Spices";
    public static final String NUTS = "Nuts";
    public static final String SEEDS = "Seeds";
    public static final String MEAT = "Meat";
    public static final String EGGS = "Eggs";
    public static final String HONEY = "Honey";
    public static final String OTHER = "Other";
    
    // Category icons for UI
    private static final Map<String, String> CATEGORY_ICONS = new HashMap<>();
    
    // Common tags for each category
    private static final Map<String, Set<String>> CATEGORY_TAGS = new HashMap<>();
    
    static {
        // Initialize category icons
        CATEGORY_ICONS.put(VEGETABLES, "🥕");
        CATEGORY_ICONS.put(FRUITS, "🍎");
        CATEGORY_ICONS.put(GRAINS, "🌾");
        CATEGORY_ICONS.put(DAIRY, "🥛");
        CATEGORY_ICONS.put(HERBS, "🌿");
        CATEGORY_ICONS.put(SPICES, "🌶️");
        CATEGORY_ICONS.put(NUTS, "🥜");
        CATEGORY_ICONS.put(SEEDS, "🌰");
        CATEGORY_ICONS.put(MEAT, "🥩");
        CATEGORY_ICONS.put(EGGS, "🥚");
        CATEGORY_ICONS.put(HONEY, "🍯");
        CATEGORY_ICONS.put(OTHER, "📦");
        
        // Initialize category tags
        CATEGORY_TAGS.put(VEGETABLES, new HashSet<>(Arrays.asList(
            "fresh", "organic", "local", "seasonal", "raw", "cooked", "leafy", "root"
        )));
        
        CATEGORY_TAGS.put(FRUITS, new HashSet<>(Arrays.asList(
            "fresh", "organic", "local", "seasonal", "sweet", "juicy", "ripe", "dried"
        )));
        
        CATEGORY_TAGS.put(GRAINS, new HashSet<>(Arrays.asList(
            "organic", "whole", "processed", "flour", "cereal", "ancient", "gluten-free"
        )));
        
        CATEGORY_TAGS.put(DAIRY, new HashSet<>(Arrays.asList(
            "fresh", "organic", "raw", "pasteurized", "low-fat", "full-fat", "artisan"
        )));
        
        CATEGORY_TAGS.put(HERBS, new HashSet<>(Arrays.asList(
            "fresh", "dried", "organic", "aromatic", "medicinal", "culinary", "potted"
        )));
        
        CATEGORY_TAGS.put(SPICES, new HashSet<>(Arrays.asList(
            "ground", "whole", "dried", "organic", "exotic", "hot", "mild", "aromatic"
        )));
        
        CATEGORY_TAGS.put(NUTS, new HashSet<>(Arrays.asList(
            "raw", "roasted", "salted", "unsalted", "organic", "shelled", "unshelled"
        )));
        
        CATEGORY_TAGS.put(SEEDS, new HashSet<>(Arrays.asList(
            "organic", "heirloom", "hybrid", "treated", "untreated", "sprouting"
        )));
        
        CATEGORY_TAGS.put(MEAT, new HashSet<>(Arrays.asList(
            "fresh", "frozen", "organic", "grass-fed", "free-range", "local", "lean"
        )));
        
        CATEGORY_TAGS.put(EGGS, new HashSet<>(Arrays.asList(
            "fresh", "organic", "free-range", "cage-free", "brown", "white", "duck", "quail"
        )));
        
        CATEGORY_TAGS.put(HONEY, new HashSet<>(Arrays.asList(
            "raw", "processed", "organic", "wildflower", "clover", "local", "artisan"
        )));
        
        CATEGORY_TAGS.put(OTHER, new HashSet<>(Arrays.asList(
            "specialty", "artisan", "handmade", "local", "unique"
        )));
    }
    
    /**
     * Get all available product categories
     */
    public static List<String> getAllCategories() {
        return Arrays.asList(
            VEGETABLES, FRUITS, GRAINS, DAIRY, HERBS, SPICES, 
            NUTS, SEEDS, MEAT, EGGS, HONEY, OTHER
        );
    }
    
    /**
     * Get icon for a category
     */
    public static String getCategoryIcon(String category) {
        return CATEGORY_ICONS.getOrDefault(category, "📦");
    }
    
    /**
     * Get suggested tags for a category
     */
    public static Set<String> getSuggestedTags(String category) {
        return new HashSet<>(CATEGORY_TAGS.getOrDefault(category, new HashSet<>()));
    }
    
    /**
     * Check if a category is valid
     */
    public static boolean isValidCategory(String category) {
        return getAllCategories().contains(category);
    }
    
    /**
     * Get category display name with icon
     */
    public static String getCategoryDisplayName(String category) {
        return getCategoryIcon(category) + " " + category;
    }
    
    /**
     * Get all common tags across categories
     */
    public static Set<String> getAllCommonTags() {
        Set<String> allTags = new HashSet<>();
        CATEGORY_TAGS.values().forEach(allTags::addAll);
        return allTags;
    }
    
    /**
     * Filter products by category
     */
    public static List<Product> filterByCategory(List<Product> products, String category) {
        if (category == null || category.isEmpty() || category.equals("All")) {
            return new ArrayList<>(products);
        }
        
        return products.stream()
                      .filter(product -> category.equals(product.getCategory()))
                      .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Filter products by tags
     */
    public static List<Product> filterByTags(List<Product> products, Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>(products);
        }
        
        return products.stream()
                      .filter(product -> product.getTags().stream()
                                               .anyMatch(tags::contains))
                      .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Search products by term
     */
    public static List<Product> searchProducts(List<Product> products, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>(products);
        }
        
        return products.stream()
                      .filter(product -> product.matchesSearchTerm(searchTerm))
                      .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Sort products by various criteria
     */
    public enum SortBy {
        NAME, PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW, RATING, NEWEST, POPULARITY
    }
    
    public static List<Product> sortProducts(List<Product> products, SortBy sortBy) {
        List<Product> sortedProducts = new ArrayList<>(products);
        
        switch (sortBy) {
            case NAME:
                sortedProducts.sort(Comparator.comparing(Product::getName));
                break;
            case PRICE_LOW_TO_HIGH:
                sortedProducts.sort(Comparator.comparing(Product::getPrice));
                break;
            case PRICE_HIGH_TO_LOW:
                sortedProducts.sort(Comparator.comparing(Product::getPrice).reversed());
                break;
            case RATING:
                sortedProducts.sort(Comparator.comparing(Product::getAverageRating).reversed());
                break;
            case NEWEST:
                sortedProducts.sort(Comparator.comparing(Product::getCreatedAt).reversed());
                break;
            case POPULARITY:
                sortedProducts.sort(Comparator.comparing(Product::getTotalSold).reversed());
                break;
        }
        
        return sortedProducts;
    }
}
