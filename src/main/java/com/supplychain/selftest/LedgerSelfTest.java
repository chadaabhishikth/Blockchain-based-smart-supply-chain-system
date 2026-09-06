package com.supplychain.selftest;

import com.supplychain.domain.ledger.Blockchain;
import com.supplychain.domain.model.ProductHistoryEntry;
import com.supplychain.domain.model.Transaction;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Self-Test Layer — Phase 3: Blockchain Ledger
 * =============================================
 *
 * Unit tests for the immutable ledger. Run from the menu (option 1) or
 * directly: java com.supplychain.selftest.LedgerSelfTest
 */
public final class LedgerSelfTest {

    private final PrintStream out;

    public LedgerSelfTest(PrintStream out) {
        this.out = out;
    }

    public void testGenesisBlock() {
        Blockchain chain = new Blockchain();

        Check.that(chain.getChainLength() == 1, "Should start with genesis block");
        Check.that(chain.getBlock(0).getIndex() == 0, "Genesis block index should be 0");
        Check.that(chain.getBlock(0).getPreviousHash().equals(Blockchain.GENESIS_PREVIOUS_HASH),
                "Genesis previous hash should be zeros");
        Check.that(!chain.getBlock(0).getHash().isEmpty(), "Genesis block should have a hash");

        out.println("✓ Genesis block created correctly");
    }

    public void testBlockChaining() {
        Blockchain chain = new Blockchain();

        Transaction tx1 = Transaction.create("PROD-001", "Factory", "Distributor", "Shanghai", null);
        chain.addBlock(Collections.singletonList(tx1));

        Transaction tx2 = Transaction.create("PROD-001", "Distributor", "Retailer", "Beijing", null);
        chain.addBlock(Collections.singletonList(tx2));

        Check.that(chain.getChainLength() == 3, "Should have 3 blocks (genesis + 2)");
        Check.that(chain.getBlock(2).getPreviousHash().equals(chain.getBlock(1).getHash()), "Chain link broken");

        out.println("✓ Blocks properly chained");
    }

    public void testChainValidation() {
        Blockchain chain = new Blockchain();

        for (int i = 0; i < 5; i++) {
            Transaction tx = Transaction.create(
                    String.format("PROD-%03d", i),
                    String.format("Factory %d", i),
                    String.format("Dist %d", i),
                    String.format("Loc %d", i),
                    null
            );
            chain.addBlock(Collections.singletonList(tx));
        }

        Check.that(chain.isValid(), "Valid chain should pass validation");

        // Simulate tampering: rewrite a location without updating the Merkle root
        chain.simulateTampering(2, 0, "Tampered Location");

        Check.that(!chain.isValid(), "Tampered chain should fail validation");

        out.println("✓ Chain validation detects tampering");
    }

    public void testProductHistory() {
        Blockchain chain = new Blockchain();

        List<String[]> journey = Arrays.asList(
                new String[]{"Factory-A", "Distributor-B", "Shanghai"},
                new String[]{"Distributor-B", "Wholesaler-C", "Beijing"},
                new String[]{"Wholesaler-C", "Retailer-D", "Shanghai"},
                new String[]{"Retailer-D", "Consumer", "Customer Home"}
        );

        for (String[] step : journey) {
            Transaction tx = Transaction.create("AUTHENTIC-001", step[0], step[1], step[2], null);
            chain.addBlock(Collections.singletonList(tx));
        }

        List<ProductHistoryEntry> history = chain.getProductHistory("AUTHENTIC-001");

        Check.that(history.size() == 4, "Should have 4 transactions, got " + history.size());
        Check.that("Factory-A".equals(history.get(0).getSender()), "Origin should be factory");
        Check.that("Consumer".equals(history.get(history.size() - 1).getReceiver()),
                "Final destination should be consumer");

        out.println("✓ Product journey tracked: " + history.size() + " steps");
        for (ProductHistoryEntry step : history) {
            out.println("  " + step.getSender() + " → " + step.getReceiver() + " (" + step.getLocation() + ")");
        }
    }

    public void runAll() {
        out.println("============================================================");
        out.println("PHASE 3: BLOCKCHAIN LEDGER UNIT TESTS");
        out.println("============================================================\n");

        testGenesisBlock();
        out.println();
        testBlockChaining();
        out.println();
        testChainValidation();
        out.println();
        testProductHistory();

        out.println("\n============================================================");
        out.println("ALL TESTS PASSED ✓");
        out.println("============================================================\n");
    }

    public static void main(String[] args) {
        new LedgerSelfTest(System.out).runAll();
    }
}
