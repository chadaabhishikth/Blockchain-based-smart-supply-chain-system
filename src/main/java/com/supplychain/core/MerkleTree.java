package com.supplychain.core;

import java.util.*;

/**
 * Phase 2: Merkle Tree Implementation
 * =====================================
 * This class implements the Merkle Tree data structure for efficient
 * transaction verification in our supply chain blockchain.
 *
 * A Merkle Tree is a binary hash tree where:
 * - LEAVES: Individual transaction hashes
 * - INTERNAL NODES: Hash of concatenated child nodes
 * - ROOT: Single hash representing all transactions (Merkle Root)
 *
 * COMPLEXITY ANALYSIS:
 * - Building the tree: O(n) time
 * - Verification of single transaction: O(log n) time
 * - Storage: O(n) where n is number of transactions
 *
 * This enables lightweight nodes to verify product authenticity without
 * downloading the entire transaction history.
 */
public class MerkleTree {

    /**
     * Node in the Merkle Tree.
     *
     * Each node holds:
     * - hash: The cryptographic hash of this node's data
     * - left: Reference to left child (for internal nodes or hash pairs)
     * - right: Reference to right child (null for odd-numbered leaf nodes)
     */
    public static class MerkleNode {
        public String hash;
        public MerkleNode left;
        public MerkleNode right;

        public MerkleNode(String hash, MerkleNode left, MerkleNode right) {
            this.hash = hash;
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "MerkleNode{" +
                    "hash='" + (hash.length() > 16 ? hash.substring(0, 16) + "..." : hash) + "'}";
        }
    }

    private List<String> data;  // Original data (leaf hashes)
    private MerkleNode root;

    /**
     * Initialize Merkle Tree with leaf data.
     *
     * @param data List of strings (transaction hashes) to build tree from
     */
    public MerkleTree(List<String> data) {
        this.data = data;
        if (data != null && !data.isEmpty()) {
            buildTree();
        }
    }

    /**
     * Build the complete Merkle Tree from the transaction list.
     *
     * ALGORITHM:
     * 1. Start with leaf hashes
     * 2. Pair adjacent leaves and hash them to create parent nodes
     * 3. Repeat recursively until single root remains
     *
     * TIME COMPLEXITY: O(n) where n is number of leaves
     */
    public void buildTree() {
        if (data == null || data.isEmpty()) {
            root = null;
            return;
        }

        // Convert data to list of nodes
        List<MerkleNode> currentLevel = new ArrayList<>();
        for (String d : data) {
            currentLevel.add(new MerkleNode(d, null, null));
        }

        // Build tree bottom-up
        while (currentLevel.size() > 1) {
            List<MerkleNode> nextLevel = new ArrayList<>();

            for (int i = 0; i < currentLevel.size(); i += 2) {
                MerkleNode left = currentLevel.get(i);
                // If odd number, duplicate the last node
                MerkleNode right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;

                // Hash the combination of children
                String combined = left.hash + right.hash;
                String parentHash = HashUtils.calculateSHA256(combined);

                MerkleNode parent = new MerkleNode(parentHash, left, right);
                nextLevel.add(parent);
            }

            currentLevel = nextLevel;
        }

        root = currentLevel.get(0);
    }

    /**
     * Get the Merkle Root - the single hash representing all data.
     *
     * @return 64-character hex string (SHA-256 hash) or null if tree is empty
     */
    public String getMerkleRoot() {
        return root != null ? root.hash : null;
    }

    /**
     * Generate a Merkle Proof for verifying a specific hash.
     *
     * A proof consists of sibling hashes that, when combined with the
     * target hash up to the root, should recreate the Merkle root.
     *
     * COMPLEXITY: O(log n) - we only traverse one path to the root
     *
     * @param targetHash The hash to verify
     * @return List of tuples (siblingHash, position) where position indicates
     *         whether the sibling is to the 'left' or 'right'
     */
    public List<ProofElement> generateProof(String targetHash) {
        List<ProofElement> proof = new ArrayList<>();

        if (root == null) {
            return proof;
        }

        findAndBuildProof(root, targetHash, proof);
        return proof;
    }

    /**
     * Helper class for proof elements.
     */
    public static class ProofElement {
        public String siblingHash;
        public String position;  // "left" or "right"

        public ProofElement(String siblingHash, String position) {
            this.siblingHash = siblingHash;
            this.position = position;
        }

        @Override
        public String toString() {
            return "(" + (siblingHash.length() > 16 ? siblingHash.substring(0, 16) + "..." : siblingHash) +
                    ", " + position + ")";
        }
    }

    /**
     * Find the target node and build proof by tracking the path.
     */
    private boolean findAndBuildProof(MerkleNode node, String target, List<ProofElement> proof) {
        if (node == null) {
            return false;
        }

        // Leaf node
        if (node.left == null && node.right == null) {
            return node.hash.equals(target);
        }

        // Internal node - search children
        if (node.left != null) {
            if (findAndBuildProof(node.left, target, proof)) {
                // Found in left subtree
                // Add right sibling (or left if no right exists - duplicate case)
                MerkleNode sibling = node.right != null ? node.right : node.left;
                proof.add(new ProofElement(sibling.hash, "right"));
                return true;
            }
        }

        if (node.right != null) {
            if (findAndBuildProof(node.right, target, proof)) {
                // Found in right subtree
                // Add left sibling
                proof.add(new ProofElement(node.left.hash, "left"));
                return true;
            }
        }

        return false;
    }

    /**
     * Verify that a target hash is part of the tree with given root.
     *
     * Takes the target hash and sequentially combines it with proof
     * hashes (according to their positions) until reaching the root.
     * If we arrive at the same rootHash, the proof is valid.
     *
     * COMPLEXITY: O(log n) - only processes proof path
     *
     * @param targetHash The hash to verify
     * @param proof List of proof elements from generateProof()
     * @param rootHash The expected Merkle root
     * @return true if the proof is valid and target is in the tree
     */
    public static boolean verifyProof(String targetHash, List<ProofElement> proof, String rootHash) {
        String currentHash = targetHash;

        for (ProofElement element : proof) {
            String combined;
            if ("left".equals(element.position)) {
                // Target is on right, sibling is on left
                combined = element.siblingHash + currentHash;
            } else {
                // Target is on left, sibling is on right
                combined = currentHash + element.siblingHash;
            }

            currentHash = HashUtils.calculateSHA256(combined);
        }

        return currentHash.equals(rootHash);
    }

    /**
     * Calculate the depth of the tree.
     *
     * @return Number of levels in the tree (1 for single node)
     */
    public int getTreeDepth() {
        return depth(root);
    }

    private int depth(MerkleNode node) {
        if (node == null) {
            return 0;
        }
        if (node.left == null && node.right == null) {
            return 1;
        }
        return 1 + Math.max(depth(node.left), depth(node.right));
    }

    // =========================================================================
    // UNIT TESTS FOR MERKLE TREE
    // =========================================================================

    public static void testMerkleTreeConstruction() {
        // Test with 4 leaves
        List<String> data = Arrays.asList("hash1", "hash2", "hash3", "hash4");
        MerkleTree tree = new MerkleTree(data);

        assert tree.getMerkleRoot() != null : "Tree must have a root";
        assert tree.getMerkleRoot().length() == 64 : "Root must be SHA-256 hash";

        // Verify tree depth is log2(n) + 1
        int depth = tree.getTreeDepth();
        assert depth == 3 : "Expected depth 3 for 4 leaves, got " + depth;

        System.out.println("✓ Merkle root: " + tree.getMerkleRoot().substring(0, 32) + "...");
        System.out.println("✓ Tree depth: " + depth);
    }

    public static void testProofGenerationAndVerification() {
        // Test with 8 items (power of 2)
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            data.add("hash" + i);
        }

        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        // Verify each item
        for (int i = 0; i < data.size(); i++) {
            String hashVal = data.get(i);
            List<ProofElement> proof = tree.generateProof(hashVal);

            // Verify proof
            boolean isValid = MerkleTree.verifyProof(hashVal, proof, root);
            assert isValid : "Proof verification failed for hash" + i;

            // Verify proof length is O(log n)
            assert proof.size() <= 4 : "Proof too long: " + proof.size() + " (expected ~3 for 8 items)";
        }

        System.out.println("✓ All 8 transactions verified with proof length O(log n)");
        System.out.println("  Proof length for first item: " + tree.generateProof(data.get(0)).size());
    }

    public static void testInvalidProofRejection() {
        List<String> data = Arrays.asList("hash1", "hash2");
        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        // Get proof for first hash
        List<ProofElement> proof = tree.generateProof("hash1");

        // Try to verify a different hash against the proof
        boolean isValid = MerkleTree.verifyProof("hash2", proof, root);
        assert !isValid : "Different hash must be rejected";

        System.out.println("✓ Tampered hash correctly rejected");
    }

    public static void testSingleItem() {
        List<String> data = Collections.singletonList("single_hash");
        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        List<ProofElement> proof = tree.generateProof("single_hash");
        assert MerkleTree.verifyProof("single_hash", proof, root) : "Single item must work";
        assert proof.isEmpty() : "Single node needs no proof path";

        System.out.println("✓ Single item tree handled correctly");
    }

    public static void testOddNumberOfItems() {
        List<String> data = Arrays.asList("hash1", "hash2", "hash3");
        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        // All items should verify
        for (String d : data) {
            List<ProofElement> proof = tree.generateProof(d);
            assert MerkleTree.verifyProof(d, proof, root) : d + " should verify";
        }

        System.out.println("✓ Odd number of items handled (duplicate last node)");
    }

    public static void testBatchVerification() {
        // Large batch: 1000 items
        int batchSize = 1000;
        List<String> data = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            data.add(String.format("hash%04d", i));
        }

        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        // Verify entire batch
        int successfulVerifications = 0;
        for (String d : data) {
            List<ProofElement> proof = tree.generateProof(d);
            if (MerkleTree.verifyProof(d, proof, root)) {
                successfulVerifications++;
            }
        }

        assert successfulVerifications == batchSize : "All items must verify";

        int proofLen = tree.generateProof(data.get(500)).size();
        System.out.println("✓ Batch verification: " + batchSize + " items verified at O(log n) each");
        System.out.println("  Proof length for " + batchSize + " items: " + proofLen + " (log2(1000) ≈ 10)");
    }

    public static void testMerkleRootConsistency() {
        List<String> data1 = Arrays.asList("hash1", "hash2", "hash3", "hash4");

        MerkleTree tree1 = new MerkleTree(data1);
        MerkleTree tree2 = new MerkleTree(data1);

        assert tree1.getMerkleRoot().equals(tree2.getMerkleRoot()) : "Same data must produce same root";

        System.out.println("✓ Merkle root is deterministic");
    }

    /**
     * Run all Merkle Tree tests.
     */
    public static void runAllTests() {
        System.out.println("============================================================");
        System.out.println("PHASE 2: MERKLE TREE UNIT TESTS");
        System.out.println("============================================================\n");

        testMerkleTreeConstruction();
        System.out.println();
        testProofGenerationAndVerification();
        System.out.println();
        testInvalidProofRejection();
        System.out.println();
        testSingleItem();
        System.out.println();
        testOddNumberOfItems();
        System.out.println();
        testBatchVerification();
        System.out.println();
        testMerkleRootConsistency();

        System.out.println("\n============================================================");
        System.out.println("ALL TESTS PASSED ✓");
        System.out.println("============================================================\n");
    }

    public static void main(String[] args) {
        runAllTests();
    }
}
