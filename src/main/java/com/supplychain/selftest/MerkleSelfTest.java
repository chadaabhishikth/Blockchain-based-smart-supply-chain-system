package com.supplychain.selftest;

import com.supplychain.domain.merkle.MerkleProofElement;
import com.supplychain.domain.merkle.MerkleTree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Self-Test Layer — Phase 2: Merkle Tree
 * =======================================
 *
 * Unit tests for the Merkle tree data structure. Run from the menu
 * (option 1) or directly: java com.supplychain.selftest.MerkleSelfTest
 */
public final class MerkleSelfTest {

    private final PrintStream out;

    public MerkleSelfTest(PrintStream out) {
        this.out = out;
    }

    public void testMerkleTreeConstruction() {
        List<String> data = Arrays.asList("hash1", "hash2", "hash3", "hash4");
        MerkleTree tree = new MerkleTree(data);

        Check.that(tree.getMerkleRoot() != null, "Tree must have a root");
        Check.that(tree.getMerkleRoot().length() == 64, "Root must be SHA-256 hash");

        int depth = tree.getTreeDepth();
        Check.that(depth == 3, "Expected depth 3 for 4 leaves, got " + depth);

        out.println("✓ Merkle root: " + tree.getMerkleRoot().substring(0, 32) + "...");
        out.println("✓ Tree depth: " + depth);
    }

    public void testProofGenerationAndVerification() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            data.add("hash" + i);
        }

        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        for (int i = 0; i < data.size(); i++) {
            String hashVal = data.get(i);
            List<MerkleProofElement> proof = tree.generateProof(hashVal);

            boolean isValid = tree.verifyProof(hashVal, proof, root);
            Check.that(isValid, "Proof verification failed for hash" + i);

            Check.that(proof.size() <= 4, "Proof too long: " + proof.size() + " (expected ~3 for 8 items)");
        }

        out.println("✓ All 8 transactions verified with proof length O(log n)");
        out.println("  Proof length for first item: " + tree.generateProof(data.get(0)).size());
    }

    public void testInvalidProofRejection() {
        List<String> data = Arrays.asList("hash1", "hash2");
        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        List<MerkleProofElement> proof = tree.generateProof("hash1");

        boolean isValid = tree.verifyProof("hash2", proof, root);
        Check.that(!isValid, "Different hash must be rejected");

        out.println("✓ Tampered hash correctly rejected");
    }

    public void testSingleItem() {
        List<String> data = Collections.singletonList("single_hash");
        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        List<MerkleProofElement> proof = tree.generateProof("single_hash");
        Check.that(tree.verifyProof("single_hash", proof, root), "Single item must work");
        Check.that(proof.isEmpty(), "Single node needs no proof path");

        out.println("✓ Single item tree handled correctly");
    }

    public void testOddNumberOfItems() {
        List<String> data = Arrays.asList("hash1", "hash2", "hash3");
        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        for (String d : data) {
            List<MerkleProofElement> proof = tree.generateProof(d);
            Check.that(tree.verifyProof(d, proof, root), d + " should verify");
        }

        out.println("✓ Odd number of items handled (duplicate last node)");
    }

    public void testBatchVerification() {
        int batchSize = 1000;
        List<String> data = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            data.add(String.format("hash%04d", i));
        }

        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        int successfulVerifications = 0;
        for (String d : data) {
            List<MerkleProofElement> proof = tree.generateProof(d);
            if (tree.verifyProof(d, proof, root)) {
                successfulVerifications++;
            }
        }

        Check.that(successfulVerifications == batchSize, "All items must verify");

        int proofLen = tree.generateProof(data.get(500)).size();
        out.println("✓ Batch verification: " + batchSize + " items verified at O(log n) each");
        out.println("  Proof length for " + batchSize + " items: " + proofLen + " (log2(1000) ≈ 10)");
    }

    public void testMerkleRootConsistency() {
        List<String> data1 = Arrays.asList("hash1", "hash2", "hash3", "hash4");

        MerkleTree tree1 = new MerkleTree(data1);
        MerkleTree tree2 = new MerkleTree(data1);

        Check.that(tree1.getMerkleRoot().equals(tree2.getMerkleRoot()), "Same data must produce same root");

        out.println("✓ Merkle root is deterministic");
    }

    public void runAll() {
        out.println("============================================================");
        out.println("PHASE 2: MERKLE TREE UNIT TESTS");
        out.println("============================================================\n");

        testMerkleTreeConstruction();
        out.println();
        testProofGenerationAndVerification();
        out.println();
        testInvalidProofRejection();
        out.println();
        testSingleItem();
        out.println();
        testOddNumberOfItems();
        out.println();
        testBatchVerification();
        out.println();
        testMerkleRootConsistency();

        out.println("\n============================================================");
        out.println("ALL TESTS PASSED ✓");
        out.println("============================================================\n");
    }

    public static void main(String[] args) {
        new MerkleSelfTest(System.out).runAll();
    }
}
