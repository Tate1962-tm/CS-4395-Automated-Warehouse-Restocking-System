package com.awrs.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Demo 1 — Test Suite: WarehouseLocation Model
 *
 * Covers: hierarchical location construction, path resolution, type enum.
 * Maps to SRS §2.2 — Item Catalog and Location Definitions
 */
@DisplayName("Demo 1 | WarehouseLocation Model Tests")
class WarehouseLocationTest {

    private WarehouseLocation warehouse;
    private WarehouseLocation aisle;
    private WarehouseLocation bin;

    @BeforeEach
    void setUp() {
        warehouse = new WarehouseLocation(1, "WH1",
                WarehouseLocation.LocationType.WAREHOUSE, 0, "WH1");
        aisle     = new WarehouseLocation(2, "A3",
                WarehouseLocation.LocationType.AISLE, 1, "WH1 > A3");
        bin       = new WarehouseLocation(5, "B4",
                WarehouseLocation.LocationType.BIN, 4, "WH1 > A3 > S2 > B4");
    }

    @Test
    @DisplayName("Warehouse location created with correct name")
    void testCreation_Name() {
        assertEquals("WH1", warehouse.getName());
    }

    @Test
    @DisplayName("Warehouse root has parentId of 0")
    void testCreation_RootParentId() {
        assertEquals(0, warehouse.getParentId());
    }

    @Test
    @DisplayName("Aisle has correct parent pointing to warehouse")
    void testCreation_AisleParentId() {
        assertEquals(1, aisle.getParentId());
    }

    @Test
    @DisplayName("Location type is set correctly")
    void testCreation_LocationType() {
        assertEquals(WarehouseLocation.LocationType.WAREHOUSE, warehouse.getType());
        assertEquals(WarehouseLocation.LocationType.AISLE,     aisle.getType());
        assertEquals(WarehouseLocation.LocationType.BIN,        bin.getType());
    }

    @Test
    @DisplayName("Full path is stored and retrievable")
    void testCreation_FullPath() {
        assertEquals("WH1 > A3 > S2 > B4", bin.getFullPath());
    }

    @Test
    @DisplayName("setName updates the location name")
    void testSetName() {
        warehouse.setName("WH2");
        assertEquals("WH2", warehouse.getName());
    }

    @Test
    @DisplayName("setFullPath updates the path")
    void testSetFullPath() {
        bin.setFullPath("WH1 > A1 > S1 > B1");
        assertEquals("WH1 > A1 > S1 > B1", bin.getFullPath());
    }

    @Test
    @DisplayName("toString includes name and type")
    void testToString() {
        String s = warehouse.toString();
        assertTrue(s.contains("WH1"));
        assertTrue(s.contains("WAREHOUSE"));
    }

    @Test
    @DisplayName("No-arg constructor produces non-null object")
    void testNoArgConstructor() {
        assertNotNull(new WarehouseLocation());
    }
}
