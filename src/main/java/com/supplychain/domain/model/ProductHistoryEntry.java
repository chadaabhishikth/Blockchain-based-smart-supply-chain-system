package com.supplychain.domain.model;

/**
 * Domain Layer — Product History Entry
 * ======================================
 *
 * One provenance record for a product: the custody event plus the block
 * in which it was committed. Produced by
 * {@code Blockchain#getProductHistory(String)} and consumed by the
 * service layer's verification and provenance reports.
 *
 * Every entry carries {@code verified = true} — its presence in a
 * validated block is exactly what proves it.
 */
public final class ProductHistoryEntry {

    private final int blockIndex;
    private final String blockHash;
    private final String timestamp;
    private final String sender;
    private final String receiver;
    private final String location;
    private final boolean verified;

    public ProductHistoryEntry(int blockIndex, String blockHash, String timestamp,
                               String sender, String receiver, String location) {
        this.blockIndex = blockIndex;
        this.blockHash = blockHash;
        this.timestamp = timestamp;
        this.sender = sender;
        this.receiver = receiver;
        this.location = location;
        this.verified = true;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getLocation() {
        return location;
    }

    public boolean isVerified() {
        return verified;
    }
}
