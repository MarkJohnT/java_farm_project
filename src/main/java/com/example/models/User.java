package com.example.models;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Abstract base class for all user types in the system.
 * Provides common user functionality including authentication,
 * two-factor authentication, and notification preferences.
 */
public abstract class User {
    protected String id;
    protected String fullName;
    protected String username;
    protected String email;
    protected String phoneNumber;
    protected String passwordHash;
    protected String passwordSalt;
    protected LocalDateTime joinDate;
    protected LocalDateTime lastLoginDate;
    protected boolean isVerified;
    protected boolean isActive;
    
    // Two-Factor Authentication fields
    protected boolean twoFactorEnabled;
    protected String twoFactorSecret;
    protected String backupCodes; // JSON array of backup codes
    
    // Notification preferences
    protected boolean emailNotificationsEnabled;
    protected boolean smsNotificationsEnabled;
    protected boolean orderNotificationsEnabled;
    protected boolean promotionalNotificationsEnabled;
    
    // Security and session management
    protected List<String> activeSessions; // Store session tokens
    protected LocalDateTime passwordLastChanged;
    protected int failedLoginAttempts;
    protected LocalDateTime lastFailedLogin;
    protected boolean accountLocked;
    protected LocalDateTime lockoutUntil;
    
    // Profile image management
    protected String profileImagePath;
    protected String profileImageName;
    protected LocalDateTime profileImageUploadDate;
    protected long profileImageSize; // in bytes
    protected String profileImageType; // MIME type (e.g., "image/jpeg", "image/png")
    
    /**
     * Protected constructor for subclasses
     */
    protected User(String fullName, String username, String email, String phoneNumber) {
        this.id = java.util.UUID.randomUUID().toString();
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.joinDate = LocalDateTime.now();
        this.isVerified = false;
        this.isActive = true;
        this.twoFactorEnabled = false;
        this.emailNotificationsEnabled = true;
        this.smsNotificationsEnabled = false;
        this.orderNotificationsEnabled = true;
        this.promotionalNotificationsEnabled = true;
        this.activeSessions = new ArrayList<>();
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
    }

    // Basic getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public LocalDateTime getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }
    
    public LocalDateTime getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(LocalDateTime lastLoginDate) { this.lastLoginDate = lastLoginDate; }
    
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    // Two-Factor Authentication methods
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    
    public void enableTwoFactor(String secret, String backupCodes) {
        this.twoFactorEnabled = true;
        this.twoFactorSecret = secret;
        this.backupCodes = backupCodes;
    }
    
    public void disableTwoFactor() {
        this.twoFactorEnabled = false;
        this.twoFactorSecret = null;
        this.backupCodes = null;
    }
    
    public String getTwoFactorSecret() { return twoFactorSecret; }
    public String getBackupCodes() { return backupCodes; }

    // Notification preference methods
    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(boolean enabled) { this.emailNotificationsEnabled = enabled; }
    
    public boolean isSmsNotificationsEnabled() { return smsNotificationsEnabled; }
    public void setSmsNotificationsEnabled(boolean enabled) { this.smsNotificationsEnabled = enabled; }
    
    public boolean isOrderNotificationsEnabled() { return orderNotificationsEnabled; }
    public void setOrderNotificationsEnabled(boolean enabled) { this.orderNotificationsEnabled = enabled; }
    
    public boolean isPromotionalNotificationsEnabled() { return promotionalNotificationsEnabled; }
    public void setPromotionalNotificationsEnabled(boolean enabled) { this.promotionalNotificationsEnabled = enabled; }

    // Password management
    public void setPassword(String password) {
        this.passwordSalt = generateSalt();
        this.passwordHash = hashPassword(password, this.passwordSalt);
        this.passwordLastChanged = LocalDateTime.now();
    }
    
    public boolean checkPassword(String password) {
        if (this.passwordSalt == null || this.passwordHash == null) return false;
        return this.passwordHash.equals(hashPassword(password, this.passwordSalt));
    }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
    
    public LocalDateTime getPasswordLastChanged() { return passwordLastChanged; }
    public void setPasswordLastChanged(LocalDateTime passwordLastChanged) { this.passwordLastChanged = passwordLastChanged; }

    // Account security methods
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLogin = LocalDateTime.now();
        
        // Lock account after 5 failed attempts
        if (this.failedLoginAttempts >= 5) {
            this.accountLocked = true;
            this.lockoutUntil = LocalDateTime.now().plusMinutes(30); // Lock for 30 minutes
        }
    }
    
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLogin = null;
    }
    
    public boolean isAccountLocked() {
        if (this.accountLocked && this.lockoutUntil != null) {
            // Check if lockout period has expired
            if (LocalDateTime.now().isAfter(this.lockoutUntil)) {
                this.accountLocked = false;
                this.lockoutUntil = null;
                this.failedLoginAttempts = 0;
                return false;
            }
        }
        return this.accountLocked;
    }
    
    public LocalDateTime getLockoutUntil() { return lockoutUntil; }

    // Session management
    public List<String> getActiveSessions() { return new ArrayList<>(activeSessions); }
    
    public void addSession(String sessionToken) {
        this.activeSessions.add(sessionToken);
    }
    
    public void removeSession(String sessionToken) {
        this.activeSessions.remove(sessionToken);
    }
    
    public void clearAllSessions() {
        this.activeSessions.clear();
    }
    
    public boolean hasActiveSession(String sessionToken) {
        return this.activeSessions.contains(sessionToken);
    }

    // Profile image management methods
    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    
    public String getProfileImageName() { return profileImageName; }
    public void setProfileImageName(String profileImageName) { this.profileImageName = profileImageName; }
    
    public LocalDateTime getProfileImageUploadDate() { return profileImageUploadDate; }
    public void setProfileImageUploadDate(LocalDateTime profileImageUploadDate) { this.profileImageUploadDate = profileImageUploadDate; }
    
    public long getProfileImageSize() { return profileImageSize; }
    public void setProfileImageSize(long profileImageSize) { this.profileImageSize = profileImageSize; }
    
    public String getProfileImageType() { return profileImageType; }
    public void setProfileImageType(String profileImageType) { this.profileImageType = profileImageType; }
    
    /**
     * Updates the profile image information
     * @param imagePath Full path to the image file
     * @param imageName Original name of the image file
     * @param imageSize Size of the image in bytes
     * @param imageType MIME type of the image
     */
    public void updateProfileImage(String imagePath, String imageName, long imageSize, String imageType) {
        this.profileImagePath = imagePath;
        this.profileImageName = imageName;
        this.profileImageSize = imageSize;
        this.profileImageType = imageType;
        this.profileImageUploadDate = LocalDateTime.now();
    }
    
    /**
     * Removes the profile image
     */
    public void removeProfileImage() {
        this.profileImagePath = null;
        this.profileImageName = null;
        this.profileImageSize = 0;
        this.profileImageType = null;
        this.profileImageUploadDate = null;
    }
    
    /**
     * Checks if the user has a profile image
     */
    public boolean hasProfileImage() {
        return profileImagePath != null && !profileImagePath.trim().isEmpty();
    }
    
    /**
     * Validates image file type
     */
    public boolean isValidImageType(String mimeType) {
        return mimeType != null && (
            mimeType.equals("image/jpeg") ||
            mimeType.equals("image/jpg") ||
            mimeType.equals("image/png") ||
            mimeType.equals("image/gif") ||
            mimeType.equals("image/webp")
        );
    }
    
    /**
     * Validates image file size (max 5MB)
     */
    public boolean isValidImageSize(long sizeInBytes) {
        return sizeInBytes > 0 && sizeInBytes <= 5 * 1024 * 1024; // 5MB limit
    }

    // Utility methods for password hashing
    protected static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    protected static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashed = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // Abstract methods that subclasses must implement
    public abstract String getUserType();
    public abstract boolean hasPermission(String permission);
    
    // Notification preference validation
    public void updateNotificationPreferences(boolean email, boolean sms, boolean orders, boolean promotional) {
        this.emailNotificationsEnabled = email;
        this.smsNotificationsEnabled = sms;
        this.orderNotificationsEnabled = orders;
        this.promotionalNotificationsEnabled = promotional;
    }
    
    @Override
    public String toString() {
        return String.format("%s{id='%s', username='%s', email='%s', verified=%s, active=%s}", 
                getUserType(), id, username, email, isVerified, isActive);
    }
}
