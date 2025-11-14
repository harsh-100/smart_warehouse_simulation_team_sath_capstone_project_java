package com.warehouse.simulation.tasks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.IOException;

import com.warehouse.simulation.storage.Item;
import com.warehouse.simulation.storage.Order;

public class TaskManagerTest {

    @Test
    public void testAddAndRobotGetTask_simple() {
        System.out.println("[TEST] testAddAndRobotGetTask_simple starting");
        try {
            TaskManager tm = new TaskManager("TM1");
            Item it = new Item("IT1", "Thing", 1.0);
            Tasks t = new Tasks("T1", it);
            tm.addTask(t);
            Tasks assigned = tm.robotGetTask();
            assertNotNull(assigned);
            assertEquals(Tasks.TaskStatus.IN_PROGRESS, assigned.getStatus());
            assertEquals(1, tm.getActiveTaskCount());
        } catch (IOException e) {
            fail("TaskManager constructor threw IOException: " + e.getMessage());
        }
    }

    @Test
    public void testRobotGetTask_withBatteryAndPosition_assigns() {
        System.out.println("[TEST] testRobotGetTask_withBatteryAndPosition_assigns starting");
        try {
            TaskManager tm = new TaskManager("TM2");
            Item it = new Item("IT2", "Heavy", 4.0);
            Tasks t = new Tasks("T2", new Point(10, 10), it);
            tm.addTask(t);
            Tasks assigned = tm.robotGetTask("R1", new Point(0, 0), 200.0);
            assertNotNull(assigned);
            assertEquals("R1", assigned.getRobotId());
            assertEquals(1, tm.getActiveTaskCount());
        } catch (IOException e) {
            fail("TaskManager constructor threw IOException: " + e.getMessage());
        }
    }

    @Test
    public void testMarkTaskFailed_movesToCompleted() {
        System.out.println("[TEST] testMarkTaskFailed_movesToCompleted starting");
        try {
            TaskManager tm = new TaskManager("TM3");
            Item it = new Item("IT3", "Small", 0.5);
            Tasks t = new Tasks("T3", it);
            tm.addTask(t);
            Tasks assigned = tm.robotGetTask();
            assertNotNull(assigned);
            tm.markTaskFailed(assigned);
            assertEquals(0, tm.getActiveTaskCount());
            assertTrue(tm.getCompletedTasksList().stream().anyMatch(x -> x.getId().equals(assigned.getId())));
        } catch (IOException e) {
            fail("TaskManager constructor threw IOException: " + e.getMessage());
        }
    }

    @Test
    public void testRequeueTask_returnsToPending() {
        System.out.println("[TEST] testRequeueTask_returnsToPending starting");
        try {
            TaskManager tm = new TaskManager("TM4");
            Item it = new Item("IT4", "Sample", 2.2);
            Tasks t = new Tasks("T4", it);
            tm.addTask(t);
            Tasks assigned = tm.robotGetTask();
            assertNotNull(assigned);
            tm.requeueTask(assigned);
            assertEquals(0, tm.getActiveTaskCount());
            assertTrue(tm.getPendingTasks().stream().anyMatch(x -> x.getId().equals(t.getId())));
        } catch (IOException e) {
            fail("TaskManager constructor threw IOException: " + e.getMessage());
        }
    }

    @Test
    public void testClearAllTasks_andListenerNotifications() {
        System.out.println("[TEST] testClearAllTasks_andListenerNotifications starting");
        try {
            TaskManager tm = new TaskManager("TM5");
            final int[] pendingObserved = new int[]{-1};
            final int[] completedObserved = new int[]{-1};
            TaskManager.TaskListener listener = new TaskManager.TaskListener(){
                public void onPendingCountChanged(int newPending){ pendingObserved[0] = newPending; }
                public void onCompletedCountChanged(int newCompleted){ completedObserved[0] = newCompleted; }
            };
            tm.addListener(listener);
            // add two tasks
            tm.addTask(new Tasks("T5a", new Item("X1","A",1.0)));
            tm.addTask(new Tasks("T5b", new Item("X2","B",1.0)));
            tm.clearAllTasks();
            assertEquals(0, tm.getPendingTasks().size());
            assertEquals(0, tm.getActiveTaskCount());
            // listener should have been notified with zeros
            assertEquals(0, pendingObserved[0]);
            assertEquals(0, completedObserved[0]);
        } catch (IOException e) {
            fail("TaskManager constructor threw IOException: " + e.getMessage());
        }
    }
}
