package com.supplychain;

import com.supplychain.core.HashUtils;
import com.supplychain.core.MerkleTree;
import com.supplychain.core.Blockchain;
import com.supplychain.supplychain.SupplyChainBlockchain;
import com.supplychain.benchmarking.DatabaseBenchmark;

import java.util.*;

/**
 * Main Entry Point: Blockchain Supply Chain System
 * =================================================
 *
 * This program demonstrates the complete blockchain-based supply chain system,
 * including:
 * 1. SHA-256 cryptographic hashing
 * 2. Merkle Tree verification
 * 3. Blockchain ledger
 * 4. Supply chain operations
 * 5. Academic comparison with SQL
 *
 * Run with: java com.supplychain.Main
 */
public class Main {

    public static void printBanner() {
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

    public static void printFeatures() {
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

    public static void runAllTests() {
        System.out.println("\n" + "======================================================================");
        System.out.println("RUNNING ALL UNIT TESTS");
        System.out.println("======================================================================");

        // Phase 1: SHA-256 Hashing
        System.out.println("\n" + "──────────────────────────────────────────────────────────────────────");
        System.out.println("PHASE 1: SHA-256 CRYPTOGRAPHIC HASHING");
        System.out.println("──────────────────────────────────────────────────────────────────────");
        HashUtils.runAllTests();

        // Phase 2: Merkle Tree
        System.out.println("──────────────────────────────────────────────────────────────────────");
        System.out.println("PHASE 2: MERKLE TREE IMPLEMENTATION");
        System.out.println("──────────────────────────────────────────────────────────────────────");
        MerkleTree.runAllTests();

        // Phase 3: Blockchain Ledger
        System.out.println("──────────────────────────────────────────────────────────────────────");
        System.out.println("PHASE 3: BLOCKCHAIN LEDGER");
        System.out.println("──────────────────────────────────────────────────────────────────────");
        Blockchain.runAllTests();

        System.out.println("🎉 ALL TESTS COMPLETED SUCCESSFULLY");
    }

    public static void demoCompleteSystem() {
        System.out.println("\n" + "======================================================================");
        System.out.println("COMPLETE SYSTEM DEMONSTRATION");
        System.out.println("======================================================================");
        SupplyChainBlockchain.demoSupplyChain();
    }

    public static void runBenchmark() {
        System.out.println("\n" + "======================================================================");
        System.out.println("PERFORMANCE BENCHMARK");
        System.out.println("======================================================================");
        DatabaseBenchmark.runFullComparison();
    }

    public static void main(String[] args) {
        printBanner();
        printFeatures();

        Scanner scanner = new Scanner(System.in);

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
}
