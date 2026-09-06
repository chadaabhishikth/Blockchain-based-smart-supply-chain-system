package com.supplychain.app;

import com.supplychain.app.demo.SupplyChainDemo;
import com.supplychain.benchmark.DatabaseBenchmark;
import com.supplychain.selftest.SelfTestSuite;

import java.util.Scanner;

/**
 * Application Layer — Console Application (Composition Root)
 * ===========================================================
 *
 * Owns the user-facing console flow and is the single place where the
 * concrete implementations are wired together:
 *
 *   HashFunction  = Sha256Hasher
 *   Blockchain    = in-memory blockchain over Sha256Hasher
 *   Service       = SupplyChainService over that blockchain
 *
 * The application layer may depend on the domain, crypto, benchmark and
 * self-test layers — never the other way around.
 *
 * Menu:
 *   1. Run all unit tests (self-test layer)
 *   2. Demo complete supply chain system (demo layer)
 *   3. Run performance benchmark (benchmark layer)
 *   4. Run everything
 *   5. Exit
 */
public final class SupplyChainApplication {

    private final Scanner scanner = new Scanner(System.in);

    private SupplyChainApplication() {
        // Constructed via launch().
    }

    /**
     * Launch the console application.
     */
    public static void launch(String[] args) {
        new SupplyChainApplication().run();
    }

    private void run() {
        printBanner();
        printFeatures();

        System.out.println("Select an option:");
        System.out.println("  1. Run all unit tests");
        System.out.println("  2. Demo complete supply chain system");
        System.out.println("  3. Run performance benchmark");
        System.out.println("  4. Run everything");
        System.out.println("  5. Exit");

        System.out.print("\nEnter choice (1-5): ");
        String choice = scanner.nextLine().trim();

        System.out.println();

        switch (choice) {
            case "1":
                runAllTests();
                break;
            case "2":
                demoCompleteSystem();
                break;
            case "3":
                runBenchmark();
                break;
            case "4":
                runEverything();
                break;
            case "5":
                System.out.println("\nExiting. Thank you for using the Blockchain Supply Chain System!\n");
                break;
            default:
                System.out.println("\nInvalid choice. Please enter 1, 2, 3, 4, or 5.");
                break;
        }

        scanner.close();
    }

    private void runAllTests() {
        System.out.println("\n" + "======================================================================");
        System.out.println("RUNNING ALL UNIT TESTS");
        System.out.println("======================================================================");

        SelfTestSuite.runAll(System.out);

        System.out.println("🎉 ALL TESTS COMPLETED SUCCESSFULLY");
    }

    private void demoCompleteSystem() {
        System.out.println("\n" + "======================================================================");
        System.out.println("COMPLETE SYSTEM DEMONSTRATION");
        System.out.println("======================================================================");
        new SupplyChainDemo().run();
    }

    private void runBenchmark() {
        System.out.println("\n" + "======================================================================");
        System.out.println("PERFORMANCE BENCHMARK");
        System.out.println("======================================================================");
        DatabaseBenchmark.runFullComparison();
    }

    private void runEverything() {
        System.out.println("\n" + "======================================================================");
        System.out.println("FULL SYSTEM VALIDATION");
        System.out.println("======================================================================");

        System.out.println("\n[1/3] Running unit tests...");
        runAllTests();

        System.out.println("\n[2/3] Running system demonstration...");
        demoCompleteSystem();

        System.out.println("\n[3/3] Running performance benchmark...");
        runBenchmark();

        System.out.println("\n" + "======================================================================");
        System.out.println("🎉 FULL SYSTEM VALIDATION COMPLETE");
        System.out.println("======================================================================");
        System.out.println();
        System.out.println("All components are working correctly:");
        System.out.println("✓ Cryptographic hashing foundation");
        System.out.println("✓ Merkle Tree verification");
        System.out.println("✓ Blockchain ledger");
        System.out.println("✓ Supply chain operations");
        System.out.println("✓ Performance benchmarking");
    }

    private void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                              ║");
        System.out.println("║     ███████╗███╗   ██╗███████╗███╗   ███╗██╗███████╗███████╗                   ║");
        System.out.println("║     ██╔════╝████╗  ██║██╔════╝████╗ ████║██║██╔════╝██╔════╝                   ║");
        System.out.println("║     ███████╗██╔██╗ ██║█████╗  ██╔████╔██║██║█████╗  ███████╗                   ║");
        System.out.println("║     ╚════██║██║╚██╗██║██╔══╝  ██║╚██╔╝██║██║██╔══╝  ╚════██║                   ║");
        System.out.println("║     ███████║██║ ╚████║███████╗██║ ╚═╝ ██║██║███████╗███████║                   ║");
        System.out.println("║     ╚══════╝╚═╝  ╚═══╝╚══════╝╚═╝     ╚═╝╚═╝╚══════╝╚══════╝                   ║");
        System.out.println("║                                                                              ║");
        System.out.println("║     S M A R T   S U P P L Y   C H A I N   S Y S T E M                        ║");
        System.out.println("║                                                                              ║");
        System.out.println("║     Blockchain-Powered Product Provenance & Authentication                   ║");
        System.out.println("║                                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printFeatures() {
        System.out.println("This system implements a complete blockchain-based supply chain solution");
        System.out.println("with the following key features:");
        System.out.println();
        System.out.println("✓ SHA-256 Cryptographic Hashing");
        System.out.println("  - Digital fingerprints for all transactions");
        System.out.println("  - Avalanche effect for tamper detection");
        System.out.println();
        System.out.println("✓ Merkle Trees");
        System.out.println("  - O(log n) verification complexity");
        System.out.println("  - Efficient batch verification");
        System.out.println("  - Space-optimized proofs");
        System.out.println();
        System.out.println("✓ Blockchain Ledger");
        System.out.println("  - Immutable transaction history");
        System.out.println("  - Cryptographic block chaining");
        System.out.println("  - Complete integrity verification");
        System.out.println();
        System.out.println("✓ Supply Chain Operations");
        System.out.println("  - Product manufacturing & tracking");
        System.out.println("  - Ownership transfer through supply chain");
        System.out.println("  - Counterfeit detection");
        System.out.println("  - Consumer verification");
        System.out.println();
        System.out.println("✓ Academic Comparison");
        System.out.println("  - Performance vs SQL databases");
        System.out.println("  - Trade-off analysis");
        System.out.println("  - Recommendations for real-world use");
        System.out.println();
    }
}
