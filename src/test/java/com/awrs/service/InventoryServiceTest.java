package com.awrs.service;

import com.awrs.model.InventoryRecord;
import com.awrs.model.Item;
import com.awrs.repository.AuditLogRepository;
import com.awrs.repository.InventoryRepository;
import com.awrs.repository.ItemRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Demo 2 — Test Suite: InventoryService
 *
 * Covers: receive shipments, fulfill orders, manual adjustments,
 *         stock query, insufficient stock guard, approval gate.
 *
 * Maps to SRS §2.3 (Receive), §2.4 (Fulfill), §2.5 (Adjustments)
 */
@DisplayName("Demo 2 | InventoryService Tests")
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository mockInventoryRepo;
    @Mock private ItemRepository      mockItemRepo;
    @Mock private AuditLogRepository  mockAuditRepo;

    private InventoryService inventoryService;

    private Item            widget;
    private InventoryRecord existingRecord;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(mockInventoryRepo, mockItemRepo, mockAuditRepo);

        widget = new Item(1, "SKU-001", "Widget A", "SupplierCo", "units", 10, 100, 20);
        existingRecord = new InventoryRecord(1, 1, 5, 50, "2024-01-01T00:00:00Z");
        //                                   id itemId locId qty  timestamp
    }

    // -----------------------------------------------------------------------
    // receiveShipment
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Receive shipment increases inventory quantity")
    void testReceiveShipment_IncreasesQuantity() {
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));

        boolean result = inventoryService.receiveShipment(1, 1, 5, 30);

        assertTrue(result);
        assertEquals(80, existingRecord.getQuantity()); // 50 + 30
        verify(mockInventoryRepo).update(existingRecord);
    }

    @Test
    @DisplayName("Receive shipment creates new record when none exists")
    void testReceiveShipment_CreatesNewRecord() {
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.empty());

        boolean result = inventoryService.receiveShipment(1, 1, 5, 20);

        assertTrue(result);
        verify(mockInventoryRepo).save(any(InventoryRecord.class));
    }

    @Test
    @DisplayName("Receive shipment writes an audit log entry")
    void testReceiveShipment_AuditLogCreated() {
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));

        inventoryService.receiveShipment(1, 1, 5, 10);
        verify(mockAuditRepo).save(any());
    }

    @Test
    @DisplayName("Receive shipment fails for quantity <= 0")
    void testReceiveShipment_ZeroQuantityFails() {
        assertFalse(inventoryService.receiveShipment(1, 1, 5, 0));
        verifyNoInteractions(mockInventoryRepo);
    }

    @Test
    @DisplayName("Receive shipment fails for negative quantity")
    void testReceiveShipment_NegativeQuantityFails() {
        assertFalse(inventoryService.receiveShipment(1, 1, 5, -5));
    }

    @Test
    @DisplayName("Receive shipment fails when item does not exist")
    void testReceiveShipment_ItemNotFound() {
        when(mockItemRepo.findById(999)).thenReturn(Optional.empty());
        assertFalse(inventoryService.receiveShipment(1, 999, 5, 10));
    }

    // -----------------------------------------------------------------------
    // fulfillOrder
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Fulfill order decreases inventory quantity")
    void testFulfillOrder_DecreasesQuantity() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));

        boolean result = inventoryService.fulfillOrder(1, 1, 5, 20);

        assertTrue(result);
        assertEquals(30, existingRecord.getQuantity()); // 50 - 20
        verify(mockInventoryRepo).update(existingRecord);
    }

    @Test
    @DisplayName("Fulfill order fails when stock is insufficient")
    void testFulfillOrder_InsufficientStock() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));
        assertFalse(inventoryService.fulfillOrder(1, 1, 5, 100)); // only 50 in stock
    }

    @Test
    @DisplayName("Fulfill order fails when inventory record does not exist")
    void testFulfillOrder_NoRecord() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.empty());
        assertFalse(inventoryService.fulfillOrder(1, 1, 5, 10));
    }

    @Test
    @DisplayName("Fulfill order fails for zero quantity")
    void testFulfillOrder_ZeroQuantity() {
        assertFalse(inventoryService.fulfillOrder(1, 1, 5, 0));
        verifyNoInteractions(mockInventoryRepo);
    }

    @Test
    @DisplayName("Fulfill order writes audit log entry")
    void testFulfillOrder_AuditLogCreated() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));
        inventoryService.fulfillOrder(1, 1, 5, 5);
        verify(mockAuditRepo).save(any());
    }

    // -----------------------------------------------------------------------
    // adjustInventory
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Positive adjustment increases quantity when manager-approved")
    void testAdjustInventory_PositiveDelta_Approved() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));
        assertTrue(inventoryService.adjustInventory(1, 1, 5, +10, "CORRECTION", true));
        assertEquals(60, existingRecord.getQuantity());
    }

    @Test
    @DisplayName("Negative adjustment decreases quantity when approved")
    void testAdjustInventory_NegativeDelta_Approved() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));
        assertTrue(inventoryService.adjustInventory(1, 1, 5, -10, "DAMAGE", true));
        assertEquals(40, existingRecord.getQuantity());
    }

    @Test
    @DisplayName("Adjustment fails when manager approval not given")
    void testAdjustInventory_NotApproved() {
        assertFalse(inventoryService.adjustInventory(1, 1, 5, -10, "LOSS", false));
        verifyNoInteractions(mockInventoryRepo);
    }

    @Test
    @DisplayName("Adjustment fails when result would go negative")
    void testAdjustInventory_WouldGoNegative() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));
        // current qty = 50, delta = -100 -> would be -50
        assertFalse(inventoryService.adjustInventory(1, 1, 5, -100, "DAMAGE", true));
    }

    @Test
    @DisplayName("Adjustment fails when inventory record not found")
    void testAdjustInventory_NoRecord() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.empty());
        assertFalse(inventoryService.adjustInventory(1, 1, 5, 5, "CORRECTION", true));
    }

    @Test
    @DisplayName("Adjustment writes audit log entry")
    void testAdjustInventory_AuditLogCreated() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));
        inventoryService.adjustInventory(1, 1, 5, -5, "DAMAGE", true);
        verify(mockAuditRepo).save(any());
    }

    // -----------------------------------------------------------------------
    // getQuantity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getQuantity returns current quantity for known item/location")
    void testGetQuantity_ReturnsCorrectValue() {
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(existingRecord));
        assertEquals(50, inventoryService.getQuantity(1, 5));
    }

    @Test
    @DisplayName("getQuantity returns -1 when no record exists")
    void testGetQuantity_NotFound() {
        when(mockInventoryRepo.findByItemAndLocation(9, 9)).thenReturn(Optional.empty());
        assertEquals(-1, inventoryService.getQuantity(9, 9));
    }

    // -----------------------------------------------------------------------
    // getInventoryAtLocation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getInventoryAtLocation returns all records for a location")
    void testGetInventoryAtLocation_ReturnsList() {
        when(mockInventoryRepo.findByLocation(5))
                .thenReturn(List.of(existingRecord));
        List<InventoryRecord> records = inventoryService.getInventoryAtLocation(5);
        assertEquals(1, records.size());
        assertEquals(50, records.get(0).getQuantity());
    }

    @Test
    @DisplayName("getInventoryAtLocation returns empty list when location is empty")
    void testGetInventoryAtLocation_Empty() {
        when(mockInventoryRepo.findByLocation(99)).thenReturn(List.of());
        assertTrue(inventoryService.getInventoryAtLocation(99).isEmpty());
    }
}
