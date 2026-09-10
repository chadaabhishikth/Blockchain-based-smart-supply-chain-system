package com.supplychain.domain.ledger;

import com.supplychain.crypto.HashFunction;
import com.supplychain.crypto.Sha256Hasher;
import com.supplychain.crypto.TransactionSerializer;
import com.supplychain.domain.dto.TransactionMerkleProof;
import com.supplychain.domain.merkle.MerkleProofElement;
import com.supplychain.domain.merkle.MerkleTree;
import com.supplychain.domain.model.Block;
import com.supplychain.domain.model.ProductHistoryEntry;
import com.supplychain.domain.model.Transaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain Layer — Blockchain Ledger
 * ==================================
 *
 * The immutable ledger: blocks chained together with cryptographic
 * hashes. The chain is tamper-evident because modifying any block
 * invalidates:
 *
 * 1. its own hash,
 * 2. the "previous hash" link of the next block, and
 * 3. the Merkle root binding it to its transactions.
 *
 * This class is pure domain logic: no console output except the
 * integrity failure warnings, no I/O, no presentation concerns.
 *
 * COMPLEXITY: validation is O(n) over the blocks; adding a block is
 * O(t) in its number of transactions (Merkle tree construction).
 */
public class Blockchain {

    /** Conventional all-zeros previous hash anchoring the genesis block. */
    public static final String GENESIS_PREVIOUS_HASH = "0".repeat(64);

    private final HashFunction hasher;
    private final List<Block> chain = new ArrayList<>();
    private final List<Block> backupChain = new ArrayList<>();
    private final List<Transaction> pendingTransactions = new ArrayList<>();

    public Blockchain() {
        this(new Sha256Hasher());
    }

    public Blockchain(HashFunction hasher) {
        this.hasher = hasher;
        createGenesisBlock();
    }

    /**
     * Create the first block of the chain.
     *
     * The genesis block has index 0, an all-zeros previous hash and a
     * single system-initialization transaction.
     *
     * @return the genesis block that was added to the chain
     */
    public Block createGenesisBlock() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", "genesis_block");
        metadata.put("description", "Supply Chain Network Genesis");

        Transaction genesisTx = Transaction.create("GENESIS", "SYSTEM", "NETWORK", "Block 0", metadata);

        List<String> txHashes = new ArrayList<>();
        txHashes.add(TransactionSerializer.hashOf(genesisTx, hasher));

        MerkleTree merkleTree = new MerkleTree(txHashes, hasher);

        Block genesisBlock = new Block(
                0,
                Instant.now().toString(),
                new ArrayList<>(java.util.Collections.singletonList(genesisTx)),
                merkleTree.getMerkleRoot(),
                GENESIS_PREVIOUS_HASH,
                0,
                hasher
        );

        chain.add(genesisBlock);
        return genesisBlock;
    }

    /**
     * @return the most recent block in the chain
     */
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    /**
     * Add a new block containing the given transactions.
     *
     * @param transactions transactions to commit; if null, all pending
     *                     transactions are committed instead
     * @return the newly created block
     */
    public Block addBlock(List<Transaction> transactions) {
        List<Transaction> blockTransactions = transactions;
        if (blockTransactions == null) {
            blockTransactions = new ArrayList<>(pendingTransactions);
            pendingTransactions.clear();
        }

        List<String> txHashes = new ArrayList<>();
        for (Transaction tx : blockTransactions) {
            txHashes.add(TransactionSerializer.hashOf(tx, hasher));
        }

        MerkleTree merkleTree = new MerkleTree(txHashes, hasher);

        Block newBlock = new Block(
                chain.size(),
                Instant.now().toString(),
                blockTransactions,
                merkleTree.getMerkleRoot(),
                getLatestBlock().getHash(),
                0,
                hasher
        );

        chain.add(newBlock);
        return newBlock;
    }

    /**
     * Queue a transaction for the next mined block.
     */
    public void addPendingTransaction(Transaction transaction) {
        pendingTransactions.add(transaction);
    }

    /**
     * Commit all pending transactions into a new block.
     */
    public Block minePendingTransactions() {
        return addBlock(null);
    }

    /**
     * Validate the integrity of the entire chain.
     *
     * Checks, for every block after the genesis block:
     * 1. the stored hash equals the recomputed hash,
     * 2. the stored previous hash equals the actual hash of block i-1,
     * 3. the stored Merkle root matches the block's transactions.
     *
     * COMPLEXITY: O(n) over the number of blocks.
     *
     * @return true if the chain is tamper-free
     */
    public boolean isValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                System.out.println("✗ Block " + i + " hash mismatch!");
                return false;
            }

            if (!currentBlock.getPreviousHash().equals(previousBlock.getHash())) {
                System.out.println("✗ Block " + i + " previous hash doesn't match block " + (i - 1) + "!");
                return false;
            }

            List<String> txHashes = new ArrayList<>();
            for (Transaction tx : currentBlock.getTransactions()) {
                txHashes.add(TransactionSerializer.hashOf(tx, hasher));
            }
            MerkleTree merkleTree = new MerkleTree(txHashes, hasher);
            if (!merkleTree.getMerkleRoot().equals(currentBlock.getMerkleRoot())) {
                System.out.println("✗ Block " + i + " Merkle root invalid!");
                return false;
            }
        }

        return true;
    }

    /**
     * @param index block number to retrieve
     * @return the block, or null if the index is out of range
     */
    public Block getBlock(int index) {
        if (index >= 0 && index < chain.size()) {
            return chain.get(index);
        }
        return null;
    }

    /**
     * Trace the complete journey of a product through the supply chain.
     *
     * @param productId the product identifier to trace
     * @return chronological provenance entries involving this product
     */
    public List<ProductHistoryEntry> getProductHistory(String productId) {
        List<ProductHistoryEntry> history = new ArrayList<>();

        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (productId.equals(tx.getProductId())) {
                    history.add(new ProductHistoryEntry(
                            block.getIndex(),
                            block.getHash().substring(0, 16) + "...",
                            tx.getTimestamp(),
                            tx.getSender(),
                            tx.getReceiver(),
                            tx.getLocation()
                    ));
                }
            }
        }

        return history;
    }

    /**
     * EDUCATIONAL/TEST HOOK — simulate a malicious edit of a committed
     * transaction by rewriting its location while keeping the block's
     * stored Merkle root and hash. The next {@link #isValid()} call must
     * detect the tampering (Merkle mismatch).
     *
     * @param blockIndex       block to tamper with
     * @param transactionIndex transaction inside the block to tamper with
     * @param tamperedLocation forged location
     */
    public void simulateTampering(int blockIndex, int transactionIndex, String tamperedLocation) {
        if (blockIndex < 0 || blockIndex >= chain.size()) {
            return;
        }

        // Save backup of original chain before tampering if not already saved
        if (backupChain.isEmpty()) {
            backupChain.addAll(chain);
        }

        Block target = chain.get(blockIndex);
        List<Transaction> tamperedTxs = new ArrayList<>(target.getTransactions());
        if (transactionIndex < 0 || transactionIndex >= tamperedTxs.size()) {
            return;
        }

        Transaction original = tamperedTxs.get(transactionIndex);
        tamperedTxs.set(transactionIndex, original.withLocation(tamperedLocation));

        Block tamperedBlock = new Block(
                target.getIndex(),
                target.getTimestamp(),
                tamperedTxs,
                target.getMerkleRoot(),
                target.getPreviousHash(),
                target.getNonce(),
                hasher
        );
        chain.set(blockIndex, tamperedBlock);
    }

    /**
     * Restore the blockchain to its untampered state.
     * Useful for live demonstrations after proving tamper detection.
     *
     * @return true if restored, false if no backup existed
     */
    public boolean restoreChain() {
        if (backupChain.isEmpty()) {
            return false;
        }
        chain.clear();
        chain.addAll(backupChain);
        backupChain.clear();
        return true;
    }

    /**
     * Generate an O(log n) Merkle proof for a specific transaction hash.
     *
     * @param targetTxHash cryptographic fingerprint of the transaction
     * @return typed proof if found, or null if not found
     */
    public TransactionMerkleProof generateProofForTransaction(String targetTxHash) {
        for (Block block : chain) {
            List<String> txHashes = new ArrayList<>();
            int targetIndex = -1;
            String productId = null;

            List<Transaction> txList = block.getTransactions();
            for (int i = 0; i < txList.size(); i++) {
                Transaction tx = txList.get(i);
                String h = TransactionSerializer.hashOf(tx, hasher);
                txHashes.add(h);
                if (h.equals(targetTxHash)) {
                    targetIndex = i;
                    productId = tx.getProductId();
                }
            }

            if (targetIndex != -1) {
                MerkleTree tree = new MerkleTree(txHashes, hasher);
                List<MerkleProofElement> proof = tree.generateProof(targetTxHash);
                boolean verified = tree.verifyProof(targetTxHash, proof, block.getMerkleRoot());
                return new TransactionMerkleProof(
                        productId,
                        targetTxHash,
                        block.getIndex(),
                        block.getHash(),
                        block.getMerkleRoot(),
                        proof,
                        verified
                );
            }
        }
        return null;
    }

    /**
     * @return total number of blocks (including the genesis block)
     */
    public int getChainLength() {
        return chain.size();
    }

    /**
     * @return unmodifiable view of all blocks in the chain
     */
    public List<Block> getChain() {
        return Collections.unmodifiableList(chain);
    }

    /**
     * @return number of transactions waiting to be mined
     */
    public int getPendingCount() {
        return pendingTransactions.size();
    }
}
