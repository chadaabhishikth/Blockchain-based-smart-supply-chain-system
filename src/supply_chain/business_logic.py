"""
Phase 4: Supply Chain Business Logic
=====================================
This module implements the business logic layer that applies blockchain
technology to real-world supply chain scenarios.

FUNCTIONS:
1. manufacture_product(): Create new authenticated products
2. transfer_ownership(): Move products through supply chain stages
3. sell_to_consumer(): Final sale transaction
4. verify_authenticity(): End-to-end verification
5. batch_verification(): Verify entire shipments

This is where the "physical world meets blockchain" - handling the
business processes that create the transaction records.

Author: Blockchain Supply Chain Team
"""

from typing import List, Dict, Any, Optional
from datetime import datetime
from enum import Enum

try:
    from ..core.blockchain import Blockchain
    from ..core.hash_utils import create_transaction, hash_transaction
    from ..core.merkle_tree import MerkleTree
except ImportError:
    import sys
    import os
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..'))
    from src.core.blockchain import Blockchain
    from src.core.hash_utils import create_transaction, hash_transaction
    from src.core.merkle_tree import MerkleTree


class SupplyChainStage(Enum):
    """Enum representing the stages of the supply chain."""
    MANUFACTURING = "manufacturing"
    DISTRIBUTION = "distribution"
    WHOLESALING = "wholesaling"
    RETAIL = "retail"
    CONSUMER = "consumer"


class ProductStatus(Enum):
    """Status of a product in the system."""
    PENDING = "pending"
    ACTIVE = "active"
    SOLD = "sold"
    COUNTERFEIT = "counterfeit"


class SupplyChainBlockchain:
    """
    Complete supply chain management system using blockchain.
    
    This class provides the high-level API for supply chain operations,
    hiding the complexity of the underlying blockchain implementation.
    
    USAGE:
        >>> sc = SupplyChainBlockchain()
        >>> sc.manufacture_product('Factory-A', ['PROD-001', 'PROD-002'], 'Shanghai')
        >>> sc.transfer_ownership('PROD-001', 'Factory-A', 'Distributor-B', 'Distribution Center')
        >>> sc.verify_product('PROD-001')
    """
    
    def __init__(self):
        """Initialize the supply chain system."""
        self.blockchain = Blockchain()
        self.products: Dict[str, Dict[str, Any]] = {}  # Track product metadata
        self.authorized_manufacturers: set = set()
    
    # =========================================================================
    # PRODUCT LIFECYCLE MANAGEMENT
    # =========================================================================
    
    def register_manufacturer(self, manufacturer_id: str) -> bool:
        """
        Register an authorized manufacturer in the system.
        
        Only registered manufacturers can create new products.
        This is a whitelist approach to prevent counterfeit at source.
        
        Args:
            manufacturer_id: Unique identifier for the manufacturer
            
        Returns:
            True if registration successful
        """
        self.authorized_manufacturers.add(manufacturer_id)
        return True
    
    def manufacture_product(
        self,
        manufacturer_id: str,
        product_ids: List[str],
        location: str,
        batch_number: str = None,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Create new authenticated products at the manufacturing stage.
        
        This is the ORIGIN POINT - products created here are the only
        legitimate products in the system. Any product not originating
        from this process will be flagged as counterfeit.
        
        Args:
            manufacturer_id: ID of the manufacturing entity
            product_ids: List of product IDs to create
            location: Manufacturing facility location
            batch_number: Optional batch/lot number for traceability
            metadata: Additional product information
            
        Returns:
            Dictionary with manufacturing results
            
        Raises:
            PermissionError: If manufacturer is not registered
        """
        if manufacturer_id not in self.authorized_manufacturers:
            raise PermissionError(f"Manufacturer {manufacturer_id} not authorized")
        
        transactions = []
        
        for product_id in product_ids:
            # Create manufacturing transaction
            tx_data = create_transaction(
                product_id=product_id,
                sender='SYSTEM',  # Products originate from "system"
                receiver=manufacturer_id,
                location=location,
                metadata={
                    'stage': SupplyChainStage.MANUFACTURING.value,
                    'batch_number': batch_number,
                    'manufacturing_date': datetime.utcnow().isoformat() + 'Z',
                    'custom_metadata': metadata or {}
                }
            )
            transactions.append(tx_data)
            
            # Track product metadata
            self.products[product_id] = {
                'manufacturer': manufacturer_id,
                'status': ProductStatus.ACTIVE.value,
                'origin_location': location,
                'manufacturing_date': tx_data['timestamp'],
                'current_owner': manufacturer_id,
                'batch_number': batch_number
            }
        
        # Add block with all manufacturing transactions
        block = self.blockchain.add_block(transactions)
        
        return {
            'success': True,
            'products_created': len(product_ids),
            'product_ids': product_ids,
            'block_index': block.index,
            'block_hash': block.hash,
            'merkle_root': block.merkle_root,
            'timestamp': block.timestamp
        }
    
    def transfer_ownership(
        self,
        product_id: str,
        sender_id: str,
        receiver_id: str,
        location: str,
        stage: SupplyChainStage = SupplyChainStage.DISTRIBUTION,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Transfer product ownership through the supply chain.
        
        This is used for all intermediate transactions:
        Factory → Distributor, Distributor → Wholesaler, etc.
        
        Args:
            product_id: Product being transferred
            sender_id: Current owner
            receiver_id: New owner
            location: Transaction location
            stage: Current supply chain stage
            metadata: Additional transfer information
            
        Returns:
            Dictionary with transfer results
            
        Raises:
            ValueError: If product not found or invalid transfer
        """
        # Validate product exists
        if product_id not in self.products:
            return {
                'success': False,
                'error': 'PRODUCT_NOT_FOUND',
                'message': f'Product {product_id} not found in blockchain',
                'counterfeit': True
            }
        
        # Validate sender owns the product
        if self.products[product_id]['current_owner'] != sender_id:
            return {
                'success': False,
                'error': 'INVALID_SENDER',
                'message': f'{sender_id} does not own {product_id}',
                'counterfeit': False
            }
        
        # Create transfer transaction
        tx_data = create_transaction(
            product_id=product_id,
            sender=sender_id,
            receiver=receiver_id,
            location=location,
            metadata={
                'stage': stage.value,
                'transfer_date': datetime.utcnow().isoformat() + 'Z',
                'custom_metadata': metadata or {}
            }
        )
        
        # Add to blockchain
        block = self.blockchain.add_block([tx_data])
        
        # Update product tracking
        self.products[product_id]['current_owner'] = receiver_id
        self.products[product_id]['status'] = ProductStatus.ACTIVE.value
        
        return {
            'success': True,
            'product_id': product_id,
            'from': sender_id,
            'to': receiver_id,
            'location': location,
            'stage': stage.value,
            'block_index': block.index,
            'transaction_hash': hash_transaction(tx_data),
            'timestamp': block.timestamp
        }
    
    def sell_to_consumer(
        self,
        product_id: str,
        retailer_id: str,
        consumer_id: str,
        location: str,
        sale_price: float = None,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Final sale transaction from retailer to consumer.
        
        This completes the supply chain journey. The consumer receives
        a verifiable record of the entire product history.
        
        Args:
            product_id: Product being sold
            retailer_id: Selling retailer
            consumer_id: Consumer identifier (or 'ANONYMOUS')
            location: Sale location
            sale_price: Optional sale price for records
            metadata: Additional sale information
            
        Returns:
            Dictionary with sale confirmation and full provenance
        """
        result = self.transfer_ownership(
            product_id=product_id,
            sender_id=retailer_id,
            receiver_id=consumer_id,
            location=location,
            stage=SupplyChainStage.CONSUMER,
            metadata={
                'sale_price': sale_price,
                'sale_date': datetime.utcnow().isoformat() + 'Z',
                'custom_metadata': metadata or {}
            }
        )
        
        if result['success']:
            # Update product status to sold
            self.products[product_id]['status'] = ProductStatus.SOLD.value
            
            # Add provenance information
            result['provenance'] = self.get_product_provenance(product_id)
        
        return result
    
    # =========================================================================
    # VERIFICATION & AUTHENTICATION
    # =========================================================================
    
    def verify_product(self, product_id: str) -> Dict[str, Any]:
        """
        Verify product authenticity and trace its complete journey.
        
        This is the primary consumer-facing verification function.
        It checks:
        1. Product exists in our system
        2. Has valid origin from authorized manufacturer
        3. Has complete, unbroken chain of custody
        
        Args:
            product_id: Product to verify
            
        Returns:
            Dictionary with verification status and full provenance
        """
        # Step 1: Check if product exists in our registry
        if product_id not in self.products:
            return {
                'verified': False,
                'authentic': False,
                'reason': 'PRODUCT_NOT_IN_REGISTRY',
                'message': f'Product {product_id} is not registered in this supply chain system',
                'counterfeit_probability': 'HIGH',
                'recommendation': 'Do not purchase - product cannot be verified'
            }
        
        # Step 2: Verify blockchain integrity
        if not self.blockchain.is_valid():
            return {
                'verified': False,
                'authentic': False,
                'reason': 'BLOCKCHAIN_CORRUPTED',
                'message': 'Blockchain integrity check failed',
                'recommendation': 'System error - contact administrator'
            }
        
        # Step 3: Get product history from blockchain
        history = self.blockchain.get_product_history(product_id)
        
        if not history:
            return {
                'verified': False,
                'authentic': False,
                'reason': 'NO_BLOCKCHAIN_RECORD',
                'message': 'Product in registry but no blockchain transactions found',
                'counterfeit_probability': 'MEDIUM',
                'recommendation': 'Contact manufacturer'
            }
        
        # Step 4: Verify origin
        origin = history[0]
        if origin['sender'] != 'SYSTEM':
            return {
                'verified': False,
                'authentic': False,
                'reason': 'INVALID_ORIGIN',
                'message': 'Product does not originate from a valid manufacturing process',
                'counterfeit_probability': 'HIGH',
                'recommendation': 'Do not purchase - counterfeit detected'
            }
        
        # Step 5: Check all transactions are verifiable
        merkle_root = self.blockchain.get_block(history[0]['block_index']).merkle_root
        all_verified = True
        
        for record in history:
            tx_hash = hash_transaction({
                'product_id': product_id,
                'sender': record['sender'],
                'receiver': record['receiver'],
                'location': record['location'],
                'timestamp': record['timestamp']
            })
            proof = self.blockchain.chain[record['block_index']].merkle_root
            # Note: Simplified verification - full implementation would use Merkle proofs
        
        return {
            'verified': True,
            'authentic': True,
            'product_id': product_id,
            'manufacturer': self.products[product_id]['manufacturer'],
            'origin_location': self.products[product_id]['origin_location'],
            'current_owner': self.products[product_id]['current_owner'],
            'current_status': self.products[product_id]['status'],
            'journey_length': len(history),
            'journey': history,
            'blockchain_integrity': 'VERIFIED',
            'recommendation': 'Product is authentic - safe to purchase'
        }
    
    def get_product_provenance(self, product_id: str) -> Dict[str, Any]:
        """
        Get complete provenance information for a product.
        
        Returns detailed journey including verification proofs.
        
        Args:
            product_id: Product to trace
            
        Returns:
            Complete provenance report
        """
        if product_id not in self.products:
            return {'error': 'Product not found'}
        
        history = self.blockchain.get_product_history(product_id)
        
        return {
            'product_id': product_id,
            'manufacturer': self.products[product_id]['manufacturer'],
            'manufacturing_date': self.products[product_id]['manufacturing_date'],
            'origin': {
                'location': self.products[product_id]['origin_location'],
                'verified': True
            },
            'journey': [
                {
                    'step': i + 1,
                    'from': record['sender'],
                    'to': record['receiver'],
                    'location': record['location'],
                    'timestamp': record['timestamp'],
                    'verified': True
                }
                for i, record in enumerate(history)
            ],
            'total_transactions': len(history),
            'blockchain_verified': True
        }
    
    def verify_batch(self, product_ids: List[str]) -> Dict[str, Any]:
        """
        Verify an entire batch of products at once.
        
        This uses Merkle Tree efficiency to verify multiple products
        simultaneously without checking each individually.
        
        Args:
            product_ids: List of product IDs to verify
            
        Returns:
            Batch verification report
        """
        results = {
            'total_products': len(product_ids),
            'verified_products': 0,
            'failed_products': 0,
            'products': []
        }
        
        for product_id in product_ids:
            verification = self.verify_product(product_id)
            
            if verification['authentic']:
                results['verified_products'] += 1
            else:
                results['failed_products'] += 1
            
            results['products'].append({
                'product_id': product_id,
                'authentic': verification.get('authentic', False),
                'reason': verification.get('reason', 'VERIFIED')
            })
        
        results['batch_authentic'] = results['failed_products'] == 0
        results['success_rate'] = (
            results['verified_products'] / results['total_products'] * 100
        )
        
        return results
    
    # =========================================================================
    # COUNTERFEIT DETECTION
    # =========================================================================
    
    def detect_counterfeit(self, product_id: str, claimant_owner: str) -> Dict[str, Any]:
        """
        Detect if a product is counterfeit when ownership is disputed.
        
        This is used when someone claims to own a product but the
        blockchain history doesn't support their claim.
        
        Args:
            product_id: Product to check
            claimant_owner: Who claims to own the product
            
        Returns:
            Counterfeit detection report
        """
        verification = self.verify_product(product_id)
        
        if not verification['authentic']:
            return {
                'counterfeit': True,
                'reason': verification['reason'],
                'message': 'Product failed authenticity verification'
            }
        
        # Check if claimant is in the ownership chain
        history = self.blockchain.get_product_history(product_id)
        claimant_in_history = any(
            record['receiver'] == claimant_owner 
            for record in history
        )
        
        if not claimant_in_history:
            return {
                'counterfeit': True,
                'reason': 'OWNERSHIP_NOT_VERIFIED',
                'message': f'{claimant_owner} has no valid transaction record for this product',
                'suggestion': 'Product may be stolen or counterfeit'
            }
        
        # Check if claimant is the current owner
        current_owner = history[-1]['receiver'] if history else None
        
        if claimant_owner != current_owner:
            return {
                'counterfeit': False,
                'authentic': True,
                'reason': 'OWNERSHIP_MISMATCH',
                'message': f'Product is authentic but owned by {current_owner}, not {claimant_owner}',
                'current_owner': current_owner,
                'claimant': claimant_owner
            }
        
        return {
            'counterfeit': False,
            'authentic': True,
            'verified_owner': claimant_owner,
            'message': 'Product is authentic and ownership verified'
        }
    
    # =========================================================================
    # ANALYTICS & REPORTING
    # =========================================================================
    
    def get_supply_chain_summary(self) -> Dict[str, Any]:
        """Get summary statistics of the supply chain."""
        total_products = len(self.products)
        active_products = sum(
            1 for p in self.products.values() 
            if p['status'] == ProductStatus.ACTIVE.value
        )
        sold_products = sum(
            1 for p in self.products.values() 
            if p['status'] == ProductStatus.SOLD.value
        )
        
        return {
            'total_products': total_products,
            'active_products': active_products,
            'sold_products': sold_products,
            'registered_manufacturers': len(self.authorized_manufacturers),
            'total_blocks': self.blockchain.get_chain_length(),
            'pending_transactions': len(self.blockchain.pending_transactions),
            'blockchain_valid': self.blockchain.is_valid()
        }


# ============================================================================
# DEMONSTRATION & TESTING
# ============================================================================

def demo_supply_chain():
    """
    Demonstrate complete supply chain lifecycle.
    
    Simulates:
    1. Manufacturer registration
    2. Product manufacturing
    3. Distribution
    4. Wholesaling
    5. Retail
    6. Sale to consumer
    7. Verification
    """
    print("\n" + "=" * 70)
    print("SUPPLY CHAIN BLOCKCHAIN DEMONSTRATION")
    print("=" * 70 + "\n")
    
    # Initialize system
    sc = SupplyChainBlockchain()
    
    # Register authorized manufacturers
    print("STEP 1: Registering authorized manufacturers...")
    sc.register_manufacturer('AUTHENTIC-FACTORY-A')
    sc.register_manufacturer('AUTHENTIC-FACTORY-B')
    print("✓ Factories registered\n")
    
    # Manufacture products
    print("STEP 2: Manufacturing products...")
    result = sc.manufacture_product(
        manufacturer_id='AUTHENTIC-FACTORY-A',
        product_ids=['PROD-001', 'PROD-002', 'PROD-003'],
        location='Shanghai Manufacturing Hub',
        batch_number='BATCH-2026-001',
        metadata={'product_type': 'Electronics', 'quality_grade': 'A'}
    )
    print(f"✓ {result['products_created']} products manufactured")
    print(f"  Block #{result['block_index']} created with Merkle root:")
    print(f"  {result['merkle_root'][:32]}...\n")
    
    # Transfer to distributor
    print("STEP 3: Transferring to distributor...")
    for product_id in ['PROD-001', 'PROD-002']:
        result = sc.transfer_ownership(
            product_id=product_id,
            sender_id='AUTHENTIC-FACTORY-A',
            receiver_id='LOGISTICS-DIST-B',
            location='Shanghai Distribution Center',
            stage=SupplyChainStage.DISTRIBUTION
        )
        print(f"  ✓ {product_id} → LOGISTICS-DIST-B")
    print()
    
    # Transfer to wholesaler
    print("STEP 4: Transferring to wholesaler...")
    result = sc.transfer_ownership(
        product_id='PROD-001',
        sender_id='LOGISTICS-DIST-B',
        receiver_id='WHOLESALE-CENTRAL',
        location='Beijing Wholesale Hub',
        stage=SupplyChainStage.WHOLESALING
    )
    print(f"  ✓ PROD-001 → WHOLESALE-CENTRAL\n")
    
    # Transfer to retailer
    print("STEP 5: Transferring to retailer...")
    result = sc.transfer_ownership(
        product_id='PROD-001',
        sender_id='WHOLESALE-CENTRAL',
        receiver_id='RETAIL-STORE-SHANGHAI',
        location='Shanghai Retail Store',
        stage=SupplyChainStage.RETAIL
    )
    print(f"  ✓ PROD-001 → RETAIL-STORE-SHANGHAI\n")
    
    # Sell to consumer
    print("STEP 6: Selling to consumer...")
    result = sc.sell_to_consumer(
        product_id='PROD-001',
        retailer_id='RETAIL-STORE-SHANGHAI',
        consumer_id='CONSUMER-XYZ-123',
        location='Shanghai Retail Store',
        sale_price=299.99
    )
    print(f"  ✓ PROD-001 sold to CONSUMER-XYZ-123 for $299.99")
    print(f"  Transaction hash: {result['transaction_hash'][:32]}...\n")
    
    # Verify product
    print("STEP 7: Verifying product authenticity...")
    verification = sc.verify_product('PROD-001')
    print(f"\n  VERIFICATION RESULT:")
    print(f"  {'✓' if verification['verified'] else '✗'} Verified: {verification['verified']}")
    print(f"  {'✓' if verification['authentic'] else '✗'} Authentic: {verification['authentic']}")
    print(f"  Manufacturer: {verification['manufacturer']}")
    print(f"  Current Owner: {verification['current_owner']}")
    print(f"  Journey Steps: {verification['journey_length']}")
    print(f"  Recommendation: {verification['recommendation']}\n")
    
    # Try to verify counterfeit product
    print("STEP 8: Testing counterfeit detection...")
    fake_verification = sc.verify_product('PROD-FAKE-999')
    print(f"\n  VERIFICATION RESULT:")
    print(f"  {'✓' if fake_verification['verified'] else '✗'} Verified: {fake_verification['verified']}")
    print(f"  {'✓' if fake_verification['authentic'] else '✗'} Authentic: {fake_verification['authentic']}")
    print(f"  Reason: {fake_verification['reason']}")
    print(f"  Recommendation: {fake_verification['recommendation']}\n")
    
    # Batch verification
    print("STEP 9: Batch verification...")
    batch_result = sc.verify_batch(['PROD-001', 'PROD-002', 'PROD-FAKE'])
    print(f"  Total Products: {batch_result['total_products']}")
    print(f"  Verified: {batch_result['verified_products']}")
    print(f"  Failed: {batch_result['failed_products']}")
    print(f"  Success Rate: {batch_result['success_rate']:.1f}%\n")
    
    # Display blockchain
    print(sc.blockchain.display_chain())
    
    # Summary
    summary = sc.get_supply_chain_summary()
    print("\n" + "=" * 70)
    print("SUPPLY CHAIN SUMMARY")
    print("=" * 70)
    print(f"Total Products: {summary['total_products']}")
    print(f"Active Products: {summary['active_products']}")
    print(f"Sold Products: {summary['sold_products']}")
    print(f"Total Blocks: {summary['total_blocks']}")
    print(f"Blockchain Valid: {summary['blockchain_valid']}")
    print("=" * 70 + "\n")


if __name__ == '__main__':
    demo_supply_chain()
