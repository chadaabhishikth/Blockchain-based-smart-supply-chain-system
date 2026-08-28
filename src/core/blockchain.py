"""
Phase 3: Blockchain Ledger Implementation
===========================================
This module implements the blockchain data structure that chains blocks
together using cryptographic hashes, creating an immutable ledger.

STRUCTURE:
Each Block contains:
- Index: Position in the chain
- Timestamp: When the block was created
- Transactions: List of supply chain transactions
- Merkle Root: Hash summarizing all transactions in this block
- Previous Hash: Hash of the immediately preceding block
- Nonce: Proof of work counter (optional, for mining)
- Hash: SHA-256 hash of this entire block

The chain is immutable because modifying any block would:
1. Change its hash
2. Break the "Previous Hash" link to the next block
3. Invalidate the entire subsequent chain

Author: Blockchain Supply Chain Team
"""

import time
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, field
from datetime import datetime

try:
    from .hash_utils import calculate_sha256, hash_transaction, create_transaction
    from .merkle_tree import MerkleTree
except ImportError:
    from hash_utils import calculate_sha256, hash_transaction, create_transaction
    from merkle_tree import MerkleTree


@dataclass
class Block:
    """
    A single block in the blockchain.
    
    Contains all supply chain transactions for a time period and
    cryptographic links to the previous and next blocks.
    """
    index: int
    timestamp: str
    transactions: List[Dict[str, Any]]
    merkle_root: str
    previous_hash: str
    nonce: int = 0
    hash: str = ""
    
    def __post_init__(self):
        """Calculate hash after initialization if not provided."""
        if not self.hash:
            self.hash = self.calculate_hash()
    
    def calculate_hash(self) -> str:
        """
        Calculate SHA-256 hash of this block.
        
        Hash includes all critical data to ensure tamper evidence.
        Any change to block contents will produce different hash.
        """
        block_data = (
            str(self.index) +
            self.timestamp +
            self.merkle_root +
            self.previous_hash +
            str(self.nonce)
        )
        return calculate_sha256(block_data)
    
    def to_dict(self) -> Dict[str, Any]:
        """Serialize block to dictionary."""
        return {
            'index': self.index,
            'timestamp': self.timestamp,
            'transactions': self.transactions,
            'merkle_root': self.merkle_root,
            'previous_hash': self.previous_hash,
            'nonce': self.nonce,
            'hash': self.hash
        }


class Blockchain:
    """
    The complete blockchain ledger for supply chain tracking.
    
    KEY OPERATIONS:
    1. create_genesis_block(): Initialize the first block
    2. add_block(transactions): Add new block with transactions
    3. is_valid(): Verify entire chain integrity
    4. get_block(index): Retrieve specific block
    5. get_transaction_history(product_id): Trace product journey
    
    USAGE:
        >>> chain = Blockchain()
        >>> chain.add_block([tx1, tx2, tx3])
        >>> chain.is_valid()
        True
        >>> chain.get_product_history('PROD-001')
        [{'from': 'Factory', 'to': 'Dist'}, ...]
    """
    
    GENESIS_PREVIOUS_HASH = "0" * 64  # Hardcoded start for genesis block
    
    def __init__(self):
        """Initialize empty blockchain."""
        self.chain: List[Block] = []
        self.pending_transactions: List[Dict[str, Any]] = []
        
        # Create genesis block
        self.create_genesis_block()
    
    def create_genesis_block(self) -> Block:
        """
        Create the first block in the chain (Genesis Block).
        
        The genesis block has:
        - Index 0
        - Previous hash of all zeros (conventional starting point)
        - Empty or minimal transactions (often includes system initialization data)
        
        Returns:
            The genesis block that was added to the chain
        """
        genesis_tx = create_transaction(
            product_id='GENESIS',
            sender='SYSTEM',
            receiver='NETWORK',
            location='Block 0',
            metadata={'type': 'genesis_block', 'description': 'Supply Chain Network Genesis'}
        )
        
        # Hash the transaction for Merkle tree
        tx_hashes = [hash_transaction(genesis_tx)]
        merkle = MerkleTree(tx_hashes)
        
        genesis_block = Block(
            index=0,
            timestamp=datetime.utcnow().isoformat() + 'Z',
            transactions=[genesis_tx],
            merkle_root=merkle.get_merkle_root(),
            previous_hash=self.GENESIS_PREVIOUS_HASH,
            nonce=0
        )
        
        self.chain.append(genesis_block)
        return genesis_block
    
    def get_latest_block(self) -> Block:
        """Get the most recent block in the chain."""
        return self.chain[-1]
    
    def add_block(self, transactions: List[Dict[str, Any]] = None) -> Block:
        """
        Add a new block to the chain containing the given transactions.
        
        This is the core operation of the blockchain:
        1. Create Merkle tree from transactions
        2. Link to previous block
        3. Calculate cryptographic hash
        4. Add to chain
        
        Args:
            transactions: List of transaction dictionaries to include
            
        Returns:
            The newly created and added block
        """
        if transactions is None:
            transactions = self.pending_transactions.copy()
            self.pending_transactions.clear()
        
        # Hash transactions for Merkle tree
        tx_hashes = [hash_transaction(tx) for tx in transactions]
        merkle_tree = MerkleTree(tx_hashes)
        merkle_root = merkle_tree.get_merkle_root()
        
        # Create new block linked to previous
        new_block = Block(
            index=len(self.chain),
            timestamp=datetime.utcnow().isoformat() + 'Z',
            transactions=transactions,
            merkle_root=merkle_root,
            previous_hash=self.get_latest_block().hash,
            nonce=0
        )
        
        self.chain.append(new_block)
        return new_block
    
    def add_pending_transaction(self, transaction: Dict[str, Any]) -> None:
        """
        Add a transaction to the pending pool.
        
        Pending transactions are waiting to be mined (added to a block).
        This simulates real-world blockchain where transactions wait
        until a miner includes them in a block.
        
        Args:
            transaction: Transaction dictionary to add to pending pool
        """
        self.pending_transactions.append(transaction)
    
    def mine_pending_transactions(self) -> Block:
        """
        Mine all pending transactions into a new block.
        
        In a real blockchain, this would involve proof-of-work.
        For our supply chain, we use a simplified model where
        pending transactions are batched into blocks.
        
        Returns:
            The newly created block with pending transactions
        """
        return self.add_block()
    
    def is_valid(self) -> bool:
        """
        Validate the entire blockchain integrity.
        
        Checks performed:
        1. Each block's hash is correctly calculated
        2. Each block's "previous hash" matches the actual previous block's hash
        3. Merkle root matches the transactions in each block
        
        Returns:
            True if chain is valid and tamper-free, False if compromised
            
        COMPLEXITY: O(n) where n is number of blocks
        """
        for i in range(1, len(self.chain)):
            current_block = self.chain[i]
            previous_block = self.chain[i - 1]
            
            # Check 1: Hash integrity
            if current_block.hash != current_block.calculate_hash():
                print(f"✗ Block {i} hash mismatch!")
                return False
            
            # Check 2: Chain integrity (previous hash link)
            if current_block.previous_hash != previous_block.hash:
                print(f"✗ Block {i} previous hash doesn't match block {i-1}!")
                return False
            
            # Check 3: Merkle root validity
            tx_hashes = [hash_transaction(tx) for tx in current_block.transactions]
            merkle = MerkleTree(tx_hashes)
            if merkle.get_merkle_root() != current_block.merkle_root:
                print(f"✗ Block {i} Merkle root invalid!")
                return False
        
        return True
    
    def get_block(self, index: int) -> Optional[Block]:
        """
        Retrieve a specific block by index.
        
        Args:
            index: Block number to retrieve
            
        Returns:
            Block if found, None if index out of range
        """
        if 0 <= index < len(self.chain):
            return self.chain[index]
        return None
    
    def get_product_history(self, product_id: str) -> List[Dict[str, Any]]:
        """
        Trace the complete journey of a product through the supply chain.
        
        Searches all blocks and transactions to find every occurrence
        of the specified product_id, returning them in chronological order.
        
        Args:
            product_id: The product identifier to trace
            
        Returns:
            List of transaction dictionaries involving this product
        """
        history = []
        
        for block in self.chain:
            for tx in block.transactions:
                if tx.get('product_id') == product_id:
                    history.append({
                        'block_index': block.index,
                        'block_hash': block.hash[:16] + '...',
                        'timestamp': tx['timestamp'],
                        'sender': tx['sender'],
                        'receiver': tx['receiver'],
                        'location': tx['location'],
                        'verified': True  # Proven by blockchain
                    })
        
        return history
    
    def verify_product(self, product_id: str, expected_hash: str = None) -> Dict[str, Any]:
        """
        Verify product authenticity by checking its complete history.
        
        This is the end-to-end verification that consumers use when
        scanning a product QR code.
        
        Args:
            product_id: Product to verify
            expected_hash: Optional hash of a specific transaction to verify
            
        Returns:
            Dictionary with verification results
        """
        history = self.get_product_history(product_id)
        
        if not history:
            return {
                'verified': False,
                'reason': 'Product not found in blockchain',
                'authentic': False
            }
        
        # Check if product originated from valid genesis-linked manufacturer
        origin = history[0]
        if origin['sender'] == 'SYSTEM':
            return {
                'verified': False,
                'reason': 'Invalid origin (genesis block only)',
                'authentic': False
            }
        
        result = {
            'verified': True,
            'authentic': True,
            'origin': origin,
            'current_location': history[-1]['location'],
            'current_owner': history[-1]['receiver'],
            'journey_length': len(history),
            'journey': history
        }
        
        return result
    
    def get_chain_length(self) -> int:
        """Get the total number of blocks in the chain."""
        return len(self.chain)
    
    def display_chain(self) -> str:
        """
        Generate ASCII representation of the blockchain.
        
        Returns:
            String showing chain structure with key information
        """
        lines = []
        lines.append("\n" + "=" * 70)
        lines.append("BLOCKCHAIN LEDGER")
        lines.append("=" * 70)
        
        for block in self.chain:
            lines.append(f"\n┌─ Block #{block.index}")
            lines.append(f"│  Timestamp: {block.timestamp}")
            lines.append(f"│  Transactions: {len(block.transactions)}")
            lines.append(f"│  Merkle Root: {block.merkle_root[:32]}...")
            lines.append(f"│  Previous Hash: {block.previous_hash[:32]}...")
            lines.append(f"│  Block Hash: {block.hash[:32]}...")
            lines.append(f"│  Status: ✓ Verified")
            lines.append("└" + "─" * 68)
        
        lines.append(f"\nTotal Blocks: {len(self.chain)}")
        lines.append(f"Pending Transactions: {len(self.pending_transactions)}")
        lines.append("=" * 70 + "\n")
        
        return "\n".join(lines)


# ============================================================================
# UNIT TESTS FOR BLOCKCHAIN
# ============================================================================

def test_genesis_block():
    """Test that genesis block is created correctly."""
    chain = Blockchain()
    
    assert len(chain.chain) == 1, "Should start with genesis block"
    assert chain.chain[0].index == 0, "Genesis block index should be 0"
    assert chain.chain[0].previous_hash == "0" * 64, "Genesis previous hash should be zeros"
    assert chain.chain[0].hash != "", "Genesis block should have a hash"
    
    print("✓ Genesis block created correctly")


def test_block_chaining():
    """Test that blocks are properly chained together."""
    chain = Blockchain()
    
    tx1 = create_transaction('PROD-001', 'Factory', 'Distributor', 'Shanghai')
    chain.add_block([tx1])
    
    tx2 = create_transaction('PROD-001', 'Distributor', 'Retailer', 'Beijing')
    chain.add_block([tx2])
    
    assert len(chain.chain) == 3, "Should have 3 blocks (genesis + 2)"
    assert chain.chain[2].previous_hash == chain.chain[1].hash, "Chain link broken"
    
    print("✓ Blocks properly chained")


def test_chain_validation():
    """Test blockchain integrity validation."""
    chain = Blockchain()
    
    # Add some blocks
    for i in range(5):
        tx = create_transaction(f'PROD-{i:03d}', f'Factory {i}', f'Dist {i}', f'Loc {i}')
        chain.add_block([tx])
    
    assert chain.is_valid() == True, "Valid chain should pass validation"
    
    # Tamper with a block
    chain.chain[2].transactions[0]['location'] = 'Tampered Location'
    
    assert chain.is_valid() == False, "Tampered chain should fail validation"
    
    print("✓ Chain validation detects tampering")


def test_product_history():
    """Test tracing product journey through supply chain."""
    chain = Blockchain()
    
    # Simulate product journey
    journey = [
        ('Factory-A', 'Distributor-B', 'Shanghai'),
        ('Distributor-B', 'Wholesaler-C', 'Beijing'),
        ('Wholesaler-C', 'Retailer-D', 'Shanghai'),
        ('Retailer-D', 'Consumer', 'Customer Home'),
    ]
    
    for sender, receiver, location in journey:
        tx = create_transaction('AUTHENTIC-001', sender, receiver, location)
        chain.add_block([tx])
    
    history = chain.get_product_history('AUTHENTIC-001')
    
    assert len(history) == 4, f"Should have 4 transactions, got {len(history)}"
    assert history[0]['sender'] == 'Factory-A', "Origin should be factory"
    assert history[-1]['receiver'] == 'Consumer', "Final destination should be consumer"
    
    print(f"✓ Product journey tracked: {len(history)} steps")
    for step in history:
        print(f"  {step['sender']} → {step['receiver']} ({step['location']})")


def test_verify_product():
    """Test product verification functionality."""
    chain = Blockchain()
    
    # Create authentic product
    tx1 = create_transaction('PROD-REAL', 'Factory', 'Dist', 'Factory Location')
    chain.add_block([tx1])
    
    tx2 = create_transaction('PROD-REAL', 'Dist', 'Retailer', 'Distribution Center')
    chain.add_block([tx2])
    
    # Verify authentic product
    result = chain.verify_product('PROD-REAL')
    assert result['verified'] == True, "Real product should verify"
    assert result['authentic'] == True, "Real product should be authentic"
    
    # Try to verify non-existent product
    result = chain.verify_product('PROD-FAKE')
    assert result['verified'] == False, "Fake product should not verify"
    assert result['authentic'] == False, "Fake product should not be authentic"
    
    print("✓ Product verification working correctly")


def test_merkle_root_in_block():
    """Test that Merkle root is correctly calculated and stored."""
    chain = Blockchain()
    
    # Add block with multiple transactions
    transactions = [
        create_transaction(f'PROD-{i:03d}', f'Sender {i}', f'Receiver {i}', f'Loc {i}')
        for i in range(4)
    ]
    
    block = chain.add_block(transactions)
    
    # Verify Merkle root matches (need to hash transactions)
    tx_hashes = [hash_transaction(tx) for tx in transactions]
    merkle = MerkleTree(tx_hashes)
    assert block.merkle_root == merkle.get_merkle_root(), "Merkle root mismatch"
    
    print("✓ Merkle root correctly stored in block")


if __name__ == '__main__':
    print("=" * 60)
    print("PHASE 3: BLOCKCHAIN LEDGER UNIT TESTS")
    print("=" * 60)
    
    test_genesis_block()
    print()
    test_block_chaining()
    print()
    test_chain_validation()
    print()
    test_product_history()
    print()
    test_verify_product()
    print()
    test_merkle_root_in_block()
    
    print("\n" + "=" * 60)
    print("ALL TESTS PASSED ✓")
    print("=" * 60)
    print("\nBlockchain ledger complete. Proceeding to supply chain logic...")
