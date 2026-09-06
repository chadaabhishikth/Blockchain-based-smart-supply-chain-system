package com.supplychain.domain.merkle;

/**
 * Domain Layer — Merkle Side
 * ===========================
 *
 * The side of a concatenation at which a proof's sibling hash sits
 * during Merkle proof verification.
 */
public enum MerkleSide {
    LEFT, RIGHT
}
