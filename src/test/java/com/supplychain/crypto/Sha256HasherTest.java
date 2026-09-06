package com.supplychain.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the SHA-256 hash function.
 */
class Sha256HasherTest {

    private final Sha256Hasher hasher = new Sha256Hasher();

    @Test
    void producesKnownSha256Digest() {
        // Well-known test vector: SHA-256("abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                hasher.hash("abc"));
    }

    @Test
    void isDeterministic() {
        assertEquals(hasher.hash("supply-chain"), hasher.hash("supply-chain"));
    }

    @Test
    void produces64LowercaseHexCharacters() {
        assertTrue(hasher.hash("anything").matches("[0-9a-f]{64}"));
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        assertNotEquals(hasher.hash("input-1"), hasher.hash("input-2"));
    }
}
