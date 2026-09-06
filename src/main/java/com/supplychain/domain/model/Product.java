package com.supplychain.domain.model;

/**
 * Domain Layer — Product
 * ========================
 *
 * Mutable aggregate representing one physical product tracked by the
 * supply chain. It holds the registry metadata (origin, manufacturer,
 * batch) plus the mutable custody state (current owner, status).
 *
 * The immutable provenance itself lives on the blockchain; this object
 * is the service's fast in-memory index over that provenance.
 */
public class Product {

    private final String productId;
    private final String manufacturer;
    private final String originLocation;
    private final String manufacturingDate;
    private final String batchNumber;

    private String currentOwner;
    private ProductStatus status;

    /**
     * Create a product at the manufacturing stage. The manufacturer is
     * the first owner and the status is ACTIVE.
     */
    public Product(String productId, String manufacturer, String originLocation,
                   String manufacturingDate, String batchNumber) {
        this.productId = productId;
        this.manufacturer = manufacturer;
        this.originLocation = originLocation;
        this.manufacturingDate = manufacturingDate;
        this.batchNumber = batchNumber;
        this.currentOwner = manufacturer;
        this.status = ProductStatus.ACTIVE;
    }

    /**
     * Move custody of the product to a new owner.
     */
    public void transferTo(String newOwner) {
        this.currentOwner = newOwner;
        this.status = ProductStatus.ACTIVE;
    }

    /**
     * Mark the product as sold to a consumer.
     */
    public void markSold() {
        this.status = ProductStatus.SOLD;
    }

    public String getProductId() {
        return productId;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getOriginLocation() {
        return originLocation;
    }

    public String getManufacturingDate() {
        return manufacturingDate;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public String getCurrentOwner() {
        return currentOwner;
    }

    public ProductStatus getStatus() {
        return status;
    }
}
