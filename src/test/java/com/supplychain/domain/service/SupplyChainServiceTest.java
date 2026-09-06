package com.supplychain.domain.service;

import com.supplychain.domain.dto.BatchVerificationResult;
import com.supplychain.domain.dto.TransferResult;
import com.supplychain.domain.dto.VerificationResult;
import com.supplychain.domain.model.SupplyChainStage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the supply chain business logic.
 */
class SupplyChainServiceTest {

    @Test
    void rejectsUnregisteredManufacturers() {
        SupplyChainService service = new SupplyChainService();

        assertThrows(IllegalArgumentException.class, () ->
                service.manufactureProduct("ROGUE", Collections.singletonList("P1"), "Here", "B1", null));
    }

    @Test
    void supportsFullProductLifecycle() {
        SupplyChainService service = new SupplyChainService();
        service.registerManufacturer("FACTORY-A");

        service.manufactureProduct("FACTORY-A", Collections.singletonList("PROD-1"), "Floor", "B1", null);
        assertTrue(service.transferOwnership("PROD-1", "FACTORY-A", "DIST", "W1", SupplyChainStage.DISTRIBUTION, null).isSuccess());
        TransferResult sale = service.sellToConsumer("PROD-1", "DIST", "CONSUMER", "Store", 10.0, null);

        assertTrue(sale.isSuccess());
        assertNotNull(sale.getProvenance());

        VerificationResult verification = service.verifyProduct("PROD-1");
        assertTrue(verification.isVerified());
        assertTrue(verification.isAuthentic());
        assertEquals(3, verification.getJourneyLength());
    }

    @Test
    void flagsUnknownProductsAsCounterfeit() {
        SupplyChainService service = new SupplyChainService();

        TransferResult transfer = service.transferOwnership(
                "PROD-UNKNOWN", "A", "B", "Nowhere", SupplyChainStage.DISTRIBUTION, null);

        assertFalse(transfer.isSuccess());
        assertTrue(transfer.isCounterfeit());

        VerificationResult verification = service.verifyProduct("PROD-UNKNOWN");
        assertFalse(verification.isAuthentic());
        assertEquals("PRODUCT_NOT_IN_REGISTRY", verification.getReason());
    }

    @Test
    void verifiesBatches() {
        SupplyChainService service = new SupplyChainService();
        service.registerManufacturer("FACTORY-A");
        service.manufactureProduct("FACTORY-A", Arrays.asList("P1", "P2"), "Floor", "B1", null);

        BatchVerificationResult batch = service.verifyBatch(Arrays.asList("P1", "P2", "FAKE"));

        assertEquals(3, batch.getTotalProducts());
        assertEquals(2, batch.getVerifiedProducts());
        assertEquals(1, batch.getFailedProducts());
        assertFalse(batch.isBatchAuthentic());
    }
}
