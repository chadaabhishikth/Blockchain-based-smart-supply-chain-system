package com.supplychain.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Phase 1: SHA-256 Cryptographic Hashing Utility
 * ===============================================
 * This class provides the foundational cryptographic fingerprinting for our
 * blockchain-based supply chain system.
 *
 * Every transaction will be hashed using SHA-256 to create an unforgeable
 * digital fingerprint. Even the slightest change in transaction data will
 * completely change the hash output.
 *
 * COMPLEXITY: O(1) time and space for hashing
 */
public class HashUtils {

    /**
     * Calculate the SHA-256 hash of a string.
     *
     * SHA-256 produces a fixed 256-bit (32-byte) hash regardless of input size.
     * It exhibits:
     * - Determinism: Same input always produces same output
     * - Avalanche effect: Small input changes cause massive output changes
     * - One-way function: Practically impossible to reverse
     *
     * @param data String to hash
     * @return Hexadecimal string representation of the 256-bit hash (64 characters)
     */
    public static String calculateSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Convert byte array to hexadecimal string.
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

    /**
     * Serialize a transaction map to a canonical JSON string.
     *
     * CRITICAL: This must produce identical output for identical input.
     * We sort keys to ensure consistent ordering.
     *
     * @param transaction Map containing transaction data
     * @return Canonical string representation
     */
    public static String serializeTransaction(Map<String, Object> transaction) {
        // Sort keys for consistent ordering
        TreeMap<String, Object> sortedMap = new TreeMap<>(transaction);
        StringBuilder sb = new StringBuilder();
        serializeMap(sortedMap, sb);
        return sb.toString();
    }

    /**
     * Recursively serialize a map to string format.
     */
    private static void serializeMap(Map<String, Object> map, StringBuilder sb) {
        sb.append("{");
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Map) {
                serializeMap((Map<String, Object>) value, sb);
            } else if (value instanceof List) {
                serializeList((List<Object>) value, sb);
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
    }

    /**
     * Recursively serialize a list to string format.
     */
    private static void serializeList(List<Object> list, StringBuilder sb) {
        sb.append("[");
        Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (value instanceof Map) {
                serializeMap((Map<String, Object>) value, sb);
            } else if (value instanceof List) {
                serializeList((List<Object>) value, sb);
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("]");
    }

    /**
     * Create a unique cryptographic fingerprint for a transaction.
     *
     * This is the core hashing function that all transactions pass through.
     *
     * @param transaction Map with keys: productId, sender, receiver, timestamp, location
     * @return SHA-256 hash of the transaction as a 64-character hex string
     */
    public static String hashTransaction(Map<String, Object> transaction) {
        String serialized = serializeTransaction(transaction);
        return calculateSHA256(serialized);
    }

    /**
     * Verify that a transaction matches an expected hash.
     *
     * This is our tamper detection mechanism. If anyone modifies even
     * a single character in the transaction, the hash will change completely.
     *
     * @param transaction The transaction data to verify
     * @param expectedHash The hash we expect the transaction to produce
     * @return true if the transaction matches the expected hash, false otherwise
     */
    public static boolean verifyTransactionIntegrity(Map<String, Object> transaction, String expectedHash) {
        String computedHash = hashTransaction(transaction);
        return computedHash.equals(expectedHash);
    }

    /**
     * Factory method to create a well-structured transaction object.
     *
     * Ensures all transactions have the same structure for consistent hashing.
     *
     * @param productId Unique identifier for the product
     * @param sender Current owner/entity transferring the product
     * @param receiver New owner/entity receiving the product
     * @param location Physical location of the transaction
     * @param metadata Optional additional data
     * @return Complete transaction map with timestamp
     */
    public static Map<String, Object> createTransaction(
            String productId,
            String sender,
            String receiver,
            String location,
            Map<String, Object> metadata) {

        Map<String, Object> transaction = new LinkedHashMap<>();
        transaction.put("product_id", productId);
        transaction.put("sender", sender);
        transaction.put("receiver", receiver);
        transaction.put("location", location);
        transaction.put("timestamp", Instant.now().toString());
        transaction.put("metadata", metadata != null ? metadata : new LinkedHashMap<>());

        return transaction;
    }

    // =========================================================================
    // UNIT TESTS FOR SHA-256 HASHING
    // =========================================================================

    public static void testHashConsistency() {
        Map<String, Object> tx = createTransaction(
                "PROD-001",
                "Factory A",
                "Distributor B",
                "Shanghai, China",
                null
        );

        String hash1 = hashTransaction(tx);
        String hash2 = hashTransaction(tx);

        assert hash1.equals(hash2) : "Same transaction must produce identical hash";
        System.out.println("✓ Hash consistency test passed");
    }

    public static void testAvalancheEffect() {
        Map<String, Object> tx1 = createTransaction(
                "PROD-001",
                "Factory A",
                "Distributor B",
                "Warehouse 1",
                null
        );

        Map<String, Object> tx2 = new LinkedHashMap<>(tx1);
        tx2.put("location", "Warehouse 2");  // Only change the location

        String hash1 = hashTransaction(tx1);
        String hash2 = hashTransaction(tx2);

        assert !hash1.equals(hash2) : "Different data must produce different hashes";

        // Count how many characters changed (should be ~50% for true avalanche)
        long differingChars = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                differingChars++;
            }
        }
        double changePercentage = (differingChars * 100.0) / 64.0;

        System.out.println("✓ Avalanche effect test passed: " + String.format("%.1f", changePercentage) + "% of hash characters changed");
    }

    public static void testHashUniqueness() {
        Map<String, Object> tx1 = createTransaction(
                "PROD-001",
                "Factory A",
                "Distributor B",
                "Shanghai",
                null
        );

        Map<String, Object> tx2 = createTransaction(
                "PROD-002",  // Different product
                "Factory A",
                "Distributor B",
                "Shanghai",
                null
        );

        String hash1 = hashTransaction(tx1);
        String hash2 = hashTransaction(tx2);

        assert !hash1.equals(hash2) : "Different transactions must produce different hashes";
        System.out.println("✓ Hash uniqueness test passed");
    }

    public static void testHashLength() {
        Map<String, Object> tx = createTransaction(
                "PROD-001",
                "Factory A",
                "Distributor B",
                "Test Location",
                null
        );

        String hashResult = hashTransaction(tx);

        assert hashResult.length() == 64 : "SHA-256 must produce 64 chars, got " + hashResult.length();
        assert hashResult.matches("[0-9a-f]+") : "Hash must be hexadecimal";

        System.out.println("✓ Hash length test passed: 64 hexadecimal characters");
    }

    /**
     * Run all hash utility tests.
     */
    public static void runAllTests() {
        System.out.println("============================================================");
        System.out.println("PHASE 1: SHA-256 HASHING UNIT TESTS");
        System.out.println("============================================================\n");

        testHashConsistency();
        testAvalancheEffect();
        testHashUniqueness();
        testHashLength();

        System.out.println("\n============================================================");
        System.out.println("ALL TESTS PASSED ✓");
        System.out.println("============================================================\n");
    }

    public static void main(String[] args) {
        runAllTests();
    }
}
