package com.supplychain.domain.dto;

import com.supplychain.domain.merkle.MerkleProofElement;

import java.util.Collections;
import java.util.List;

/**
 * Domain Layer — Transaction Merkle Proof DTO
 * ============================================
 *
 * Represents an O(log n) cryptographic proof that a specific transaction
 * is committed inside a specific block's Merkle tree without needing to
 * transmit or store all other transactions in the block.
 *
 * This directly implements the storage-optimization requirement for
 * lightweight consumer clients (SPV model).
 */
public final class TransactionMerkleProof {

    private final String productId;
    private final String transactionHash;
    private final int blockIndex;
    private final String blockHash;
    private final String blockMerkleRoot;
    private final List<MerkleProofElement> proofElements;
    private final boolean verified;

    public TransactionMerkleProof(String productId, String transactionHash, int blockIndex,
                                  String blockHash, String blockMerkleRoot,
                                  List<MerkleProofElement> proofElements, boolean verified) {
        this.productId = productId;
        this.transactionHash = transactionHash;
        this.blockIndex = blockIndex;
        this.blockHash = blockHash;
        this.blockMerkleRoot = blockMerkleRoot;
        this.proofElements = proofElements == null ? Collections.emptyList() : Collections.unmodifiableList(proofElements);
        this.verified = verified;
    }

    public String getProductId() {
        return productId;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getBlockMerkleRoot() {
        return blockMerkleRoot;
    }

    public List<MerkleProofElement> getProofElements() {
        return proofElements;
    }

    public boolean isVerified() {
        return verified;
    }

    public int getProofLength() {
        return proofElements.size();
    }
}