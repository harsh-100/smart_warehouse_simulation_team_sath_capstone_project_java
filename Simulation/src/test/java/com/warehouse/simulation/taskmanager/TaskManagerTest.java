package com.warehouse.simulation.taskmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.warehouse.simulation.app.model.OrdersStore;
import com.warehouse.simulation.app.model.StorageUnitsStore;
import com.warehouse.simulation.storage.Item;
import com.warehouse.simulation.storage.Order;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.tasks.Tasks;
import org.junit.jupiter.api.AfterEach;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerTest {

    private TaskManager taskManager;

    // A helper listener to track notifications
    static class TestListener implements TaskManager.TaskListener {
        AtomicInteger pendingCount = new AtomicInteger(-1);
        AtomicInteger completedCount = new AtomicInteger(-1);

        @Override
        public void onPendingCountChanged(int newPending) {
            pendingCount.set(newPending);
        }

        @Override
        public void onCompletedCountChanged(int newCompleted) {
            completedCount.set(newCompleted);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        // Reset singleton stores before each test
        OrdersStore.getInstance().getOrders().clear();
        StorageUnitsStore.getInstance().getUnits().clear();
        
        // Initialize a new TaskManager
        taskManager = new TaskManager("test-tm");
    }

    @AfterEach
    void tearDown() {
        // Clear tasks to ensure no state leaks between tests
        taskManager.clearAllTasks();
    }

    
    @Test
    void testTaskLifecycle() {
        // 1. Add Task
        Tasks task = new Tasks("T1", new Item("I1", "Test Item", 45.6));
        taskManager.addTask(task);

        assertEquals(1, taskManager.getPendingTasks().size(), "Task should be in pending queue");
        assertEquals(0, taskManager.getActiveTaskCount(), "No tasks should be active");
        
        // 2. Get Task
        Tasks retrievedTask = taskManager.robotGetTask();

        assertEquals(task, retrievedTask, "Retrieved task should be the one we added");
        assertEquals(0, taskManager.getPendingTasks().size(), "Pending queue should be empty");
        assertEquals(1, taskManager.getActiveTaskCount(), "One task should be active");
        assertEquals(Tasks.TaskStatus.IN_PROGRESS, retrievedTask.getStatus(), "Task status should be IN_PROGRESS");
        assertTrue(taskManager.getActiveTasks().containsKey("T1"), "Task should be in active map");

        // 3. Complete Task
        taskManager.completeTask(retrievedTask);

        assertEquals(0, taskManager.getActiveTaskCount(), "Active tasks should be empty");
        assertFalse(taskManager.getActiveTasks().containsKey("T1"), "Task should be removed from active map");
        assertEquals(1, taskManager.getCompletedTasksList().size(), "One task should be in completed list");
        assertEquals(Tasks.TaskStatus.COMPLETED, retrievedTask.getStatus(), "Task status should be COMPLETED");
    }

    @Test
    void testCreateTasksFromOrders() throws IOException {
        // Create an order with two items
        Item item1 = new Item("I1", "Item 1", 1.1);
        Item item2 = new Item("I2", "Item 2", 1.3);
        Order order = new Order("O1");
        order.addItem(item1);
        order.addItem(item2);

        // Action
        taskManager.createTasksFromOrders(order);

        // Assertions
        assertEquals(2, taskManager.getPendingTasks().size(), "Should create 2 pending tasks");

        List<Tasks> pending = taskManager.getPendingTasks();
        // Check that tasks were created for the correct items and linked to the order
        assertTrue(pending.stream().anyMatch(t -> t.getItems().getId().equals("I1") && t.getOrderId().equals("O1")), "Task for Item 1 not found");
        assertTrue(pending.stream().anyMatch(t -> t.getItems().getId().equals("I2") && t.getOrderId().equals("O1")), "Task for Item 2 not found");
    }

    @Test
    void testListenerNotifications() {
        TestListener listener = new TestListener();
        taskManager.addListener(listener);

        Tasks task = new Tasks("T1", new Item("I1", "Test Item", 3.2));

        // Test addTask notification
        taskManager.addTask(task);
        assertEquals(1, listener.pendingCount.get(), "Listener pending count should be 1 after add");

        // Test robotGetTask notification
        Tasks activeTask = taskManager.robotGetTask();
        assertEquals(0, listener.pendingCount.get(), "Listener pending count should be 0 after get");

        // Test completeTask notification
        taskManager.completeTask(activeTask);
        assertEquals(1, listener.completedCount.get(), "Listener completed count should be 1 after complete");

        // Test clearAllTasks notification
        taskManager.clearAllTasks();
        assertEquals(0, listener.pendingCount.get(), "Listener pending count should be 0 after clear");
        assertEquals(0, listener.completedCount.get(), "Listener completed count should be 0 after clear");
    }

    @Test
    void testRequeueTask() {
        Tasks task1 = new Tasks("T1", new Item("I1", "Item 1", 2.4));
        Tasks task2 = new Tasks("T2", new Item("I2", "Item 2", 2.5));

        taskManager.addTask(task1);
        taskManager.addTask(task2); // Queue is now [T1, T2]

        // Robot picks up T1
        Tasks activeTask = taskManager.robotGetTask();
        assertEquals("T1", activeTask.getId());
        assertEquals(1, taskManager.getPendingTasks().size(), "Only T2 should be pending");

        // T1 fails and is re-queued
        taskManager.requeueTask(activeTask);
        assertEquals(2, taskManager.getPendingTasks().size(), "Both tasks should be pending again");

        System.out.println("There are now : " + taskManager.getPendingTasks());
        System.out.println("There are now : " + taskManager.getPendingTasks().size());
        // The next task retrieved should be T1 (because it was addedFirst)
        Tasks nextTask = taskManager.robotGetTask();
        assertEquals("T1", nextTask.getId(), "Re-queued task should be at the front");
        
        // The next task should be T2
        Tasks finalTask = taskManager.robotGetTask();
        assertEquals("T2", finalTask.getId(), "Second task should be next");
    }

    @Test
    void testOrderCompletionSetsStatus() throws IOException {
        // Setup: Create an order with two items and add it to the store
        Item item1 = new Item("I1", "Item 1", 2.34);
        Item item2 = new Item("I2", "Item 2", 2.54);
        Order order = new Order("O1");
        order.addItem(item2);
        order.addItem(item1);
        
        order.setStatus(Order.Status.PENDING);
        OrdersStore.getInstance().getOrders().add(order);

        // Create tasks from the order
        taskManager.createTasksFromOrders(order);
        assertEquals(2, taskManager.getPendingTasks().size());

        // Robot gets and completes the first task
        Tasks task1 = taskManager.robotGetTask();
        taskManager.completeTask(task1);

        // Assert: Order should NOT be shipped yet
        assertEquals(Order.Status.PENDING, order.getStatus(), "Order status should not change after 1st task");
        assertEquals(1, taskManager.getPendingTasks().size(), "One task should still be pending");

        // Robot gets and completes the second (and final) task
        Tasks task2 = taskManager.robotGetTask();
        taskManager.completeTask(task2);

        // Assert: Order SHOULD be shipped now
        assertEquals(Order.Status.SHIPPED, order.getStatus(), "Order status should be SHIPPED after final task");
        assertEquals(0, taskManager.getPendingTasks().size(), "No tasks should be pending");
        assertEquals(0, taskManager.getActiveTaskCount(), "No tasks should be active");
    }
}