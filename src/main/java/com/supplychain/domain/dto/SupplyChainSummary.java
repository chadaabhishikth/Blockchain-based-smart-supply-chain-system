package com.supplychain.domain.dto;

/**
 * Domain Layer — Supply Chain Summary
 * ====================================
 *
 * Aggregate statistics of the whole supply chain, returned by
 * {@code SupplyChainService#getSummary}.
 */
public final class SupplyChainSummary {

    private final int totalProducts;
    private final int activeProducts;
    private final int soldProducts;
    private final int registeredManufacturers;
    private final int totalBlocks;
    private final int pendingTransactions;
    private final boolean blockchainValid;

    public SupplyChainSummary(int totalProducts, int activeProducts, int soldProducts,
                              int registeredManufacturers, int totalBlocks,
                              int pendingTransactions, boolean blockchainValid) {
        this.totalProducts = totalProducts;
        this.activeProducts = activeProducts;
        this.soldProducts = soldProducts;
        this.registeredManufacturers = registeredManufacturers;
        this.totalBlocks = totalBlocks;
        this.pendingTransactions = pendingTransactions;
        this.blockchainValid = blockchainValid;
    }

    public int getTotalProducts() {
        return totalProducts;
    }

    public int getActiveProducts() {
        return activeProducts;
    }

    public int getSoldProducts() {
        return soldProducts;
    }

    public int getRegisteredManufacturers() {
        return registeredManufacturers;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getPendingTransactions() {
        return pendingTransactions;
    }

    public boolean isBlockchainValid() {
        return blockchainValid;
    }
}
