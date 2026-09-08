package com.supplychain.selftest;

import java.io.PrintStream;

/**
 * Self-Test Layer — Test Suite Aggregator
 * ========================================
 *
 * Runs every phase's self tests in order. Invoked by the application
 * menu (option 1); each phase can also be run individually via its own
 * {@code main} method.
 */
public final class SelfTestSuite {

    private SelfTestSuite() {
        // Static utility class — no instances.
    }

    /**
     * Run all phase self tests.
     *
     * @param out destination for the test report
     */
    public static void runAll(PrintStream out) {
        new HashingSelfTest(out).runAll();
        new MerkleSelfTest(out).runAll();
        new LedgerSelfTest(out).runAll();
        new SupplyChainSelfTest(out).runAll();
    }
}
