"""
Phase 2: Merkle Tree Implementation
====================================
This module implements the Merkle Tree data structure for efficient
transaction verification in our supply chain blockchain.

A Merkle Tree is a binary hash tree where:
- LEAVES: Individual transaction hashes
- INTERNAL NODES: Hash of concatenated child nodes
- ROOT: Single hash representing all transactions (Merkle Root)

COMPLEXITY ANALYSIS:
- Building the tree: O(n) time
- Verification of single transaction: O(log n) time
- Storage: O(n) where n is number of transactions

Author: Blockchain Supply Chain Team
"""

from typing import List, Optional, Tuple, Dict
from dataclasses import dataclass
import hashlib


def calculate_sha256(data: str) -> str:
    """Calculate SHA-256 hash of a string."""
    return hashlib.sha256(data.encode('utf-8')).hexdigest()


@dataclass
class MerkleNode:
    """
    Node in the Merkle Tree.
    
    Each node holds:
    - hash: The cryptographic hash of this node's data
    - left: Reference to left child
    - right: Reference to right child
    """
    hash: str
    left: Optional['MerkleNode'] = None
    right: Optional['MerkleNode'] = None
    
    def __repr__(self):
        return f"MerkleNode({self.hash[:16]}...)"


class MerkleTree:
    """
    A complete Merkle Tree implementation for supply chain verification.
    
    KEY OPERATIONS:
    1. build_tree(transactions): Construct tree from transaction list
    2. get_merkle_root(): Get the single root hash
    3. generate_proof(target_hash): Create verification path
    4. verify_proof(target_hash, proof, root): Verify transaction exists
    
    USAGE EXAMPLE:
        >>> tree = MerkleTree(['hash1', 'hash2', 'hash3', 'hash4'])
        >>> root = tree.get_merkle_root()
        >>> proof = tree.generate_proof('hash1')
        >>> MerkleTree.verify_proof('hash1', proof, root)
        True
    """
    
    def __init__(self, data: List[str] = None):
        """
        Initialize Merkle Tree with leaf data.
        
        Args:
            data: List of strings (transaction hashes) to build tree from
        """
        self.data = data or []
        self.root: Optional[MerkleNode] = None
        
        if self.data:
            self.build_tree()
    
    def build_tree(self) -> None:
        """
        Build the complete Merkle Tree from the transaction list.
        
        ALGORITHM:
        1. Start with leaf hashes
        2. Pair adjacent leaves and hash them to create parent nodes
        3. Repeat recursively until single root remains
        
        TIME COMPLEXITY: O(n) where n is number of leaves
        """
        if not self.data:
            self.root = None
            return
        
        # Convert data to list of nodes
        current_level = [MerkleNode(hash=d) for d in self.data]
        
        # Build tree bottom-up
        while len(current_level) > 1:
            next_level = []
            
            for i in range(0, len(current_level), 2):
                left = current_level[i]
                # If odd number, duplicate the last node
                right = current_level[i + 1] if i + 1 < len(current_level) else left
                
                # Hash the combination of children
                combined = left.hash + right.hash
                parent_hash = calculate_sha256(combined)
                
                parent = MerkleNode(hash=parent_hash, left=left, right=right)
                next_level.append(parent)
            
            current_level = next_level
        
        self.root = current_level[0]
    
    def get_merkle_root(self) -> Optional[str]:
        """
        Get the Merkle Root - the single hash representing all data.
        
        Returns:
            64-character hex string (SHA-256 hash) or None if tree is empty
        """
        return self.root.hash if self.root else None
    
    def generate_proof(self, target_hash: str) -> List[Tuple[str, str]]:
        """
        Generate a Merkle Proof for verifying a specific hash.
        
        A proof consists of sibling hashes that, when combined with the
        target hash up to the root, should recreate the Merkle root.
        
        Args:
            target_hash: The hash to verify
            
        Returns:
            List of (sibling_hash, position) tuples where position indicates
            whether the sibling is to the 'left' or 'right' of the current hash
        """
        proof = []
        
        if not self.root:
            return proof
        
        # Build tree structure for traversal
        # Each node stores: (node, level, position_in_level)
        
        def find_and_build_proof(node: MerkleNode, target: str) -> bool:
            """
            Find the target node and build proof by tracking the path.
            Returns True if found.
            """
            if node is None:
                return False
            
            # Leaf node
            if node.left is None and node.right is None:
                return node.hash == target
            
            # Internal node - search children
            if node.left:
                if find_and_build_proof(node.left, target):
                    # Found in left subtree
                    # Add right sibling (or left if no right exists - duplicate case)
                    sibling = node.right if node.right else node.left
                    proof.append((sibling.hash, 'right'))
                    return True
            
            if node.right:
                if find_and_build_proof(node.right, target):
                    # Found in right subtree
                    # Add left sibling
                    proof.append((node.left.hash, 'left'))
                    return True
            
            return False
        
        find_and_build_proof(self.root, target_hash)
        return proof
    
    @staticmethod
    def verify_proof(target_hash: str, proof: List[Tuple[str, str]], root_hash: str) -> bool:
        """
        Verify that a target hash is part of the tree with given root.
        
        Takes the target hash and sequentially combines it with proof
        hashes (according to their positions) until reaching the root.
        If we arrive at the same root_hash, the proof is valid.
        
        COMPLEXITY: O(log n) - only processes proof path
        
        Args:
            target_hash: The hash to verify
            proof: List of (sibling_hash, position) from generate_proof()
            root_hash: The expected Merkle root
            
        Returns:
            True if the proof is valid and target is in the tree
        """
        current_hash = target_hash
        
        for sibling_hash, position in proof:
            if position == 'left':
                # Target is on right, sibling is on left
                combined = sibling_hash + current_hash
            else:
                # Target is on left, sibling is on right
                combined = current_hash + sibling_hash
            
            current_hash = calculate_sha256(combined)
        
        return current_hash == root_hash
    
    def get_tree_depth(self) -> int:
        """Calculate the depth of the tree."""
        def depth(node: MerkleNode) -> int:
            if node is None:
                return 0
            if node.left is None and node.right is None:
                return 1
            return 1 + max(depth(node.left), depth(node.right))
        
        return depth(self.root)


# ============================================================================
# UNIT TESTS FOR MERKLE TREE
# ============================================================================

def test_merkle_tree_construction():
    """Test that Merkle Tree correctly builds."""
    # Test with 4 leaves
    data = ['hash1', 'hash2', 'hash3', 'hash4']
    tree = MerkleTree(data)
    
    assert tree.get_merkle_root() is not None, "Tree must have a root"
    assert len(tree.get_merkle_root()) == 64, "Root must be SHA-256 hash"
    
    # Verify tree depth is log2(n) + 1
    depth = tree.get_tree_depth()
    assert depth == 3, f"Expected depth 3 for 4 leaves, got {depth}"
    
    print(f"✓ Merkle root: {tree.get_merkle_root()[:32]}...")
    print(f"✓ Tree depth: {depth}")


def test_proof_generation_and_verification():
    """
    Test O(log n) proof generation and verification.
    This is the CORE verification mechanism for our supply chain.
    """
    # Test with 8 items (power of 2)
    data = [f'hash{i}' for i in range(8)]
    tree = MerkleTree(data)
    root = tree.get_merkle_root()
    
    # Verify each item
    for i, hash_val in enumerate(data):
        proof = tree.generate_proof(hash_val)
        
        # Verify proof
        is_valid = MerkleTree.verify_proof(hash_val, proof, root)
        assert is_valid, f"Proof verification failed for hash{i}"
        
        # Verify proof length is O(log n)
        assert len(proof) <= 4, f"Proof too long: {len(proof)} (expected ~3 for 8 items)"
    
    print(f"✓ All 8 transactions verified with proof length O(log n)")
    print(f"  Proof length for first item: {len(tree.generate_proof(data[0]))}")


def test_invalid_proof_rejection():
    """Test that tampered hashes are correctly rejected."""
    data = ['hash1', 'hash2']
    tree = MerkleTree(data)
    root = tree.get_merkle_root()
    
    # Get proof for first hash
    proof = tree.generate_proof('hash1')
    
    # Try to verify a different hash against the proof
    is_valid = MerkleTree.verify_proof('hash2', proof, root)
    assert not is_valid, "Different hash must be rejected"
    
    print("✓ Tampered hash correctly rejected")


def test_single_item():
    """Test edge case: Merkle tree with single item."""
    tree = MerkleTree(['single_hash'])
    root = tree.get_merkle_root()
    
    proof = tree.generate_proof('single_hash')
    assert MerkleTree.verify_proof('single_hash', proof, root), "Single item must work"
    assert len(proof) == 0, "Single node needs no proof path"
    
    print("✓ Single item tree handled correctly")


def test_odd_number_of_items():
    """Test handling of odd number of items (duplication strategy)."""
    data = ['hash1', 'hash2', 'hash3']
    tree = MerkleTree(data)
    root = tree.get_merkle_root()
    
    # All items should verify
    for d in data:
        proof = tree.generate_proof(d)
        assert MerkleTree.verify_proof(d, proof, root), f"{d} should verify"
    
    print("✓ Odd number of items handled (duplicate last node)")


def test_batch_verification():
    """Test batch verification - verifying multiple items at once."""
    # Large batch: 1000 items
    batch_size = 1000
    data = [f'hash{i:04d}' for i in range(batch_size)]
    
    tree = MerkleTree(data)
    root = tree.get_merkle_root()
    
    # Verify entire batch
    successful_verifications = 0
    for d in data:
        proof = tree.generate_proof(d)
        if MerkleTree.verify_proof(d, proof, root):
            successful_verifications += 1
    
    assert successful_verifications == batch_size, "All items must verify"
    
    proof_len = len(tree.generate_proof(data[500]))
    print(f"✓ Batch verification: {batch_size} items verified at O(log n) each")
    print(f"  Proof length for {batch_size} items: {proof_len} (log2(1000) ≈ 10)")


def test_merkle_root_consistency():
    """Test that same data always produces same root."""
    data1 = ['hash1', 'hash2', 'hash3', 'hash4']
    
    tree1 = MerkleTree(data1)
    tree2 = MerkleTree(data1)
    
    assert tree1.get_merkle_root() == tree2.get_merkle_root(), "Same data must produce same root"
    
    print("✓ Merkle root is deterministic")


if __name__ == '__main__':
    print("=" * 60)
    print("PHASE 2: MERKLE TREE UNIT TESTS")
    print("=" * 60)
    
    test_merkle_tree_construction()
    print()
    test_proof_generation_and_verification()
    print()
    test_invalid_proof_rejection()
    print()
    test_single_item()
    print()
    test_odd_number_of_items()
    print()
    test_batch_verification()
    print()
    test_merkle_root_consistency()
    
    print("\n" + "=" * 60)
    print("ALL TESTS PASSED ✓")
    print("=" * 60)
    print("\nMerkle Tree implementation complete. Proceeding to blockchain...")
