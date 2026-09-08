package com.supplychain.domain.merkle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the Merkle tree data structure.
 */
class MerkleTreeTest {

    @Test
    void buildsTreeWithLogarithmicDepth() {
        MerkleTree tree = new MerkleTree(Arrays.asList("hash1", "hash2", "hash3", "hash4"));

        assertNotNull(tree.getMerkleRoot());
        assertEquals(64, tree.getMerkleRoot().length());
        assertEquals(3, tree.getTreeDepth());
    }

    @Test
    void provesMembershipForEveryLeaf() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            data.add("hash" + i);
        }
        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        for (String leaf : data) {
            List<MerkleProofElement> proof = tree.generateProof(leaf);
            assertTrue(proof.size() <= 4, "proof must be O(log n)");
            assertTrue(tree.verifyProof(leaf, proof, root), "proof for " + leaf + " must verify");
        }
    }

    @Test
    void rejectsForgedLeaf() {
        MerkleTree tree = new MerkleTree(Arrays.asList("hash1", "hash2"));
        List<MerkleProofElement> proof = tree.generateProof("hash1");

        assertFalse(tree.verifyProof("hash2", proof, tree.getMerkleRoot()));
    }

    @Test
    void supportsSingleLeaf() {
        MerkleTree tree = new MerkleTree(Collections.singletonList("single_hash"));
        List<MerkleProofElement> proof = tree.generateProof("single_hash");

        assertTrue(proof.isEmpty());
        assertTrue(tree.verifyProof("single_hash", proof, tree.getMerkleRoot()));
    }

    @Test
    void supportsOddLeafCounts() {
        List<String> data = Arrays.asList("hash1", "hash2", "hash3");
        MerkleTree tree = new MerkleTree(data);
        String root = tree.getMerkleRoot();

        for (String leaf : data) {
            assertTrue(tree.verifyProof(leaf, tree.generateProof(leaf), root));
        }
    }

    @Test
    void sameDataProducesSameRoot() {
        List<String> data = Arrays.asList("hash1", "hash2", "hash3", "hash4");

        assertEquals(new MerkleTree(data).getMerkleRoot(), new MerkleTree(data).getMerkleRoot());
    }

    @Test
    void differentDataProducesDifferentRoot() {
        assertNotEquals(
                new MerkleTree(Arrays.asList("a", "b")).getMerkleRoot(),
                new MerkleTree(Arrays.asList("a", "c")).getMerkleRoot());
    }
}
