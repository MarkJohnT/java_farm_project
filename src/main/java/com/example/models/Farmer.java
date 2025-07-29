package com.example.models;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Farmer extends User {
    private String farmName;
    private String farmLocation;
    private List<Product> products;

    public Farmer(String fullName, String username, String email, String phoneNumber,
            String farmName, String farmLocation, String password) {
        super(fullName, username, email, phoneNumber);
        this.farmName = farmName;
        this.farmLocation = farmLocation;
        this.products = new ArrayList<>();
        setPassword(password);
    }

    // Farm-specific getters and setters
    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getFarmLocation() {
        return farmLocation;
    }

    public void setFarmLocation(String farmLocation) {
        this.farmLocation = farmLocation;
    }

    // Product management
    public List<Product> getProducts() {
        return products;
    }

    public void addProduct(Product product) {
        this.products.add(product);
    }
    
    public void removeProduct(Product product) {
        this.products.remove(product);
    }
    
    public Product getProductById(String productId) {
        return products.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    // Abstract method implementations from User
    @Override
    public String getUserType() {
        return "Farmer";
    }

    @Override
    public boolean hasPermission(String permission) {
        // Farmer permissions
        switch (permission.toLowerCase()) {
            case "add_product":
            case "edit_product":
            case "delete_product":
            case "view_products":
            case "view_profile":
            case "update_profile":
            case "view_orders":
            case "update_order_status":
            case "view_sales_reports":
                return true;
            case "manage_users":
            case "manage_farmers":
            case "view_all_reports":
            case "system_admin":
                return false;
            default:
                return false;
        }
    }
    
    // Business methods
    public int getTotalProducts() {
        return products.size();
    }
    
    public int getActiveProducts() {
        return (int) products.stream()
                .filter(p -> p.getQuantity() > 0)
                .count();
    }
    
    public double getTotalInventoryValue() {
        return products.stream()
                .mapToDouble(p -> p.getPrice() * p.getQuantity())
                .sum();
    }
    
    // Product image management methods
    /**
     * Updates the image for a specific product
     * @param productId ID of the product to update
     * @param imagePath Full path to the image file
     * @param imageName Original name of the image file
     * @param imageSize Size of the image in bytes
     * @param imageType MIME type of the image
     * @return true if successful, false if product not found
     */
    public boolean updateProductImage(String productId, String imagePath, String imageName, long imageSize, String imageType) {
        Product product = getProductById(productId);
        if (product != null && isValidImageType(imageType) && isValidImageSize(imageSize)) {
            product.setImagePath(imagePath);
            // You might want to add more image metadata to Product class
            return true;
        }
        return false;
    }
    
    /**
     * Removes the image for a specific product
     * @param productId ID of the product
     * @return true if successful, false if product not found
     */
    public boolean removeProductImage(String productId) {
        Product product = getProductById(productId);
        if (product != null) {
            product.setImagePath(null);
            return true;
        }
        return false;
    }
    
    /**
     * Gets all products that have images
     */
    public List<Product> getProductsWithImages() {
        return products.stream()
                .filter(p -> p.getImagePath() != null && !p.getImagePath().trim().isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all products without images
     */
    public List<Product> getProductsWithoutImages() {
        return products.stream()
                .filter(p -> p.getImagePath() == null || p.getImagePath().trim().isEmpty())
                .collect(Collectors.toList());
    }
}