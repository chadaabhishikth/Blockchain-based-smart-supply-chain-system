"""
Blockchain-based Smart Supply Chain System
===========================================

A comprehensive implementation of blockchain technology for supply chain
provenance tracking and counterfeit detection.

MODULES:
- core.hash_utils: SHA-256 cryptographic hashing
- core.merkle_tree: Merkle Tree for efficient verification
- core.blockchain: Immutable blockchain ledger
- supply_chain.business_logic: Supply chain operations
- benchmarking.comparison: SQL vs blockchain analysis

USAGE:
    from src.supply_chain.business_logic import SupplyChainBlockchain
    
    sc = SupplyChainBlockchain()
    sc.register_manufacturer('Factory-A')
    sc.manufacture_product('Factory-A', ['PROD-001'], 'Shanghai')
    sc.verify_product('PROD-001')
"""

__version__ = '1.0.0'
__author__ = 'Blockchain Supply Chain Team'

from .hash_utils import (
    calculate_sha256,
    hash_transaction,
    create_transaction,
    verify_transaction_integrity
)

from .merkle_tree import MerkleTree, MerkleNode

from .blockchain import Blockchain, Block

from ..supply_chain.business_logic import (
    SupplyChainBlockchain,
    SupplyChainStage,
    ProductStatus
)

__all__ = [
    # Hash utilities
    'calculate_sha256',
    'hash_transaction',
    'create_transaction',
    'verify_transaction_integrity',
    
    # Merkle Tree
    'MerkleTree',
    'MerkleNode',
    
    # Blockchain
    'Blockchain',
    'Block',
    
    # Supply Chain
    'SupplyChainBlockchain',
    'SupplyChainStage',
    'ProductStatus',
]
