package com.supplychain.crypto;

/**
 * Crypto Layer — Hash Function Abstraction
 * =========================================
 *
 * Abstracts the cryptographic hash algorithm away from the domain logic.
 * The domain layer (Merkle trees, blockchain) depends only on this
 * interface, so the underlying algorithm can be swapped (SHA-256,
 * SHA3-256, BLAKE2, ...) without touching any domain code.
 *
 * COMPLEXITY: implementations are expected to be O(1) time and space.
 */
public interface HashFunction {

    /**
     * Hash an arbitrary string and return its hexadecimal digest.
     *
     * @param data the input to hash
     * @return hexadecimal string representation of the digest
     */
    String hash(String data);
}
