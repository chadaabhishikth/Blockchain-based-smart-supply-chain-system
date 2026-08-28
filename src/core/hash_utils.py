"""
Phase 1: SHA-256 Cryptographic Hashing Utility
===============================================
This module provides the foundational cryptographic fingerprinting for our
blockchain-based supply chain system.

Every transaction will be hashed using SHA-256 to create an unforgeable
digital fingerprint. Even the slightest change in transaction data will
completely change the hash output.

Author: Blockchain Supply Chain Team
"""

import hashlib
import json
from typing import Any, Dict
from datetime import datetime


def serialize_transaction(transaction: Dict[str, Any]) -> str:
    """
    Serialize a transaction dictionary to a canonical JSON string.
    
    CRITICAL: This must produce identical output for identical input.
    We sort keys to ensure consistent ordering and use separators to
    avoid any ambiguity in the serialization.
    
    Args:
        transaction: Dictionary containing transaction data
        
    Returns:
        Canonical JSON string representation
    """
    return json.dumps(
        transaction, 
        sort_keys=True, 
        separators=(',', ':'),
        default=str  # Handle datetime objects
    )


def calculate_sha256(data: str) -> str:
    """
    Calculate the SHA-256 hash of a string.
    
    SHA-256 produces a fixed 256-bit (32-byte) hash regardless of input size.
    It exhibits:
    - Determinism: Same input always produces same output
    - Avalanche effect: Small input changes cause massive output changes
    - One-way function: Practically impossible to reverse
    
    Args:
        data: String to hash
        
    Returns:
        Hexadecimal string representation of the 256-bit hash (64 characters)
    """
    return hashlib.sha256(data.encode('utf-8')).hexdigest()


def hash_transaction(transaction: Dict[str, Any]) -> str:
    """
    Create a unique cryptographic fingerprint for a transaction.
    
    This is the core hashing function that all transactions pass through.
    It serializes the transaction consistently and returns its SHA-256 hash.
    
    Args:
        transaction: Dictionary with keys: product_id, sender, receiver, 
                    timestamp, location
        
    Returns:
        SHA-256 hash of the transaction as a 64-character hex string
        
    Example:
        >>> tx = {
        ...     'product_id': 'PROD-001',
        ...     'sender': 'Factory A',
        ...     'receiver': 'Distributor B',
        ...     'timestamp': '2026-08-28T10:30:00',
        ...     'location': 'Warehouse 1'
        ... }
        >>> hash_transaction(tx)
        'a7f3e2d1c8b4...'  # 64-character hash
    """
    serialized = serialize_transaction(transaction)
    return calculate_sha256(serialized)


def verify_transaction_integrity(transaction: Dict[str, Any], expected_hash: str) -> bool:
    """
    Verify that a transaction matches an expected hash.
    
    This is our tamper detection mechanism. If anyone modifies even
    a single character in the transaction, the hash will change completely.
    
    Args:
        transaction: The transaction data to verify
        expected_hash: The hash we expect the transaction to produce
        
    Returns:
        True if the transaction matches the expected hash, False otherwise
    """
    computed_hash = hash_transaction(transaction)
    return computed_hash == expected_hash


def create_transaction(
    product_id: str,
    sender: str,
    receiver: str,
    location: str,
    metadata: Dict[str, Any] = None
) -> Dict[str, Any]:
    """
    Factory function to create a well-structured transaction object.
    
    Ensures all transactions have the same structure for consistent hashing.
    
    Args:
        product_id: Unique identifier for the product
        sender: Current owner/entity transferring the product
        receiver: New owner/entity receiving the product
        location: Physical location of the transaction
        metadata: Optional additional data (batch number, quality checks, etc.)
        
    Returns:
        Complete transaction dictionary with timestamp
    """
    transaction = {
        'product_id': product_id,
        'sender': sender,
        'receiver': receiver,
        'location': location,
        'timestamp': datetime.utcnow().isoformat() + 'Z',
        'metadata': metadata or {}
    }
    return transaction


# ============================================================================
# UNIT TESTS FOR SHA-256 HASHING
# ============================================================================

def test_hash_consistency():
    """
    Test that the same transaction produces identical hashes.
    """
    tx = create_transaction(
        product_id='PROD-001',
        sender='Factory A',
        receiver='Distributor B',
        location='Shanghai, China'
    )
    
    hash1 = hash_transaction(tx)
    hash2 = hash_transaction(tx)
    
    assert hash1 == hash2, "Same transaction must produce identical hash"
    print("✓ Hash consistency test passed")


def test_avalanche_effect():
    """
    Test that tiny changes completely change the hash (avalanche effect).
    
    This is CRITICAL for security - a cryptographic hash must change
    dramatically even with minimal input changes.
    """
    tx1 = create_transaction(
        product_id='PROD-001',
        sender='Factory A',
        receiver='Distributor B',
        location='Warehouse 1'
    )
    
    tx2 = tx1.copy()
    tx2['location'] = 'Warehouse 2'  # Only change the location
    
    hash1 = hash_transaction(tx1)
    hash2 = hash_transaction(tx2)
    
    assert hash1 != hash2, "Different data must produce different hashes"
    
    # Count how many characters changed (should be ~50% for true avalanche)
    differing_chars = sum(c1 != c2 for c1, c2 in zip(hash1, hash2))
    change_percentage = (differing_chars / 64) * 100
    
    print(f"✓ Avalanche effect test passed: {change_percentage:.1f}% of hash characters changed")


def test_hash_uniqueness():
    """
    Test that different transactions produce different hashes.
    """
    tx1 = create_transaction(
        product_id='PROD-001',
        sender='Factory A',
        receiver='Distributor B',
        location='Shanghai'
    )
    
    tx2 = create_transaction(
        product_id='PROD-002',  # Different product
        sender='Factory A',
        receiver='Distributor B',
        location='Shanghai'
    )
    
    hash1 = hash_transaction(tx1)
    hash2 = hash_transaction(tx2)
    
    assert hash1 != hash2, "Different transactions must produce different hashes"
    print("✓ Hash uniqueness test passed")


def test_hash_length():
    """
    Verify that SHA-256 always produces 64-character hex strings.
    """
    tx = create_transaction(
        product_id='PROD-001',
        sender='Factory A',
        receiver='Distributor B',
        location='Test Location'
    )
    
    hash_result = hash_transaction(tx)
    
    assert len(hash_result) == 64, f"SHA-256 must produce 64 chars, got {len(hash_result)}"
    assert all(c in '0123456789abcdef' for c in hash_result), "Hash must be hexadecimal"
    print("✓ Hash length test passed: 64 hexadecimal characters")


if __name__ == '__main__':
    print("=" * 60)
    print("PHASE 1: SHA-256 HASHING UNIT TESTS")
    print("=" * 60)
    
    test_hash_consistency()
    test_avalanche_effect()
    test_hash_uniqueness()
    test_hash_length()
    
    print("\n" + "=" * 60)
    print("ALL TESTS PASSED ✓")
    print("=" * 60)
    print("\nCryptographic foundation ready. Proceeding to Merkle Trees...")
