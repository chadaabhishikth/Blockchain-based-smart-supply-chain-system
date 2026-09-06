/**
 * Crypto Layer
 * =============
 * Cryptographic primitives: the {@code HashFunction} abstraction, the
 * SHA-256 implementation and the canonical transaction serializer.
 * The domain layer depends only on the abstraction, so the algorithm
 * can be swapped without touching business logic.
 */
package com.supplychain.crypto;
