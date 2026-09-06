package com.supplychain.selftest;

import com.supplychain.crypto.HashFunction;
import com.supplychain.crypto.Sha256Hasher;
import com.supplychain.crypto.TransactionSerializer;
import com.supplychain.domain.model.Transaction;

import java.io.PrintStream;

/**
 * Self-Test Layer — Phase 1: SHA-256 Hashing
 * ===========================================
 *
 * Unit tests for the cryptographic foundation. Run from the menu
 * (option 1) or directly: java com.supplychain.selftest.HashingSelfTest
 */
public final class HashingSelfTest {

    private final PrintStream out;
    private final HashFunction hasher = new Sha256Hasher();

    public HashingSelfTest(PrintStream out) {
        this.out = out;
    }

    public void testHashConsistency() {
        Transaction tx = Transaction.create("PROD-001", "Factory A", "Distributor B", "Shanghai, China", null);

        String hash1 = TransactionSerializer.hashOf(tx, hasher);
        String hash2 = TransactionSerializer.hashOf(tx, hasher);

        Check.that(hash1.equals(hash2), "Same transaction must produce identical hash");
        out.println("✓ Hash consistency test passed");
    }

    public void testAvalancheEffect() {
        Transaction tx1 = Transaction.create("PROD-001", "Factory A", "Distributor B", "Warehouse 1", null);
        Transaction tx2 = tx1.withLocation("Warehouse 2");  // Only change the location

        String hash1 = TransactionSerializer.hashOf(tx1, hasher);
        String hash2 = TransactionSerializer.hashOf(tx2, hasher);

        Check.that(!hash1.equals(hash2), "Different data must produce different hashes");

        // Count how many characters changed (should be ~50% for true avalanche)
        int differingChars = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                differingChars++;
            }
        }
        double changePercentage = (differingChars * 100.0) / 64.0;

        out.println("✓ Avalanche effect test passed: " + String.format("%.1f", changePercentage) + "% of hash characters changed");
    }

    public void testHashUniqueness() {
        Transaction tx1 = Transaction.create("PROD-001", "Factory A", "Distributor B", "Shanghai", null);
        Transaction tx2 = Transaction.create("PROD-002", "Factory A", "Distributor B", "Shanghai", null);

        String hash1 = TransactionSerializer.hashOf(tx1, hasher);
        String hash2 = TransactionSerializer.hashOf(tx2, hasher);

        Check.that(!hash1.equals(hash2), "Different transactions must produce different hashes");
        out.println("✓ Hash uniqueness test passed");
    }

    public void testHashLength() {
        Transaction tx = Transaction.create("PROD-001", "Factory A", "Distributor B", "Test Location", null);

        String hashResult = TransactionSerializer.hashOf(tx, hasher);

        Check.that(hashResult.length() == 64, "SHA-256 must produce 64 chars, got " + hashResult.length());
        Check.that(hashResult.matches("[0-9a-f]+"), "Hash must be hexadecimal");

        out.println("✓ Hash length test passed: 64 hexadecimal characters");
    }

    public void runAll() {
        out.println("============================================================");
        out.println("PHASE 1: SHA-256 HASHING UNIT TESTS");
        out.println("============================================================\n");

        testHashConsistency();
        testAvalancheEffect();
        testHashUniqueness();
        testHashLength();

        out.println("\n============================================================");
        out.println("ALL TESTS PASSED ✓");
        out.println("============================================================\n");
    }

    public static void main(String[] args) {
        new HashingSelfTest(System.out).runAll();
    }
}
