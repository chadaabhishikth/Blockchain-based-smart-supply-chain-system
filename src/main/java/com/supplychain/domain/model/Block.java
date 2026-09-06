package com.supplychain.domain.model;

import com.supplychain.crypto.HashFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain Layer — Block (Entity)
 * ===============================
 *
 * A single immutable block in the blockchain. Contains all supply chain
 * transactions committed together, plus the cryptographic links that
 * make the chain tamper-evident:
 *
 * - index:        position of the block in the chain
 * - timestamp:    creation time
 * - transactions: the committed custody events
 * - merkleRoot:   hash summarizing all transactions (tamper evidence)
 * - previousHash: hash of the preceding block (chains the ledger)
 * - nonce:        proof-of-work counter (reserved for mining)
 * - hash:         SHA-256 fingerprint of this entire block
 *
 * Modifying any of these — even one character — changes the hash and
 * invalidates every subsequent block.
 */
public final class Block {

    private final int index;
    private final String timestamp;
    private final List<Transaction> transactions;
    private final String merkleRoot;
    private final String previousHash;
    private final int nonce;
    private final String hash;

    /** Hash function retained so the block's integrity can be re-verified. */
    private final HashFunction hasher;

    public Block(int index, String timestamp, List<Transaction> transactions,
                 String merkleRoot, String previousHash, int nonce, HashFunction hasher) {
        this.index = index;
        this.timestamp = timestamp;
        this.transactions = new ArrayList<>(transactions);
        this.merkleRoot = merkleRoot;
        this.previousHash = previousHash;
        this.nonce = nonce;
        this.hasher = hasher;
        this.hash = calculateHash();
    }

    /**
     * Recompute the SHA-256 hash of this block.
     *
     * The digest covers index, timestamp, Merkle root, previous hash and
     * nonce — any change to them yields a completely different hash.
     */
    public String calculateHash() {
        String blockData = index + timestamp + merkleRoot + previousHash + nonce;
        return hasher.hash(blockData);
    }

    public int getIndex() {
        return index;
    }

    public String getTimestamp() {
        return timestamp;
    }

    /**
     * The committed transactions (unmodifiable).
     */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public int getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    /**
     * Map representation of this block (transactions included as maps).
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("index", index);
        map.put("timestamp", timestamp);

        List<Map<String, Object>> txMaps = new ArrayList<>();
        for (Transaction tx : transactions) {
            txMaps.add(tx.toMap());
        }
        map.put("transactions", txMaps);
        map.put("merkleRoot", merkleRoot);
        map.put("previousHash", previousHash);
        map.put("nonce", nonce);
        map.put("hash", hash);
        return map;
    }

    @Override
    public String toString() {
        String shortHash = hash.length() > 16 ? hash.substring(0, 16) : hash;
        return String.format("Block{#%d, txs=%d, hash=%s...}", index, transactions.size(), shortHash);
    }
}
