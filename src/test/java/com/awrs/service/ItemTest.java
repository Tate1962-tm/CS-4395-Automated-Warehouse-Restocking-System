package com.awrs.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Demo 1 — Test Suite: Item Model (Catalog)
 *
 * Covers: Item construction, threshold configuration, SKU management.
 * Maps to SRS §2.2 — Item Catalog and Location Definitions
 */
@DisplayName("Demo 1 | Item Catalog Model Tests")
class ItemTest {

    private Item widget;

    @BeforeEach
    void setUp() {
        widget = new Item(1, "SKU-001", "Widget A", "SupplierCo",
                          "units", 10, 100, 20);
    }

    @Test
    @DisplayName("Item is created with correct SKU")
    void testItemCreation_Sku() {
        assertEquals("SKU-001", widget.getSku());
    }

    @Test
    @DisplayName("Item is created with correct description")
    void testItemCreation_Description() {
        assertEquals("Widget A", widget.getDescription());
    }

    @Test
    @DisplayName("Item is created with correct supplier")
    void testItemCreation_Supplier() {
        assertEquals("SupplierCo", widget.getSupplier());
    }

    @Test
    @DisplayName("Item min threshold is set correctly")
    void testItemThresholds_Min() {
        assertEquals(10, widget.getMinThreshold());
    }

    @Test
    @DisplayName("Item max threshold is set correctly")
    void testItemThresholds_Max() {
        assertEquals(100, widget.getMaxThreshold());
    }

    @Test
    @DisplayName("Item reorder point is set correctly")
    void testItemThresholds_ReorderPoint() {
        assertEquals(20, widget.getReorderPoint());
    }

    @Test
    @DisplayName("Min threshold must be less than max threshold")
    void testThresholdConstraint_MinLessThanMax() {
        assertTrue(widget.getMinThreshold() < widget.getMaxThreshold());
    }

    @Test
    @DisplayName("Reorder point must be greater than min threshold")
    void testThresholdConstraint_ReorderAboveMin() {
        assertTrue(widget.getReorderPoint() > widget.getMinThreshold());
    }

    @Test
    @DisplayName("setMinThreshold updates the value")
    void testSetMinThreshold() {
        widget.setMinThreshold(15);
        assertEquals(15, widget.getMinThreshold());
    }

    @Test
    @DisplayName("setSku updates the SKU")
    void testSetSku() {
        widget.setSku("SKU-999");
        assertEquals("SKU-999", widget.getSku());
    }

    @Test
    @DisplayName("setUnitOfMeasure updates the unit")
    void testSetUnitOfMeasure() {
        widget.setUnitOfMeasure("boxes");
        assertEquals("boxes", widget.getUnitOfMeasure());
    }

    @Test
    @DisplayName("toString contains SKU and description")
    void testToString() {
        String s = widget.toString();
        assertTrue(s.contains("SKU-001"));
        assertTrue(s.contains("Widget A"));
    }

    @Test
    @DisplayName("No-arg constructor creates non-null Item")
    void testNoArgConstructor() {
        Item blank = new Item();
        assertNotNull(blank);
    }
}
