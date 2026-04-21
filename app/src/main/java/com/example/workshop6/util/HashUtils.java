// Contributor(s): Owen
// Main: Owen - SHA-256 hex for client-side fingerprints where needed.

package com.example.workshop6.util;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2 password hashing for storage strings built during registration flows.
 */
public class HashUtils {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Hashes {@code password} with a random 16-byte salt using PBKDF2WithHmacSHA256.
     * Returns Base64 salt and Base64 hash joined by one ASCII colon for storage.
     */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        char[] chars = password.toCharArray();
        try {
            byte[] hash = pbkdf2(chars, salt);
            return Base64.encodeToString(salt, Base64.NO_WRAP)
                    + ":" + Base64.encodeToString(hash, Base64.NO_WRAP);
        } finally {
            java.util.Arrays.fill(chars, '\0');
        }
    }

    /**
     * Verifies {@code plainPassword} against a stored salt-and-hash string from {@link #hash}.
     * Uses constant-time comparison so timing does not leak password material.
     */
    public static boolean verify(String plainPassword, String storedValue) {
        String[] parts = storedValue.split(":", 2);
        if (parts.length != 2) return false;
        try {
            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[1], Base64.NO_WRAP);
            char[] password = plainPassword.toCharArray();
            try {
                byte[] actualHash = pbkdf2(password, salt);
                return MessageDigest.isEqual(expectedHash, actualHash);
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return hash;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 unavailable", e);
        }
    }
}
