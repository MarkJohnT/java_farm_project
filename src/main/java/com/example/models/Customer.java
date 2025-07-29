package com.example.models;

public class Customer extends User {
    private String location;
    private boolean isAdmin;

    public Customer(String fullName, String username, String email, String phoneNumber, String location) {
        super(fullName, username, email, phoneNumber);
        this.location = location;
        this.isAdmin = false;
    }
    
    // Constructor with password for registration
    public Customer(String fullName, String username, String email, String phoneNumber, String location, String password) {
        super(fullName, username, email, phoneNumber);
        this.location = location;
        this.isAdmin = false;
        setPassword(password);
    }

    // Location-specific getters and setters
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // Admin-specific methods
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
    }

    // Abstract method implementations from User
    @Override
    public String getUserType() {
        return "Customer";
    }

    @Override
    public boolean hasPermission(String permission) {
        if (isAdmin) {
            return true; // Admins have all permissions
        }
        
        // Regular customer permissions
        switch (permission.toLowerCase()) {
            case "place_order":
            case "view_products":
            case "view_profile":
            case "update_profile":
            case "view_orders":
            case "cancel_order":
                return true;
            case "manage_users":
            case "manage_products":
            case "view_reports":
            case "manage_farmers":
                return false;
            default:
                return false;
        }
    }
    
    // Legacy compatibility method for joinDate as String
    public String getJoinDateString() {
        return getJoinDate().toString();
    }
}