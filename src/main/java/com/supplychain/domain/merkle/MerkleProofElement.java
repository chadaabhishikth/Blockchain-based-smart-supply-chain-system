package com.supplychain.domain.merkle;

/**
 * Domain Layer — Merkle Proof Element
 * =====================================
 *
 * One step of a Merkle proof. To prove that a leaf belongs to a tree,
 * the verifier walks from the leaf to the root, and at every level needs
 * exactly one piece of information: the hash of the sibling node and on
 * which side of the concatenation it sits.
 *
 * A proof for n leaves therefore contains at most ⌈log2(n)⌉ elements —
 * this is what makes verification O(log n).
 */
public final class MerkleProofElement {

    private final String siblingHash;
    private final MerkleSide side;

    public MerkleProofElement(String siblingHash, MerkleSide side) {
        this.siblingHash = siblingHash;
        this.side = side;
    }

    public String getSiblingHash() {
        return siblingHash;
    }

    public MerkleSide getSide() {
        return side;
    }

    @Override
    public String toString() {
        String shortHash = siblingHash.length() > 16 ? siblingHash.substring(0, 16) + "..." : siblingHash;
        return "(" + shortHash + ", " + side + ")";
    }
}
