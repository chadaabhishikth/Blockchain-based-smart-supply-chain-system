package com.supplychain.domain.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain Layer — Manufacture Result
 * ===================================
 *
 * Typed result of a manufacturing operation, returned by
 * {@code SupplyChainService#manufactureProduct}.
 */
public final class ManufactureResult {

    private final List<String> productIds;
    private final int blockIndex;
    private final String blockHash;
    private final String merkleRoot;
    private final String timestamp;

    public ManufactureResult(List<String> productIds, int blockIndex,
                             String blockHash, String merkleRoot, String timestamp) {
        this.productIds = new ArrayList<>(productIds);
        this.blockIndex = blockIndex;
        this.blockHash = blockHash;
        this.merkleRoot = merkleRoot;
        this.timestamp = timestamp;
    }

    /**
     * @return number of products created in this batch
     */
    public int getProductsCreated() {
        return productIds.size();
    }

    public List<String> getProductIds() {
        return Collections.unmodifiableList(productIds);
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
