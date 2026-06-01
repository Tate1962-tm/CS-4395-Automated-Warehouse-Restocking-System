package com.awrs.service;

import com.awrs.model.*;
import com.awrs.model.RestockTask.Priority;
import com.awrs.model.RestockTask.Status;
import com.awrs.repository.*;
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
 * Demo 3 (Final Submission) — Test Suite: RestockService
 *
 * Covers: automated restock scan (batch), priority computation,
 *         task assignment, task completion (with inventory update),
 *         predictive analytics (SMA), audit logging for restock events,
 *         and duplicate-task guard.
 *
 * Maps to SRS §2.6 (Restocking Engine) + §2.7 (Predictive Analytics)
 */
@DisplayName("Demo 3 (Final) | RestockService Tests")
@ExtendWith(MockitoExtension.class)
class RestockServiceTest {

    @Mock private RestockTaskRepository mockRestockRepo;
    @Mock private InventoryRepository   mockInventoryRepo;
    @Mock private ItemRepository        mockItemRepo;
    @Mock private AuditLogRepository    mockAuditRepo;

    private RestockService restockService;

    // Item with minThreshold=10, maxThreshold=100, reorderPoint=20
    private Item widget;

    // Inventory record below minimum (qty=5)
    private InventoryRecord lowStockRecord;

    // Inventory record above minimum (qty=50)
    private InventoryRecord healthyRecord;

    @BeforeEach
    void setUp() {
        restockService = new RestockService(mockRestockRepo, mockInventoryRepo,
                                            mockItemRepo, mockAuditRepo);

        widget = new Item(1, "SKU-001", "Widget A", "SupplierCo",
                          "units", 10, 100, 20);

        lowStockRecord  = new InventoryRecord(1, 1, 5, 5,  "2024-01-01T00:00:00Z");
        healthyRecord   = new InventoryRecord(2, 1, 6, 50, "2024-01-01T00:00:00Z");
    }

    // -----------------------------------------------------------------------
    // runRestockScan — batch processing
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Scan creates a restock task for item below min threshold")
    void testRunRestockScan_CreatesTaskWhenBelowMin() {
        when(mockInventoryRepo.findAll()).thenReturn(List.of(lowStockRecord));
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockRestockRepo.findByItemAndLocation(1, 5)).thenReturn(List.of());

        List<RestockTask> tasks = restockService.runRestockScan();

        assertEquals(1, tasks.size());
        verify(mockRestockRepo).save(any(RestockTask.class));
    }

    @Test
    @DisplayName("Scan does NOT create task for healthy stock level")
    void testRunRestockScan_SkipsHealthyStock() {
        when(mockInventoryRepo.findAll()).thenReturn(List.of(healthyRecord));
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));

        List<RestockTask> tasks = restockService.runRestockScan();

        assertTrue(tasks.isEmpty());
        verify(mockRestockRepo, never()).save(any());
    }

    @Test
    @DisplayName("Scan does NOT create duplicate task when one is already pending")
    void testRunRestockScan_NoDuplicatePendingTask() {
        RestockTask pending = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        pending.setStatus(Status.PENDING);

        when(mockInventoryRepo.findAll()).thenReturn(List.of(lowStockRecord));
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockRestockRepo.findByItemAndLocation(1, 5)).thenReturn(List.of(pending));

        List<RestockTask> tasks = restockService.runRestockScan();

        assertTrue(tasks.isEmpty());
        verify(mockRestockRepo, never()).save(any());
    }

    @Test
    @DisplayName("Scan processes multiple records in one batch pass")
    void testRunRestockScan_BatchProcessesMultipleItems() {
        Item item2 = new Item(2, "SKU-002", "Gadget B", "VendorX", "units", 5, 50, 10);
        InventoryRecord low2 = new InventoryRecord(3, 2, 7, 2, "2024-01-01T00:00:00Z");

        when(mockInventoryRepo.findAll()).thenReturn(List.of(lowStockRecord, low2));
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockItemRepo.findById(2)).thenReturn(Optional.of(item2));
        when(mockRestockRepo.findByItemAndLocation(anyInt(), anyInt())).thenReturn(List.of());

        List<RestockTask> tasks = restockService.runRestockScan();

        assertEquals(2, tasks.size());
        verify(mockRestockRepo, times(2)).save(any());
    }

    @Test
    @DisplayName("Scan skips records whose item cannot be found")
    void testRunRestockScan_SkipsOrphanedRecord() {
        when(mockInventoryRepo.findAll()).thenReturn(List.of(lowStockRecord));
        when(mockItemRepo.findById(1)).thenReturn(Optional.empty());

        List<RestockTask> tasks = restockService.runRestockScan();

        assertTrue(tasks.isEmpty());
        verify(mockRestockRepo, never()).save(any());
    }

    @Test
    @DisplayName("Task quantity needed = maxThreshold - currentQty")
    void testRunRestockScan_CorrectQuantityNeeded() {
        when(mockInventoryRepo.findAll()).thenReturn(List.of(lowStockRecord));
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockRestockRepo.findByItemAndLocation(1, 5)).thenReturn(List.of());

        List<RestockTask> tasks = restockService.runRestockScan();

        // maxThreshold(100) - currentQty(5) = 95
        assertEquals(95, tasks.get(0).getQuantityNeeded());
    }

    // -----------------------------------------------------------------------
    // Priority computation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Restock task is CRITICAL when quantity is zero")
    void testPriority_CriticalWhenZero() {
        lowStockRecord.setQuantity(0);
        when(mockInventoryRepo.findAll()).thenReturn(List.of(lowStockRecord));
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockRestockRepo.findByItemAndLocation(1, 5)).thenReturn(List.of());

        List<RestockTask> tasks = restockService.runRestockScan();

        assertEquals(Priority.CRITICAL, tasks.get(0).getPriority());
    }

    @Test
    @DisplayName("Restock task is HIGH when quantity <= half of min threshold")
    void testPriority_HighWhenHalfMin() {
        // minThreshold=10, half=5, qty=4 => HIGH
        lowStockRecord.setQuantity(4);
        when(mockInventoryRepo.findAll()).thenReturn(List.of(lowStockRecord));
        when(mockItemRepo.findById(1)).thenReturn(Optional.of(widget));
        when(mockRestockRepo.findByItemAndLocation(1, 5)).thenReturn(List.of());

        List<RestockTask> tasks = restockService.runRestockScan();

        assertEquals(Priority.HIGH, tasks.get(0).getPriority());
    }

    // -----------------------------------------------------------------------
    // assignTask
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Assign task to a worker succeeds for PENDING task")
    void testAssignTask_Success() {
        RestockTask task = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        when(mockRestockRepo.findById(1)).thenReturn(Optional.of(task));

        assertTrue(restockService.assignTask(1, 42));
        assertEquals(Status.ASSIGNED, task.getStatus());
        assertEquals(42, task.getAssignedWorkerId());
        verify(mockRestockRepo).update(task);
    }

    @Test
    @DisplayName("Assign task fails for non-existent task id")
    void testAssignTask_NotFound() {
        when(mockRestockRepo.findById(999)).thenReturn(Optional.empty());
        assertFalse(restockService.assignTask(999, 1));
    }

    @Test
    @DisplayName("Assign task fails when task is already completed")
    void testAssignTask_AlreadyCompleted() {
        RestockTask task = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        task.setStatus(Status.COMPLETED);
        when(mockRestockRepo.findById(1)).thenReturn(Optional.of(task));
        assertFalse(restockService.assignTask(1, 10));
    }

    // -----------------------------------------------------------------------
    // completeTask
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Complete task updates task status to COMPLETED")
    void testCompleteTask_StatusUpdated() {
        RestockTask task = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        task.setStatus(Status.ASSIGNED);
        when(mockRestockRepo.findById(1)).thenReturn(Optional.of(task));
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(lowStockRecord));

        assertTrue(restockService.completeTask(1, 42));
        assertEquals(Status.COMPLETED, task.getStatus());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    @DisplayName("Complete task increases inventory quantity by quantityNeeded")
    void testCompleteTask_InventoryUpdated() {
        RestockTask task = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        when(mockRestockRepo.findById(1)).thenReturn(Optional.of(task));
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(lowStockRecord));

        restockService.completeTask(1, 42);

        // 5 (current) + 95 (needed) = 100
        assertEquals(100, lowStockRecord.getQuantity());
        verify(mockInventoryRepo).update(lowStockRecord);
    }

    @Test
    @DisplayName("Complete task writes an audit log entry")
    void testCompleteTask_AuditLogCreated() {
        RestockTask task = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        when(mockRestockRepo.findById(1)).thenReturn(Optional.of(task));
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.of(lowStockRecord));

        restockService.completeTask(1, 42);
        verify(mockAuditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Complete task fails for task that is already COMPLETED")
    void testCompleteTask_AlreadyCompleted() {
        RestockTask task = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        task.setStatus(Status.COMPLETED);
        when(mockRestockRepo.findById(1)).thenReturn(Optional.of(task));
        assertFalse(restockService.completeTask(1, 42));
    }

    @Test
    @DisplayName("Complete task fails for non-existent task id")
    void testCompleteTask_NotFound() {
        when(mockRestockRepo.findById(999)).thenReturn(Optional.empty());
        assertFalse(restockService.completeTask(999, 1));
    }

    @Test
    @DisplayName("Complete task creates new inventory record when none exists")
    void testCompleteTask_CreatesInventoryRecordIfMissing() {
        RestockTask task = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        when(mockRestockRepo.findById(1)).thenReturn(Optional.of(task));
        when(mockInventoryRepo.findByItemAndLocation(1, 5)).thenReturn(Optional.empty());

        boolean result = restockService.completeTask(1, 42);

        assertTrue(result);
        verify(mockInventoryRepo).save(any(InventoryRecord.class));
    }

    // -----------------------------------------------------------------------
    // getPendingTasks
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getPendingTasks returns only PENDING tasks")
    void testGetPendingTasks() {
        RestockTask t1 = new RestockTask(1, 1, 5, 95, Priority.HIGH, "2024-01-01T00:00:00Z");
        when(mockRestockRepo.findByStatus(Status.PENDING)).thenReturn(List.of(t1));

        List<RestockTask> pending = restockService.getPendingTasks();

        assertEquals(1, pending.size());
        assertEquals(Status.PENDING, pending.get(0).getStatus());
    }

    // -----------------------------------------------------------------------
    // Predictive Analytics — predictDaysUntilStockout
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Predicts correct days until stockout from SMA")
    void testPredictDaysUntilStockout_BasicCase() {
        // avg usage = 10/day, qty = 50 -> 5 days
        List<Integer> history = List.of(10, 10, 10, 10, 10);
        assertEquals(5, restockService.predictDaysUntilStockout(history, 50));
    }

    @Test
    @DisplayName("Prediction uses moving average of varying daily usage")
    void testPredictDaysUntilStockout_VariableUsage() {
        // avg = (5+10+15)/3 = 10, qty = 30 -> floor(30/10) = 3
        List<Integer> history = List.of(5, 10, 15);
        assertEquals(3, restockService.predictDaysUntilStockout(history, 30));
    }

    @Test
    @DisplayName("Prediction returns MAX_VALUE when usage history is empty")
    void testPredictDaysUntilStockout_EmptyHistory() {
        assertEquals(Integer.MAX_VALUE,
                     restockService.predictDaysUntilStockout(List.of(), 50));
    }

    @Test
    @DisplayName("Prediction returns MAX_VALUE when current quantity is zero")
    void testPredictDaysUntilStockout_ZeroQuantity() {
        assertEquals(Integer.MAX_VALUE,
                     restockService.predictDaysUntilStockout(List.of(10, 10), 0));
    }

    @Test
    @DisplayName("Prediction returns MAX_VALUE when average usage is zero")
    void testPredictDaysUntilStockout_ZeroUsage() {
        List<Integer> history = List.of(0, 0, 0);
        assertEquals(Integer.MAX_VALUE,
                     restockService.predictDaysUntilStockout(history, 50));
    }

    @Test
    @DisplayName("Prediction returns MAX_VALUE for null history")
    void testPredictDaysUntilStockout_NullHistory() {
        assertEquals(Integer.MAX_VALUE,
                     restockService.predictDaysUntilStockout(null, 50));
    }

    // -----------------------------------------------------------------------
    // Predictive Analytics — shouldFirePredictiveAlert
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Predictive alert fires when stockout predicted within lead time")
    void testShouldFirePredictiveAlert_AlertFires() {
        // avg=10, qty=20 -> 2 days; leadTime=5 -> alert fires
        List<Integer> history = List.of(10, 10, 10);
        assertTrue(restockService.shouldFirePredictiveAlert(history, 20, 5));
    }

    @Test
    @DisplayName("Predictive alert does NOT fire when stockout is beyond lead time")
    void testShouldFirePredictiveAlert_NoAlert() {
        // avg=1, qty=100 -> 100 days; leadTime=7 -> no alert
        List<Integer> history = List.of(1, 1, 1);
        assertFalse(restockService.shouldFirePredictiveAlert(history, 100, 7));
    }

    @Test
    @DisplayName("Predictive alert fires when days equals lead time exactly")
    void testShouldFirePredictiveAlert_ExactlyAtLeadTime() {
        // avg=10, qty=50 -> 5 days; leadTime=5 -> fires (<=)
        List<Integer> history = List.of(10, 10, 10);
        assertTrue(restockService.shouldFirePredictiveAlert(history, 50, 5));
    }
}
