package com.supplychain.domain.merkle;

/**
 * Domain Layer — Merkle Tree Node
 * ================================
 *
 * A node in a Merkle tree. Leaf nodes carry the hash of one transaction;
 * internal nodes carry the hash of their concatenated child hashes.
 *
 * COMPLEXITY: a tree with n leaves contains O(n) nodes in total, and
 * any leaf is at most O(log n) edges from the root.
 */
public final class MerkleNode {

    private final String hash;
    private final MerkleNode left;
    private final MerkleNode right;

    public MerkleNode(String hash, MerkleNode left, MerkleNode right) {
        this.hash = hash;
        this.left = left;
        this.right = right;
    }

    public String getHash() {
        return hash;
    }

    public MerkleNode getLeft() {
        return left;
    }

    public MerkleNode getRight() {
        return right;
    }

    /**
     * True for leaves — the transaction-hash nodes at the bottom of the tree.
     */
    public boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public String toString() {
        String shortHash = hash.length() > 16 ? hash.substring(0, 16) + "..." : hash;
        return "MerkleNode{hash='" + shortHash + "'}";
    }
}
