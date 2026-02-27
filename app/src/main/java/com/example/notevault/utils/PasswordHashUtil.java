package com.example.notevault.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Utility for hashing passwords before storing in Firestore.
 * Uses SHA-256; never store plain-text passwords.
 */
public final class PasswordHashUtil {

    private static final String ALGORITHM = "SHA-256";

    private PasswordHashUtil() {
    }

    /**
     * Hash a password with SHA-256 and return hex string.
     */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Verify that the given password matches the stored hash.
     */
    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        String computed = hash(password);
        return computed != null && computed.equalsIgnoreCase(storedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString();
    }
}
