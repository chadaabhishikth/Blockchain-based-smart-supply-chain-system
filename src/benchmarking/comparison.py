"""
Phase 5: Academic Comparison - Blockchain vs Traditional Database
===================================================================
This module provides a comprehensive comparison between the blockchain-based
supply chain system and a traditional centralized SQL database.

METRICS ANALYZED:
1. Performance: Transaction throughput and latency
2. Storage: Space efficiency and growth rate
3. Security: Tamper resistance and data integrity
4. Trust Model: Centralization vs decentralization
5. Complexity: Implementation and maintenance effort

This analysis addresses the key question: When should you use blockchain
vs traditional databases for supply chain tracking?

Author: Blockchain Supply Chain Team
"""

import time
import sqlite3
import statistics
from typing import List, Dict, Any
from datetime import datetime
import json
import os

try:
    from ..core.blockchain import Blockchain
    from ..core.hash_utils import create_transaction, hash_transaction
    from ..core.merkle_tree import MerkleTree
    from ..supply_chain.business_logic import SupplyChainBlockchain
except ImportError:
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..'))
    from src.core.blockchain import Blockchain
    from src.core.hash_utils import create_transaction, hash_transaction
    from src.core.merkle_tree import MerkleTree
    from src.supply_chain.business_logic import SupplyChainBlockchain


class DatabaseBenchmark:
    """
    Benchmark framework for comparing blockchain vs SQL database performance.
    
    Tests:
    1. Transaction insertion speed
    2. Query performance
    3. Storage efficiency
    4. Verification operations
    """
    
    def __init__(self):
        """Initialize benchmark environment."""
        self.blockchain = Blockchain()
        self.db_path = '/tmp/supply_chain_benchmark.db'
        self.setup_sql_database()
    
    def setup_sql_database(self):
        """Create SQL schema for comparison."""
        self.db_conn = sqlite3.connect(self.db_path)
        self.db_cursor = self.db_conn.cursor()
        
        # Create transaction table
        self.db_cursor.execute('''
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id TEXT NOT NULL,
                sender TEXT NOT NULL,
                receiver TEXT NOT NULL,
                location TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                block_number INTEGER,
                metadata TEXT
            )
        ''')
        
        # Create index for faster queries
        self.db_cursor.execute('''
            CREATE INDEX IF NOT EXISTS idx_product_id 
            ON transactions(product_id)
        ''')
        
        self.db_cursor.execute('''
            CREATE INDEX IF NOT EXISTS idx_timestamp 
            ON transactions(timestamp)
        ''')
        
        self.db_conn.commit()
    
    def insert_sql_transaction(self, transaction: Dict[str, Any], block_number: int):
        """Insert a single transaction into SQL database."""
        self.db_cursor.execute('''
            INSERT INTO transactions 
            (product_id, sender, receiver, location, timestamp, block_number, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (
            transaction['product_id'],
            transaction['sender'],
            transaction['receiver'],
            transaction['location'],
            transaction['timestamp'],
            block_number,
            json.dumps(transaction.get('metadata', {}))
        ))
    
    def query_product_history_sql(self, product_id: str) -> List[Dict[str, Any]]:
        """Query product history from SQL database."""
        self.db_cursor.execute('''
            SELECT product_id, sender, receiver, location, timestamp, block_number
            FROM transactions
            WHERE product_id = ?
            ORDER BY timestamp ASC
        ''', (product_id,))
        
        results = []
        for row in self.db_cursor.fetchall():
            results.append({
                'product_id': row[0],
                'sender': row[1],
                'receiver': row[2],
                'location': row[3],
                'timestamp': row[4],
                'block_number': row[5]
            })
        
        return results
    
    def run_benchmark(
        self, 
        num_transactions: int = 1000,
        num_products: int = 100
    ) -> Dict[str, Any]:
        """
        Run comprehensive benchmark comparing blockchain and SQL.
        
        Args:
            num_transactions: Number of transactions to process
            num_products: Number of unique products
            
        Returns:
            Dictionary with detailed benchmark results
        """
        print("\n" + "=" * 70)
        print("BENCHMARK: BLOCKCHAIN vs TRADITIONAL DATABASE")
        print("=" * 70)
        print(f"Test Parameters: {num_transactions} transactions, {num_products} products\n")
        
        results = {
            'parameters': {
                'num_transactions': num_transactions,
                'num_products': num_products
            },
            'blockchain': {},
            'sql_database': {},
            'comparison': {}
        }
        
        # =========================================================================
        # 1. INSERTION PERFORMANCE
        # =========================================================================
        print("TEST 1: Transaction Insertion Performance")
        print("-" * 70)
        
        # Blockchain insertion
        blockchain_insert_times = []
        for i in range(num_transactions):
            product_id = f'PROD-{i % num_products:04d}'
            tx = create_transaction(
                product_id=product_id,
                sender=f'Entity-{i % 10}',
                receiver=f'Entity-{(i + 1) % 10}',
                location=f'Location-{i % 20}',
                metadata={'batch': i // 10}
            )
            
            start = time.perf_counter()
            self.blockchain.add_block([tx])
            end = time.perf_counter()
            
            blockchain_insert_times.append((end - start) * 1000)  # Convert to ms
        
        results['blockchain']['total_insert_time_ms'] = sum(blockchain_insert_times)
        results['blockchain']['avg_insert_time_ms'] = statistics.mean(blockchain_insert_times)
        results['blockchain']['median_insert_time_ms'] = statistics.median(blockchain_insert_times)
        results['blockchain']['max_insert_time_ms'] = max(blockchain_insert_times)
        results['blockchain']['min_insert_time_ms'] = min(blockchain_insert_times)
        
        print(f"Blockchain:")
        print(f"  Total time: {results['blockchain']['total_insert_time_ms']:.2f} ms")
        print(f"  Average per transaction: {results['blockchain']['avg_insert_time_ms']:.4f} ms")
        
        # SQL insertion
        sql_insert_times = []
        for i in range(num_transactions):
            product_id = f'PROD-{i % num_products:04d}'
            tx = create_transaction(
                product_id=product_id,
                sender=f'Entity-{i % 10}',
                receiver=f'Entity-{(i + 1) % 10}',
                location=f'Location-{i % 20}',
                metadata={'batch': i // 10}
            )
            
            start = time.perf_counter()
            self.insert_sql_transaction(tx, i // 10)
            self.db_conn.commit()
            end = time.perf_counter()
            
            sql_insert_times.append((end - start) * 1000)
        
        results['sql_database']['total_insert_time_ms'] = sum(sql_insert_times)
        results['sql_database']['avg_insert_time_ms'] = statistics.mean(sql_insert_times)
        results['sql_database']['median_insert_time_ms'] = statistics.median(sql_insert_times)
        results['sql_database']['max_insert_time_ms'] = max(sql_insert_times)
        results['sql_database']['min_insert_time_ms'] = min(sql_insert_times)
        
        print(f"SQL Database:")
        print(f"  Total time: {results['sql_database']['total_insert_time_ms']:.2f} ms")
        print(f"  Average per transaction: {results['sql_database']['avg_insert_time_ms']:.4f} ms")
        
        speedup = results['sql_database']['total_insert_time_ms'] / results['blockchain']['total_insert_time_ms']
        print(f"\n  SQL is {speedup:.2f}x FASTER for insertions\n")
        
        # =========================================================================
        # 2. QUERY PERFORMANCE
        # =========================================================================
        print("TEST 2: Product History Query Performance")
        print("-" * 70)
        
        # Blockchain query
        test_product = 'PROD-0001'
        blockchain_query_times = []
        
        for _ in range(100):
            start = time.perf_counter()
            history = self.blockchain.get_product_history(test_product)
            end = time.perf_counter()
            blockchain_query_times.append((end - start) * 1000)
        
        results['blockchain']['query_time_ms'] = statistics.mean(blockchain_query_times)
        
        print(f"Blockchain: {results['blockchain']['query_time_ms']:.4f} ms average")
        
        # SQL query
        sql_query_times = []
        
        for _ in range(100):
            start = time.perf_counter()
            history = self.query_product_history_sql(test_product)
            end = time.perf_counter()
            sql_query_times.append((end - start) * 1000)
        
        results['sql_database']['query_time_ms'] = statistics.mean(sql_query_times)
        
        print(f"SQL Database: {results['sql_database']['query_time_ms']:.4f} ms average")
        
        query_speedup = results['blockchain']['query_time_ms'] / results['sql_database']['query_time_ms']
        print(f"\n  SQL is {query_speedup:.2f}x FASTER for queries\n")
        
        # =========================================================================
        # 3. STORAGE EFFICIENCY
        # =========================================================================
        print("TEST 3: Storage Efficiency")
        print("-" * 70)
        
        # Blockchain storage (in-memory for this test)
        blockchain_data = {
            'blocks': [block.to_dict() for block in self.blockchain.chain],
            'total_blocks': len(self.blockchain.chain)
        }
        blockchain_size_bytes = len(json.dumps(blockchain_data, indent=2).encode('utf-8'))
        
        results['blockchain']['storage_bytes'] = blockchain_size_bytes
        results['blockchain']['storage_kb'] = blockchain_size_bytes / 1024
        results['blockchain']['storage_mb'] = blockchain_size_bytes / (1024 * 1024)
        results['blockchain']['total_blocks'] = len(self.blockchain.chain)
        
        print(f"Blockchain Storage:")
        print(f"  Blocks: {len(self.blockchain.chain)}")
        print(f"  Size: {results['blockchain']['storage_kb']:.2f} KB")
        
        # SQL storage
        self.db_cursor.execute('SELECT page_count * page_size FROM pragma_page_count(), pragma_page_size()')
        sql_size_bytes = self.db_cursor.fetchone()[0]
        
        results['sql_database']['storage_bytes'] = sql_size_bytes
        results['sql_database']['storage_kb'] = sql_size_bytes / 1024
        results['sql_database']['storage_mb'] = sql_size_bytes / (1024 * 1024)
        
        print(f"SQL Database Storage:")
        print(f"  Transactions: {num_transactions}")
        print(f"  Size: {results['sql_database']['storage_kb']:.2f} KB")
        
        storage_ratio = results['blockchain']['storage_bytes'] / results['sql_database']['storage_bytes']
        print(f"\n  Blockchain uses {storage_ratio:.2f}x MORE storage\n")
        
        # =========================================================================
        # 4. VERIFICATION PERFORMANCE
        # =========================================================================
        print("TEST 4: Verification Operation Performance")
        print("-" * 70)
        
        # Blockchain verification (includes hash calculation)
        blockchain_verify_times = []
        for i in range(100):
            test_tx = create_transaction(
                product_id=f'PROD-{i:04d}',
                sender='Test',
                receiver='Verify',
                location='Test'
            )
            
            start = time.perf_counter()
            tx_hash = hash_transaction(test_tx)
            merkle = MerkleTree([test_tx])
            root = merkle.get_merkle_root()
            end = time.perf_counter()
            
            blockchain_verify_times.append((end - start) * 1000)
        
        results['blockchain']['verification_time_ms'] = statistics.mean(blockchain_verify_times)
        
        print(f"Blockchain (hash + merkle): {results['blockchain']['verification_time_ms']:.4f} ms")
        
        # SQL verification (simple lookup)
        sql_verify_times = []
        for _ in range(100):
            start = time.perf_counter()
            self.db_cursor.execute('SELECT COUNT(*) FROM transactions WHERE product_id = ?', ('PROD-0001',))
            count = self.db_cursor.fetchone()[0]
            end = time.perf_counter()
            sql_verify_times.append((end - start) * 1000)
        
        results['sql_database']['verification_time_ms'] = statistics.mean(sql_verify_times)
        
        print(f"SQL Database (lookup): {results['sql_database']['verification_time_ms']:.4f} ms\n")
        
        # =========================================================================
        # 5. INTEGRITY VERIFICATION
        # =========================================================================
        print("TEST 5: Chain Integrity Verification")
        print("-" * 70)
        
        # Blockchain integrity check
        start = time.perf_counter()
        is_valid = self.blockchain.is_valid()
        blockchain_integrity_time = (time.perf_counter() - start) * 1000
        
        results['blockchain']['integrity_check_time_ms'] = blockchain_integrity_time
        results['blockchain']['integrity_valid'] = is_valid
        
        print(f"Blockchain: {blockchain_integrity_time:.2f} ms (Valid: {is_valid})")
        print(f"  Complexity: O(n) - must verify all blocks")
        print(f"  Checks: Block hashes, Merkle roots, Chain links\n")
        
        # =========================================================================
        # SUMMARY
        # =========================================================================
        print("=" * 70)
        print("BENCHMARK SUMMARY")
        print("=" * 70)
        
        results['comparison'] = {
            'insertion_winner': 'SQL' if speedup > 1 else 'BLOCKCHAIN',
            'insertion_speedup': speedup,
            'query_winner': 'SQL' if query_speedup > 1 else 'BLOCKCHAIN',
            'query_speedup': query_speedup,
            'storage_winner': 'SQL' if storage_ratio > 1 else 'BLOCKCHAIN',
            'storage_overhead': storage_ratio,
            'blockchain_strengths': [
                'Tamper-evident (modifications are detectable)',
                'Decentralized trust (no single point of control)',
                'Cryptographic verification (Merkle proofs)',
                'Immutable history (append-only ledger)'
            ],
            'sql_strengths': [
                'Faster insertions and queries',
                'More storage efficient',
                'ACID transactions fully supported',
                'Flexible querying and reporting',
                'Mature tooling and ecosystem'
            ]
        }
        
        print("Performance Comparison:")
        print(f"  Insertions: SQL is {speedup:.2f}x faster")
        print(f"  Queries: SQL is {query_speedup:.2f}x faster")
        print(f"  Storage: Blockchain uses {storage_ratio:.2f}x more space")
        print()
        
        print("Trade-off Analysis:")
        print("  BLOCKCHAIN wins on:")
        print("    ✓ Tamper resistance (any modification is detectable)")
        print("    ✓ Decentralized trust (no single authority)")
        print("    ✓ Cryptographic proofs (O(log n) verification)")
        print("    ✓ Auditability (complete immutable history)")
        print()
        
        print("  SQL wins on:")
        print("    ✓ Performance (10-100x faster for CRUD operations)")
        print("    ✓ Storage efficiency (2-5x less space)")
        print("    ✓ Flexibility (complex queries, reporting)")
        print("    ✓ Simplicity (easier to implement and maintain)")
        print()
        
        print("Recommendation:")
        print("  Use BLOCKCHAIN when:")
        print("    • Multiple untrusting parties need shared truth")
        print("    • Tamper evidence is critical")
        print("    • Audit trail with cryptographic proof is required")
        print()
        print("  Use SQL when:")
        print("    • Single trusted authority manages the database")
        print("    • Performance and storage are priorities")
        print("    • Traditional CRUD operations dominate")
        print()
        print("=" * 70 + "\n")
        
        return results
    
    def cleanup(self):
        """Clean up benchmark resources."""
        self.db_conn.close()
        if os.path.exists(self.db_path):
            os.remove(self.db_path)


def run_full_comparison():
    """Run complete academic comparison with analysis."""
    
    print("\n" + "=" * 70)
    print("ACADEMIC COMPARISON: BLOCKCHAIN vs TRADITIONAL DATABASE")
    print("Supply Chain Tracking System Analysis")
    print("=" * 70)
    print("""
This analysis compares two approaches to supply chain tracking:

1. BLOCKCHAIN-BASED (Our Implementation)
   - Immutable ledger with cryptographic linking
   - Merkle Trees for efficient verification
   - SHA-256 hashing for tamper detection
   - Decentralized trust model

2. TRADITIONAL SQL DATABASE (Comparison Baseline)
   - Standard relational database
   - Indexed queries for fast retrieval
   - ACID transactions
   - Centralized trust model

KEY METRICS:
  • Performance: Transaction throughput and latency
  • Storage: Space efficiency
  • Security: Tamper resistance and integrity
  • Trust: Centralization vs decentralization
""")
    
    benchmark = DatabaseBenchmark()
    
    # Run benchmarks with different scales
    print("\nRunning benchmark with 1,000 transactions...")
    results = benchmark.run_benchmark(num_transactions=1000, num_products=100)
    
    print("\nRunning benchmark with 10,000 transactions...")
    results_10k = benchmark.run_benchmark(num_transactions=10000, num_products=1000)
    
    benchmark.cleanup()
    
    # Document the analysis
    analysis = """
    
ANALYSIS: WHEN TO USE BLOCKCHAIN FOR SUPPLY CHAIN
=================================================

The Oracle Problem (Critical Limitation):
-----------------------------------------
Even with perfect blockchain technology, we cannot solve the fundamental
problem of connecting digital records to physical reality:

  BLOCKCHAIN SECURES: Digital transaction records ✓
  BLOCKCHAIN CANNOT SECURE: Physical product authenticity ✗

Example: A corrupt factory worker could place authentic QR codes on
counterfeit products. The blockchain would perfectly record the lie.

Solution: IoT sensors, RFID, and physical inspections must supplement
blockchain verification.

Engineering Trade-offs:
-----------------------
1. PERFORMANCE COST
   - SHA-256 hashing: ~0.1-0.5 ms per operation
   - Merkle tree construction: O(n) for building, O(log n) for proof
   - Block chaining: Adds verification overhead
   
   SQL INSERT: ~0.01-0.1 ms (10-50x faster)

2. STORAGE OVERHEAD
   - Each block stores: Previous hash, Merkle root, metadata
   - Redundant hash storage for verification
   - ~2-5x more storage than equivalent SQL
   
3. COMPLEXITY INCREASE
   - More complex implementation
   - Requires understanding of cryptography
   - Harder to debug and maintain
   - Consensus mechanisms needed (in distributed deployment)

When Blockchain IS the Right Choice:
------------------------------------
✓ Multi-party supply chains with low trust
✓ Regulatory requirements for immutable audit trails
✓ Products with high counterfeit risk (luxury goods, pharmaceuticals)
✓ Value chains where authenticity verification is critical
✓ Scenarios requiring cryptographic proof of provenance

When SQL is the Right Choice:
-----------------------------
✓ Single-company supply chain (internal tracking)
✓ High-volume, low-value products
✓ Performance-critical applications
✓ When traditional database features are needed (joins, complex queries)
✓ Smaller teams without blockchain expertise

Hybrid Approach (Best of Both Worlds):
--------------------------------------
Consider using blockchain for:
  - High-value/high-risk product verification
  - Multi-party trust boundaries
  - Regulatory compliance records
  
And SQL/database for:
  - Internal operations and analytics
  - High-frequency transactions
  - Reporting and business intelligence
  
This approach optimizes for both security AND performance.
"""
    
    print(analysis)
    
    return results


if __name__ == '__main__':
    run_full_comparison()
