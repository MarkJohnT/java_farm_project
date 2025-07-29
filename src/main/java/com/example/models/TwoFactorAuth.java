package com.example.models;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

/**
 * Utility class for Two-Factor Authentication using TOTP (Time-based One-Time Password)
 * Compatible with Google Authenticator and similar apps
 */
public class TwoFactorAuth {
    private static final int TIME_STEP = 30; // 30 seconds
    private static final int DIGITS = 6;
    private static final String ALGORITHM = "HmacSHA1";
    
    /**
     * Generate a random secret key for TOTP
     * @return Base32 encoded secret key
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160 bits
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }
    
    /**
     * Generate backup codes for account recovery
     * @return List of 10 backup codes
     */
    public static List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < 10; i++) {
            // Generate 8-character alphanumeric code
            StringBuilder code = new StringBuilder();
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            for (int j = 0; j < 8; j++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
            codes.add(code.toString());
        }
        
        return codes;
    }
    
    /**
     * Verify a TOTP code
     * @param secret The user's secret key
     * @param code The code to verify
     * @return true if the code is valid
     */
    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != DIGITS) {
            return false;
        }
        
        try {
            long time = System.currentTimeMillis() / 1000L;
            long timeWindow = time / TIME_STEP;
            
            // Check current time window and ±1 window for clock drift
            for (int i = -1; i <= 1; i++) {
                String expectedCode = generateTOTP(secret, timeWindow + i);
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate a TOTP code for the current time
     * @param secret The secret key
     * @return 6-digit TOTP code
     */
    public static String getCurrentCode(String secret) {
        long time = System.currentTimeMillis() / 1000L;
        long timeWindow = time / TIME_STEP;
        return generateTOTP(secret, timeWindow);
    }
    
    /**
     * Generate QR code URL for easy setup in authenticator apps
     * @param username The username
     * @param secret The secret key
     * @param issuer The application name
     * @return QR code URL
     */
    public static String generateQRCodeURL(String username, String secret, String issuer) {
        String otpAuthURL = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
            issuer, username, secret, issuer, DIGITS, TIME_STEP
        );
        
        // Google Charts QR Code API (for demonstration - in production use a proper QR library)
        return String.format(
            "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=%s",
            java.net.URLEncoder.encode(otpAuthURL, java.nio.charset.StandardCharsets.UTF_8)
        );
    }
    
    /**
     * Generate TOTP code for a specific time window
     */
    private static String generateTOTP(String secret, long timeWindow) {
        try {
            byte[] secretBytes = base32Decode(secret);
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeWindow).array();
            
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretBytes, ALGORITHM);
            mac.init(keySpec);
            
            byte[] hash = mac.doFinal(timeBytes);
            
            // Dynamic truncation
            int offset = hash[hash.length - 1] & 0x0F;
            int code = ((hash[offset] & 0x7F) << 24) |
                      ((hash[offset + 1] & 0xFF) << 16) |
                      ((hash[offset + 2] & 0xFF) << 8) |
                      (hash[offset + 3] & 0xFF);
            
            code = code % (int) Math.pow(10, DIGITS);
            
            return String.format("%0" + DIGITS + "d", code);
        } catch (Exception e) {
            throw new RuntimeException("Error generating TOTP", e);
        }
    }
    
    /**
     * Simple Base32 encoding
     */
    private static String base32Encode(byte[] bytes) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < bytes.length; i += 5) {
            long buffer = 0;
            int length = Math.min(5, bytes.length - i);
            
            for (int j = 0; j < length; j++) {
                buffer = (buffer << 8) | (bytes[i + j] & 0xFF);
            }
            
            for (int j = 0; j < Math.ceil(length * 8.0 / 5); j++) {
                result.append(alphabet.charAt((int) ((buffer >> (35 - 5 * j)) & 0x1F)));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Simple Base32 decoding
     */
    private static byte[] base32Decode(String encoded) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        
        if (encoded.length() == 0) {
            return new byte[0];
        }
        
        int outputLength = encoded.length() * 5 / 8;
        byte[] result = new byte[outputLength];
        
        long buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        
        for (char c : encoded.toCharArray()) {
            int value = alphabet.indexOf(c);
            if (value < 0) continue;
            
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            
            if (bitsLeft >= 8) {
                result[count++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        
        return result;
    }
}
