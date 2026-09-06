package com.supplychain;

import com.supplychain.app.SupplyChainApplication;

/**
 * Entry Point: Blockchain Supply Chain System
 * ============================================
 *
 * Deliberately thin — it only delegates to the application layer.
 * All wiring, menus and orchestration live in
 * {@link SupplyChainApplication}, keeping this class stable no matter
 * how the internals evolve.
 *
 * Run with: java com.supplychain.Main
 * (or: mvn exec:java)
 */
public final class Main {

    private Main() {
        // No instances — static entry point only.
    }

    public static void main(String[] args) {
        SupplyChainApplication.launch(args);
    }
}
