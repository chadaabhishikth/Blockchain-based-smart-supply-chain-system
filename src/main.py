#!/usr/bin/env python3
"""
Main Entry Point: Blockchain Supply Chain System
=================================================

This script demonstrates the complete blockchain-based supply chain system,
including:
1. SHA-256 cryptographic hashing
2. Merkle Tree verification
3. Blockchain ledger
4. Supply chain operations
5. Academic comparison with SQL

Run with: python src/main.py
"""

import sys
import os

# Add src to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))


def print_banner():
    """Display program banner."""
    print("""
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║     ███████╗███╗   ██╗███████╗███╗   ███╗██╗███████╗███████╗                   ║
║     ██╔════╝████╗  ██║██╔════╝████╗ ████║██║██╔════╝██╔════╝                   ║
║     ███████╗██╔██╗ ██║█████╗  ██╔████╔██║██║█████╗  ███████╗                   ║
║     ╚════██║██║╚██╗██║██╔══╝  ██║╚██╔╝██║██║██╔══╝  ╚════██║                   ║
║     ███████║██║ ╚████║███████╗██║ ╚═╝ ██║██║███████╗███████║                   ║
║     ╚══════╝╚═╝  ╚═══╝╚══════╝╚═╝     ╚═╝╚═╝╚══════╝╚══════╝                   ║
║                                                                              ║
║     S M A R T   S U P P L Y   C H A I N   S Y S T E M                        ║
║                                                                              ║
║     Blockchain-Powered Product Provenance & Authentication                   ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
    """)


def run_all_tests():
    """Run all unit tests for each phase."""
    print("\n" + "=" * 70)
    print("RUNNING ALL UNIT TESTS")
    print("=" * 70)
    
    # Phase 1: SHA-256 Hashing
    print("\n" + "─" * 70)
    print("PHASE 1: SHA-256 CRYPTOGRAPHIC HASHING")
    print("─" * 70)
    try:
        from core.hash_utils import (
            test_hash_consistency,
            test_avalanche_effect,
            test_hash_uniqueness,
            test_hash_length
        )
    except ImportError:
        from src.core.hash_utils import (
            test_hash_consistency,
            test_avalanche_effect,
            test_hash_uniqueness,
            test_hash_length
        )
    
    try:
        test_hash_consistency()
        test_avalanche_effect()
        test_hash_uniqueness()
        test_hash_length()
        print("\n✓ Phase 1: ALL TESTS PASSED")
    except Exception as e:
        print(f"\n✗ Phase 1: FAILED - {e}")
        return False
    
    # Phase 2: Merkle Tree
    print("\n" + "─" * 70)
    print("PHASE 2: MERKLE TREE IMPLEMENTATION")
    print("─" * 70)
    try:
        from core.merkle_tree import (
            test_merkle_tree_construction,
            test_proof_generation_and_verification,
            test_invalid_proof_rejection,
            test_single_item,
            test_odd_number_of_items,
            test_batch_verification,
            test_merkle_root_consistency
        )
    except ImportError:
        from src.core.merkle_tree import (
            test_merkle_tree_construction,
            test_proof_generation_and_verification,
            test_invalid_proof_rejection,
            test_single_item,
            test_odd_number_of_items,
            test_batch_verification,
            test_merkle_root_consistency
        )
    
    try:
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
        print("\n✓ Phase 2: ALL TESTS PASSED")
    except Exception as e:
        print(f"\n✗ Phase 2: FAILED - {e}")
        return False
    
    # Phase 3: Blockchain Ledger
    print("\n" + "─" * 70)
    print("PHASE 3: BLOCKCHAIN LEDGER")
    print("─" * 70)
    try:
        from core.blockchain import (
            test_genesis_block,
            test_block_chaining,
            test_chain_validation,
            test_product_history,
            test_verify_product,
            test_merkle_root_in_block
        )
    except ImportError:
        from src.core.blockchain import (
            test_genesis_block,
            test_block_chaining,
            test_chain_validation,
            test_product_history,
            test_verify_product,
            test_merkle_root_in_block
        )
    
    try:
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
        print("\n✓ Phase 3: ALL TESTS PASSED")
    except Exception as e:
        print(f"\n✗ Phase 3: FAILED - {e}")
        return False
    
    return True


def demo_complete_system():
    """Demonstrate the complete supply chain system."""
    print("\n" + "=" * 70)
    print("COMPLETE SYSTEM DEMONSTRATION")
    print("=" * 70)
    
    from supply_chain.business_logic import demo_supply_chain
    
    demo_supply_chain()


def run_benchmark():
    """Run performance benchmark comparison."""
    print("\n" + "=" * 70)
    print("PERFORMANCE BENCHMARK")
    print("=" * 70)
    
    from benchmarking.comparison import run_full_comparison
    
    results = run_full_comparison()
    return results


def main():
    """Main entry point."""
    print_banner()
    
    print("""
This system implements a complete blockchain-based supply chain solution
with the following key features:

✓ SHA-256 Cryptographic Hashing
  - Digital fingerprints for all transactions
  - Avalanche effect for tamper detection

✓ Merkle Trees
  - O(log n) verification complexity
  - Efficient batch verification
  - Space-optimized proofs

✓ Blockchain Ledger
  - Immutable transaction history
  - Cryptographic block chaining
  - Complete integrity verification

✓ Supply Chain Operations
  - Product manufacturing & tracking
  - Ownership transfer through supply chain
  - Counterfeit detection
  - Consumer verification

✓ Academic Comparison
  - Performance vs SQL databases
  - Trade-off analysis
  - Recommendations for real-world use
    """)
    
    # Ask user what to run
    print("\nSelect an option:")
    print("  1. Run all unit tests")
    print("  2. Demo complete supply chain system")
    print("  3. Run performance benchmark")
    print("  4. Run everything")
    print("  5. Exit")
    
    choice = input("\nEnter choice (1-5): ").strip()
    
    if choice == '1':
        success = run_all_tests()
        if success:
            print("\n🎉 ALL TESTS COMPLETED SUCCESSFULLY")
    
    elif choice == '2':
        demo_complete_system()
    
    elif choice == '3':
        run_benchmark()
    
    elif choice == '4':
        print("\n" + "=" * 70)
        print("FULL SYSTEM VALIDATION")
        print("=" * 70)
        
        print("\n[1/3] Running unit tests...")
        success = run_all_tests()
        
        if success:
            print("\n[2/3] Running system demonstration...")
            try:
                from supply_chain.business_logic import demo_supply_chain
            except ImportError:
                from src.supply_chain.business_logic import demo_supply_chain
            demo_supply_chain()
            
            print("\n[3/3] Running performance benchmark...")
            try:
                from benchmarking.comparison import run_full_comparison
            except ImportError:
                from src.benchmarking.comparison import run_full_comparison
            run_full_comparison()
            
            print("\n" + "=" * 70)
            print("🎉 FULL SYSTEM VALIDATION COMPLETE")
            print("=" * 70)
            print("""
All components are working correctly:
✓ Cryptographic hashing foundation
✓ Merkle Tree verification
✓ Blockchain ledger
✓ Supply chain operations
✓ Performance benchmarking
            """)
        else:
            print("\n✗ Tests failed - please fix errors before proceeding")
    
    elif choice == '5':
        print("\nExiting. Thank you for using the Blockchain Supply Chain System!\n")
    
    else:
        print("\nInvalid choice. Please enter 1, 2, 3, 4, or 5.")


if __name__ == '__main__':
    main()
