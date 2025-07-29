package com.example;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service class for handling image upload functionality
 * Manages file selection, validation, and storage for user profiles and product images
 */
public class ImageUploadService {
    
    // Default upload directories
    private static final String PROFILE_IMAGES_DIR = "uploads/profiles/";
    private static final String PRODUCT_IMAGES_DIR = "uploads/products/";
    
    // Maximum file size (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    // Supported image types
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
        "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"
    );
    
    private static final List<String> SUPPORTED_MIME_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    /**
     * Opens a file chooser dialog for image selection
     * @param ownerStage The parent stage for the dialog
     * @param title Dialog title
     * @return Selected file or null if cancelled
     */
    public static File chooseImageFile(Stage ownerStage, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        
        // Set extension filters
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
            "Image Files", SUPPORTED_EXTENSIONS
        );
        fileChooser.getExtensionFilters().add(imageFilter);
        
        // Set initial directory to user's pictures directory
        String userHome = System.getProperty("user.home");
        File picturesDir = new File(userHome, "Pictures");
        if (picturesDir.exists()) {
            fileChooser.setInitialDirectory(picturesDir);
        }
        
        return fileChooser.showOpenDialog(ownerStage);
    }
    
    /**
     * Validates an image file
     * @param file The file to validate
     * @return ValidationResult containing success status and message
     */
    public static ValidationResult validateImageFile(File file) {
        if (file == null) {
            return new ValidationResult(false, "No file selected");
        }
        
        if (!file.exists()) {
            return new ValidationResult(false, "File does not exist");
        }
        
        if (!file.isFile()) {
            return new ValidationResult(false, "Selected item is not a file");
        }
        
        // Check file size
        if (file.length() > MAX_FILE_SIZE) {
            return new ValidationResult(false, "File size exceeds 5MB limit");
        }
        
        if (file.length() == 0) {
            return new ValidationResult(false, "File is empty");
        }
        
        // Check file extension
        String fileName = file.getName().toLowerCase();
        boolean hasValidExtension = SUPPORTED_EXTENSIONS.stream()
                .anyMatch(ext -> fileName.endsWith(ext.substring(1))); // Remove * from extension
        
        if (!hasValidExtension) {
            return new ValidationResult(false, "Unsupported file type. Please select a JPG, PNG, GIF, or WebP image");
        }
        
        // Try to detect MIME type
        try {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase())) {
                return new ValidationResult(false, "Invalid image file format");
            }
        } catch (IOException e) {
            // If we can't probe the content type, rely on extension validation
        }
        
        return new ValidationResult(true, "File is valid");
    }
    
    /**
     * Saves a profile image to the uploads directory
     * @param sourceFile The source image file
     * @param userId The user ID for naming
     * @return UploadResult containing success status and file path
     */
    public static UploadResult saveProfileImage(File sourceFile, String userId) {
        ValidationResult validation = validateImageFile(sourceFile);
        if (!validation.isValid()) {
            return new UploadResult(false, null, validation.getMessage());
        }
        
        try {
            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(PROFILE_IMAGES_DIR);
            Files.createDirectories(uploadDir);
            
            // Generate unique filename
            String extension = getFileExtension(sourceFile.getName());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("profile_%s_%s.%s", userId, timestamp, extension);
            
            Path targetPath = uploadDir.resolve(fileName);
            
            // Copy file to upload directory
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            return new UploadResult(true, targetPath.toString(), "Profile image uploaded successfully");
            
        } catch (IOException e) {
            return new UploadResult(false, null, "Failed to save image: " + e.getMessage());
        }
    }
    
    /**
     * Saves a product image to the uploads directory
     * @param sourceFile The source image file
     * @param farmerId The farmer ID
     * @param productId The product ID for naming
     * @return UploadResult containing success status and file path
     */
    public static UploadResult saveProductImage(File sourceFile, String farmerId, String productId) {
        ValidationResult validation = validateImageFile(sourceFile);
        if (!validation.isValid()) {
            return new UploadResult(false, null, validation.getMessage());
        }
        
        try {
            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(PRODUCT_IMAGES_DIR);
            Files.createDirectories(uploadDir);
            
            // Generate unique filename
            String extension = getFileExtension(sourceFile.getName());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("product_%s_%s_%s.%s", farmerId, productId, timestamp, extension);
            
            Path targetPath = uploadDir.resolve(fileName);
            
            // Copy file to upload directory
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            return new UploadResult(true, targetPath.toString(), "Product image uploaded successfully");
            
        } catch (IOException e) {
            return new UploadResult(false, null, "Failed to save image: " + e.getMessage());
        }
    }
    
    /**
     * Deletes an image file
     * @param imagePath Path to the image file
     * @return true if successful, false otherwise
     */
    public static boolean deleteImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path path = Paths.get(imagePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete image: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the file extension from a filename
     * @param fileName The filename
     * @return The extension without the dot
     */
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "jpg"; // Default extension
    }
    
    /**
     * Result class for file validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
    
    /**
     * Result class for file upload operations
     */
    public static class UploadResult {
        private final boolean success;
        private final String filePath;
        private final String message;
        
        public UploadResult(boolean success, String filePath, String message) {
            this.success = success;
            this.filePath = filePath;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getFilePath() { return filePath; }
        public String getMessage() { return message; }
    }
}
