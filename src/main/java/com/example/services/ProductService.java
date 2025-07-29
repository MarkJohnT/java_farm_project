package com.example.services;

import com.example.models.Product;
import com.example.models.ProductReview;
import com.example.models.ProductCategory;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Service class for managing product operations and business logic.
 */
public class ProductService {
    private Connection dbConnection;
    
    public ProductService(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }
    
    /**
     * Create a new product
     */
    public Product createProduct(String name, double price, String description, String unit, 
                               int quantity, String farmerId, String category, boolean isOrganic) {
        Product product = new Product(name, price, description, unit, quantity, farmerId, category, isOrganic);
        
        // Save to database
        try {
            String sql = "INSERT INTO Product (id, name, price, description, unit, quantity, " +
                        "farmerId, category, isOrganic, status, createdAt, lastUpdated) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            stmt.setString(1, product.getId());
            stmt.setString(2, product.getName());
            stmt.setDouble(3, product.getPrice());
            stmt.setString(4, product.getDescription());
            stmt.setString(5, product.getUnit());
            stmt.setInt(6, product.getQuantity());
            stmt.setString(7, product.getFarmerId());
            stmt.setString(8, product.getCategory());
            stmt.setBoolean(9, product.isOrganic());
            stmt.setString(10, product.getStatus().toString());
            stmt.setObject(11, product.getCreatedAt());
            stmt.setObject(12, product.getLastUpdated());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating product: " + e.getMessage());
        }
        
        return product;
    }
    
    /**
     * Update product information
     */
    public boolean updateProduct(Product product) {
        try {
            String sql = "UPDATE Product SET name=?, price=?, description=?, unit=?, quantity=?, " +
                        "category=?, isOrganic=?, status=?, lastUpdated=? WHERE id=?";
            
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            stmt.setString(1, product.getName());
            stmt.setDouble(2, product.getPrice());
            stmt.setString(3, product.getDescription());
            stmt.setString(4, product.getUnit());
            stmt.setInt(5, product.getQuantity());
            stmt.setString(6, product.getCategory());
            stmt.setBoolean(7, product.isOrganic());
            stmt.setString(8, product.getStatus().toString());
            stmt.setObject(9, LocalDateTime.now());
            stmt.setString(10, product.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating product: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get products by farmer
     */
    public List<Product> getProductsByFarmer(String farmerId) {
        List<Product> products = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM Product WHERE farmerId = ? AND status != 'DISCONTINUED'";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            stmt.setString(1, farmerId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product product = createProductFromResultSet(rs);
                products.add(product);
            }
        } catch (SQLException e) {
            System.err.println("Error getting products by farmer: " + e.getMessage());
        }
        
        return products;
    }
    
    /**
     * Get available products for customers
     */
    public List<Product> getAvailableProducts() {
        List<Product> products = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM Product WHERE status = 'ACTIVE' AND quantity > 0";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product product = createProductFromResultSet(rs);
                products.add(product);
            }
        } catch (SQLException e) {
            System.err.println("Error getting available products: " + e.getMessage());
        }
        
        return products;
    }
    
    /**
     * Process product purchase (reduce quantity)
     */
    public boolean purchaseProduct(String productId, int quantity) {
        try {
            // First, get current product details
            String selectSql = "SELECT quantity FROM Product WHERE id = ?";
            PreparedStatement selectStmt = dbConnection.prepareStatement(selectSql);
            selectStmt.setString(1, productId);
            
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                int currentQuantity = rs.getInt("quantity");
                
                if (currentQuantity >= quantity) {
                    // Update quantity and sales data
                    String updateSql = "UPDATE Product SET quantity = quantity - ?, " +
                                     "totalSold = totalSold + ?, lastSold = ?, lastUpdated = ? " +
                                     "WHERE id = ?";
                    
                    PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql);
                    updateStmt.setInt(1, quantity);
                    updateStmt.setInt(2, quantity);
                    updateStmt.setObject(3, LocalDateTime.now());
                    updateStmt.setObject(4, LocalDateTime.now());
                    updateStmt.setString(5, productId);
                    
                    return updateStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error processing purchase: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Add a product review
     */
    public boolean addProductReview(String productId, String customerId, String customerName, 
                                  int rating, String title, String comment) {
        try {
            ProductReview review = new ProductReview(productId, customerId, customerName, 
                                                   rating, title, comment);
            
            String sql = "INSERT INTO ProductReview (id, productId, customerId, customerName, " +
                        "rating, title, comment, createdAt, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            stmt.setString(1, review.getId());
            stmt.setString(2, review.getProductId());
            stmt.setString(3, review.getCustomerId());
            stmt.setString(4, review.getCustomerName());
            stmt.setInt(5, review.getRating());
            stmt.setString(6, review.getTitle());
            stmt.setString(7, review.getComment());
            stmt.setObject(8, review.getCreatedAt());
            stmt.setString(9, review.getStatus().toString());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding product review: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get product reviews
     */
    public List<ProductReview> getProductReviews(String productId) {
        List<ProductReview> reviews = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM ProductReview WHERE productId = ? AND status = 'APPROVED' " +
                        "ORDER BY createdAt DESC";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            stmt.setString(1, productId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ProductReview review = createReviewFromResultSet(rs);
                reviews.add(review);
            }
        } catch (SQLException e) {
            System.err.println("Error getting product reviews: " + e.getMessage());
        }
        
        return reviews;
    }
    
    /**
     * Get low stock products for a farmer
     */
    public List<Product> getLowStockProducts(String farmerId) {
        List<Product> products = getProductsByFarmer(farmerId);
        return products.stream()
                      .filter(Product::isLowStock)
                      .collect(Collectors.toList());
    }
    
    /**
     * Get product statistics for farmer dashboard
     */
    public Map<String, Object> getProductStatistics(String farmerId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total products
            String totalSql = "SELECT COUNT(*) as total FROM Product WHERE farmerId = ?";
            PreparedStatement totalStmt = dbConnection.prepareStatement(totalSql);
            totalStmt.setString(1, farmerId);
            ResultSet totalRs = totalStmt.executeQuery();
            if (totalRs.next()) {
                stats.put("totalProducts", totalRs.getInt("total"));
            }
            
            // Active products
            String activeSql = "SELECT COUNT(*) as active FROM Product WHERE farmerId = ? AND status = 'ACTIVE'";
            PreparedStatement activeStmt = dbConnection.prepareStatement(activeSql);
            activeStmt.setString(1, farmerId);
            ResultSet activeRs = activeStmt.executeQuery();
            if (activeRs.next()) {
                stats.put("activeProducts", activeRs.getInt("active"));
            }
            
            // Low stock products
            String lowStockSql = "SELECT COUNT(*) as lowStock FROM Product WHERE farmerId = ? " +
                               "AND quantity <= minStockLevel AND quantity > 0";
            PreparedStatement lowStockStmt = dbConnection.prepareStatement(lowStockSql);
            lowStockStmt.setString(1, farmerId);
            ResultSet lowStockRs = lowStockStmt.executeQuery();
            if (lowStockRs.next()) {
                stats.put("lowStockProducts", lowStockRs.getInt("lowStock"));
            }
            
            // Total revenue
            String revenueSql = "SELECT SUM(totalRevenue) as revenue FROM Product WHERE farmerId = ?";
            PreparedStatement revenueStmt = dbConnection.prepareStatement(revenueSql);
            revenueStmt.setString(1, farmerId);
            ResultSet revenueRs = revenueStmt.executeQuery();
            if (revenueRs.next()) {
                stats.put("totalRevenue", revenueRs.getDouble("revenue"));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting product statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Update database schema to support new product features
     */
    public void updateProductSchema() {
        try {
            // Add new columns to Product table if they don't exist
            String[] alterStatements = {
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS category VARCHAR(100) DEFAULT 'Other'",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS isOrganic BOOLEAN DEFAULT FALSE",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE'",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS minStockLevel INT DEFAULT 10",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS totalSold INT DEFAULT 0",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS totalRevenue DOUBLE DEFAULT 0.0",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS lastSold TIMESTAMP NULL",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS lastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS averageRating DOUBLE DEFAULT 0.0",
                "ALTER TABLE Product ADD COLUMN IF NOT EXISTS totalReviews INT DEFAULT 0"
            };
            
            for (String sql : alterStatements) {
                try {
                    PreparedStatement stmt = dbConnection.prepareStatement(sql);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    // Column might already exist, ignore
                }
            }
            
            // Create ProductReview table
            String createReviewTable = """
                CREATE TABLE IF NOT EXISTS ProductReview (
                    id VARCHAR(255) PRIMARY KEY,
                    productId VARCHAR(255),
                    customerId VARCHAR(255),
                    customerName VARCHAR(255),
                    rating INT,
                    title VARCHAR(500),
                    comment TEXT,
                    createdAt TIMESTAMP,
                    lastUpdated TIMESTAMP,
                    isVerifiedPurchase BOOLEAN DEFAULT FALSE,
                    helpfulCount INT DEFAULT 0,
                    status VARCHAR(50) DEFAULT 'PENDING',
                    FOREIGN KEY (productId) REFERENCES Product(id)
                )
            """;
            
            PreparedStatement createReviewStmt = dbConnection.prepareStatement(createReviewTable);
            createReviewStmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating product schema: " + e.getMessage());
        }
    }
    
    // Helper methods
    private Product createProductFromResultSet(ResultSet rs) throws SQLException {
        Product product = new Product(
            rs.getString("name"),
            rs.getDouble("price"),
            rs.getString("description"),
            rs.getString("unit"),
            rs.getInt("quantity"),
            rs.getString("farmerId")
        );
        
        // Set additional fields if they exist
        try {
            product.setCategory(rs.getString("category"));
            product.setOrganic(rs.getBoolean("isOrganic"));
            // Set other fields as needed
        } catch (SQLException e) {
            // Fields might not exist in older schema
        }
        
        return product;
    }
    
    private ProductReview createReviewFromResultSet(ResultSet rs) throws SQLException {
        ProductReview review = new ProductReview(
            rs.getString("productId"),
            rs.getString("customerId"),
            rs.getString("customerName"),
            rs.getInt("rating"),
            rs.getString("title"),
            rs.getString("comment")
        );
        
        return review;
    }
}
