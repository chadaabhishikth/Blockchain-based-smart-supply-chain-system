package com.supplychain.domain.ledger;

import com.supplychain.domain.model.ProductHistoryEntry;
import com.supplychain.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the blockchain ledger.
 */
class BlockchainTest {

    @Test
    void startsWithGenesisBlock() {
        Blockchain chain = new Blockchain();

        assertEquals(1, chain.getChainLength());
        assertEquals(0, chain.getBlock(0).getIndex());
        assertEquals(Blockchain.GENESIS_PREVIOUS_HASH, chain.getBlock(0).getPreviousHash());
    }

    @Test
    void linksBlocksCryptographically() {
        Blockchain chain = new Blockchain();

        chain.addBlock(Collections.singletonList(
                Transaction.create("PROD-001", "Factory", "Distributor", "Shanghai", null)));
        chain.addBlock(Collections.singletonList(
                Transaction.create("PROD-001", "Distributor", "Retailer", "Beijing", null)));

        assertEquals(3, chain.getChainLength());
        assertEquals(chain.getBlock(1).getHash(), chain.getBlock(2).getPreviousHash());
    }

    @Test
    void validatesIntactChain() {
        Blockchain chain = new Blockchain();
        for (int i = 0; i < 5; i++) {
            chain.addBlock(Collections.singletonList(Transaction.create(
                    "PROD-" + i, "Factory " + i, "Dist " + i, "Loc " + i, null)));
        }

        assertTrue(chain.isValid());
    }

    @Test
    void detectsTampering() {
        Blockchain chain = new Blockchain();
        for (int i = 0; i < 5; i++) {
            chain.addBlock(Collections.singletonList(Transaction.create(
                    "PROD-" + i, "Factory " + i, "Dist " + i, "Loc " + i, null)));
        }

        chain.simulateTampering(2, 0, "Tampered Location");

        assertFalse(chain.isValid());
    }

    @Test
    void tracesProductHistory() {
        Blockchain chain = new Blockchain();
        chain.addBlock(Collections.singletonList(
                Transaction.create("PROD-X", "Factory-A", "Distributor-B", "Shanghai", null)));
        chain.addBlock(Collections.singletonList(
                Transaction.create("PROD-X", "Distributor-B", "Consumer", "Beijing", null)));
        chain.addBlock(Collections.singletonList(
                Transaction.create("PROD-Y", "Factory-Z", "Consumer", "Rome", null)));

        List<ProductHistoryEntry> history = chain.getProductHistory("PROD-X");

        assertEquals(2, history.size());
        assertEquals("Factory-A", history.get(0).getSender());
        assertEquals("Consumer", history.get(1).getReceiver());
        assertTrue(history.get(0).isVerified());
    }
}
