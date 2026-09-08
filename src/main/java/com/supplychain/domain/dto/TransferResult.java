package com.supplychain.domain.dto;

/**
 * Domain Layer — Transfer Result
 * ===============================
 *
 * Typed result of an ownership transfer (including the final sale to a
 * consumer). Distinguishes hard failures (unknown product, wrong
 * sender) from successful transfers, and optionally carries the full
 * provenance report for sales.
 */
public final class TransferResult {

    private final boolean success;
    private final String error;
    private final String message;
    private final boolean counterfeit;

    private final String productId;
    private final String from;
    private final String to;
    private final String location;
    private final String stage;
    private final int blockIndex;
    private final String transactionHash;
    private final String timestamp;

    private final Provenance provenance;

    private TransferResult(boolean success, String error, String message, boolean counterfeit,
                           String productId, String from, String to, String location, String stage,
                           int blockIndex, String transactionHash, String timestamp, Provenance provenance) {
        this.success = success;
        this.error = error;
        this.message = message;
        this.counterfeit = counterfeit;
        this.productId = productId;
        this.from = from;
        this.to = to;
        this.location = location;
        this.stage = stage;
        this.blockIndex = blockIndex;
        this.transactionHash = transactionHash;
        this.timestamp = timestamp;
        this.provenance = provenance;
    }

    /**
     * A failed transfer.
     *
     * @param error       machine-readable error code
     * @param message     human-readable explanation
     * @param counterfeit whether the failure indicates a probable counterfeit
     */
    public static TransferResult failure(String error, String message, boolean counterfeit) {
        return new TransferResult(false, error, message, counterfeit,
                null, null, null, null, null, -1, null, null, null);
    }

    /**
     * A successful transfer.
     */
    public static TransferResult success(String productId, String from, String to, String location,
                                         String stage, int blockIndex, String transactionHash, String timestamp) {
        return new TransferResult(true, null, null, false,
                productId, from, to, location, stage, blockIndex, transactionHash, timestamp, null);
    }

    /**
     * Derive a copy of this result with a provenance report attached.
     */
    public TransferResult withProvenance(Provenance provenance) {
        return new TransferResult(success, error, message, counterfeit,
                productId, from, to, location, stage, blockIndex, transactionHash, timestamp, provenance);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public boolean isCounterfeit() {
        return counterfeit;
    }

    public String getProductId() {
        return productId;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getLocation() {
        return location;
    }

    public String getStage() {
        return stage;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Provenance getProvenance() {
        return provenance;
    }
}
