package com.supplychain.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Crypto Layer — SHA-256 Implementation
 * =======================================
 *
 * The default {@link HashFunction} used across the system. Provides the
 * foundational cryptographic fingerprinting for the blockchain-based
 * supply chain: every transaction and every Merkle node is fingerprinted
 * with SHA-256.
 *
 * Properties:
 * - Determinism: same input always produces the same output
 * - Avalanche effect: tiny input changes cause massive output changes
 * - One-way function: practically impossible to reverse
 *
 * COMPLEXITY: O(1) time and space for hashing.
 */
public final class Sha256Hasher implements HashFunction {

    @Override
    public String hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Convert a byte array to its lowercase hexadecimal string representation.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
