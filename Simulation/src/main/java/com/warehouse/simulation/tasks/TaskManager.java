package com.warehouse.simulation.tasks;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Map;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.warehouse.simulation.storage.Order;
import com.warehouse.simulation.logging.LogManager;
import com.warehouse.simulation.storage.Item;
import com.warehouse.simulation.exceptions.ExceptionHandler;
import com.warehouse.simulation.app.model.OrdersStore;
import com.warehouse.simulation.app.model.StorageUnitsStore;
import com.warehouse.simulation.storage.Order;
import com.warehouse.simulation.storage.StorageUnit;
import com.warehouse.simulation.logging.LogManager;
import java.awt.Point;

public class TaskManager{

    private String taskmanagerId;
    private final ConcurrentLinkedDeque<Tasks> taskQueue = new ConcurrentLinkedDeque<>();
    private final ConcurrentMap<String, Tasks> activeTasks = new ConcurrentHashMap<>();
    private LogManager logManager;
    private final ConcurrentLinkedDeque<Tasks> completedTasksList = new ConcurrentLinkedDeque<>();
    private final CopyOnWriteArrayList<TaskListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger pendingCount = new AtomicInteger(0);
    private final java.util.concurrent.ConcurrentMap<String, Boolean> busyRobots = new java.util.concurrent.ConcurrentHashMap<>();
    private final static int MAX_COMPLETED_TASKS = 1000; // larger buffer for tests
    private final DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
    private final int MAX_ASSIGN_ATTEMPTS = 3;

    public TaskManager(String id) throws IOException{
        this.taskmanagerId = id;
        try {
            this.logManager = LogManager.getInstance("logs");
        } catch (Exception e) {
            // If logging cannot be initialized, continue without logging
            this.logManager = null;
            ExceptionHandler.handle(e, "tasks.TaskManager.<init>");
        }
    }

    public interface TaskListener {
        void onPendingCountChanged(int newPending);
        void onCompletedCountChanged(int newCompleted);
    }

    public void addListener(TaskListener l) { listeners.addIfAbsent(l); }
    public void removeListener(TaskListener l) { listeners.remove(l); }
    
    //------------------- GETTERS ------------------------------
    public String getTaskManagerId(){
        return taskmanagerId;
    }

    public List<Tasks> getTaskQueue() {
        return new ArrayList<>(taskQueue);
    }
    // get active tasks
    public Map<String, Tasks> getActiveTasks() {
        return new ConcurrentHashMap<>(activeTasks);
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public LinkedList<Tasks> getCompletedTasksList() {
        // return a copy to avoid concurrent modification issues in callers
        return new LinkedList<>(completedTasksList);
    }


    //------------------- METHODS ------------------------------

    public List<Tasks> getPendingTasks() {
        return new ArrayList<>(taskQueue);
    }

    public void createTasksFromOrders(Order order) throws IOException{
        //funktio saa parametrinä order objektin jonka se sitten jakaa itemeiksi ja itemeistä tehdään taskeja
        
        try {
            List<Item> items = order.getItems();
            // StorageUnit storageUnit = new StorageUnit(null, MAX_COMPLETED_TASKS, null);

            for (Item item : items) {
                Tasks t = null;
                try {
                    String suId = item.getStorageUnitId();
                    if (suId != null && !suId.isBlank()) {
                        // lookup storage unit and use its position as destination
                        StorageUnitsStore sus = StorageUnitsStore.getInstance();
                        StorageUnit su = sus.getUnits().stream().filter(x -> x.getId().equals(suId)).findFirst().orElse(null);
                        if (su != null) {
                            t = new Tasks(LocalDateTime.now().toString(), su.getPosition(), item);
                        }
                    }
                } catch (Throwable ignore) {}
                if (t == null) t = new Tasks(LocalDateTime.now().toString(), item);
                // associate this task with the originating order
                try { t.setOrderId(order.getId()); } catch (Throwable ignore) {}
                this.addTask(t);
            }
        }
        catch (Exception e){
            ExceptionHandler.handle(e, "tasks.TaskManager.createTasksFromOrders");
        }
    }

    public void addTask(Tasks task) {
        taskQueue.offer(task);
        int p = pendingCount.incrementAndGet();
        for (TaskListener l : listeners) {
            try { l.onPendingCountChanged(p); } catch (Throwable ignore) {}
        }
        // log task addition
        try {
            if (logManager != null) {
                String date = LocalDate.now().toString();
                String fileName = String.format("TaskManagerLogs/Tasks-%s.log", date);
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                String msg = String.format("%s - ADDED task %s status=PENDING order=%s dest=%s",
                        timestamp, task.getId(), task.getOrderId(), task.getDestination());
                logManager.writeLog(fileName, msg);
            }
        } catch (Exception e) {
            ExceptionHandler.handle(e, "tasks.TaskManager.addTask.logWrite");
        }
    }

    public Tasks robotGetTask() {
        Tasks t = taskQueue.poll();
        if (t != null) {
            int p = pendingCount.decrementAndGet();
            // mark as active
            try { t.setStatus(Tasks.TaskStatus.IN_PROGRESS); } catch (Throwable ignore) {}
            try { activeTasks.put(t.getId(), t); } catch (Throwable ignore) {}
            for (TaskListener l : listeners) {
                try { l.onPendingCountChanged(p); } catch (Throwable ignore) {}
            }
        }
        return t;
    }

    // Battery-aware assignment: robot provides id, position and current battery level.
    // We pick the first pending task that the robot can reasonably execute based on
    // a simple Manhattan-distance battery estimate.
    public Tasks robotGetTask(String robotId, Point robotPos, double batteryLevel) {
        try {
            // Attempt to claim the robot as busy to avoid races where the same robot
            // receives multiple assignments concurrently.
            boolean claimed = false;
            if (robotId != null) {
                claimed = busyRobots.putIfAbsent(robotId, Boolean.TRUE) == null;
                if (!claimed) return null;
            }

            if (taskQueue.isEmpty()) {
                if (claimed && robotId != null) busyRobots.remove(robotId);
                return null;
            }

            final double MOVE_COST_PER_BLOCK = 0.5; // estimate of battery cost per grid move
            final double SAFETY_MARGIN = 8.0; // reserve battery for safety / return/charging

            Tasks chosen = null;
            for (Tasks t : taskQueue) {
                // if destination is unknown accept (short task)
                Point dest = t.getDestination();
                double estCost = 0.0;
                if (dest != null && robotPos != null) {
                    estCost = (Math.abs(dest.x - robotPos.x) + Math.abs(dest.y - robotPos.y)) * MOVE_COST_PER_BLOCK;
                }
                if (batteryLevel >= estCost + SAFETY_MARGIN) {
                    // skip tasks that already failed too many times
                    if (t.getAttempts() >= MAX_ASSIGN_ATTEMPTS) {
                        // mark failed and continue
                        System.out.println("[TaskManager] Task " + t.getId() + " exceeded max attempts -> marking FAILED");
                        markTaskFailed(t);
                        continue;
                    }
                    chosen = t;
                    break;
                }
            }

            if (chosen != null) {
                boolean removed = taskQueue.remove(chosen);
                if (removed) {
                    chosen.incrementAttempts();
                    int p = pendingCount.decrementAndGet();
                    try { chosen.setStatus(Tasks.TaskStatus.IN_PROGRESS); } catch (Throwable ignore) {}
                    try { chosen.setRobotId(robotId); } catch (Throwable ignore) {}
                    try { activeTasks.put(chosen.getId(), chosen); } catch (Throwable ignore) {}
                    // mark robot as busy so it won't receive another assignment
                    // if we didn't already claim it, mark busy now
                    if (robotId != null && !claimed) busyRobots.put(robotId, Boolean.TRUE);
                    for (TaskListener l : listeners) {
                        try { l.onPendingCountChanged(p); } catch (Throwable ignore) {}
                    }
                    // write assignment log
                    try {
                        if (logManager != null) {
                            String date = LocalDate.now().toString();
                            String fileName = String.format("TaskManagerLogs/Tasks-%s.log", date);
                            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                            String msg = String.format("%s - ASSIGNED task %s to robot %s status=IN_PROGRESS attempts=%d order=%s dest=%s",
                                    timestamp, chosen.getId(), robotId, chosen.getAttempts(), chosen.getOrderId(), chosen.getDestination());
                            logManager.writeLog(fileName, msg);
                        }
                    } catch (Exception e) {
                        ExceptionHandler.handle(e, "tasks.TaskManager.robotGetTask.logWrite");
                    }
                    return chosen;
                }
            }

            // if we claimed busy earlier but didn't manage to assign a task, release claim
            if (claimed && robotId != null) busyRobots.remove(robotId);
        } catch (Throwable e) {
            ExceptionHandler.handle(e, "tasks.TaskManager.robotGetTask");
        }
        return null;
    }

    /** Mark a task as FAILED and remove it from system collections. */
    public void markTaskFailed(Tasks task) {
        try {
            if (task == null) return;
            try { task.setStatus(Tasks.TaskStatus.FAILED); } catch (Throwable ignore) {}
            try {
                Tasks removed = activeTasks.remove(task.getId());
                if (removed != null) {
                    String rid = removed.getRobotId();
                    if (rid != null) busyRobots.remove(rid);
                }
            } catch (Throwable ignore) {}
            // ensure it's not in pending queue
            taskQueue.removeIf(t -> t.getId().equals(task.getId()));
            completedTasksList.addLast(task);
            int completed = completedTasksList.size();
            for (TaskListener l : listeners) {
                try { l.onCompletedCountChanged(completed); } catch (Throwable ignore) {}
            }
            // log failure
            try {
                if (logManager != null) {
                    String date = LocalDate.now().toString();
                    String fileName = String.format("TaskManagerLogs/Tasks-%s.log", date);
                    String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                    String msg = String.format("%s - FAILED task %s order=%s robot=%s dest=%s",
                            timestamp, task.getId(), task.getOrderId(), task.getRobotId(), task.getDestination());
                    logManager.writeLog(fileName, msg);
                }
            } catch (Exception e) {
                ExceptionHandler.handle(e, "tasks.TaskManager.markTaskFailed.logWrite");
            }
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "tasks.TaskManager.markTaskFailed");
        }
    }

    public void requeueTask(Tasks task){
        try {
            if (task == null) return;
            // remove from active tasks if present
            try {
                Tasks removed = activeTasks.remove(task.getId());
                if (removed != null) {
                    String prev = removed.getRobotId();
                    if (prev != null) busyRobots.remove(prev);
                }
            } catch (Throwable ignore) {}
            // reset task metadata
            String prevRobot = null;
            try { prevRobot = task.getRobotId(); } catch (Throwable ignore) {}
            try { task.setRobotId(null); } catch (Throwable ignore) {}
            if (prevRobot != null) busyRobots.remove(prevRobot);
            try { task.setStatus(Tasks.TaskStatus.PENDING); } catch (Throwable ignore) {}

            // avoid duplicate entries in queue
            boolean exists = false;
            for (Tasks t : taskQueue) {
                if (t.getId().equals(task.getId())) { exists = true; break; }
            }
            if (!exists) {
                this.taskQueue.addLast(task);
                int p = pendingCount.incrementAndGet();
                for (TaskListener l : listeners) {
                    try { l.onPendingCountChanged(p); } catch (Throwable ignore) {}
                }
                // log requeue
                try {
                    if (logManager != null) {
                        String date = LocalDate.now().toString();
                        String fileName = String.format("TaskManagerLogs/Tasks-%s.log", date);
                        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                        String msg = String.format("%s - REQUEUED task %s status=PENDING previousRobot=%s order=%s dest=%s",
                                timestamp, task.getId(), prevRobot, task.getOrderId(), task.getDestination());
                        logManager.writeLog(fileName, msg);
                    }
                } catch (Exception e) {
                    ExceptionHandler.handle(e, "tasks.TaskManager.requeueTask.logWrite");
                }
            }
        }
        catch (Exception e) {
            ExceptionHandler.handle(e, "tasks.TaskManager.requeueTask");
        }
    }

    public void completeTask(Tasks task) {
        // finalize task status and remove from active map
        try { task.setStatus(Tasks.TaskStatus.COMPLETED); } catch (Throwable ignore) {}
        try {
            Tasks removed = activeTasks.remove(task.getId());
            if (removed != null) {
                String rid = removed.getRobotId();
                if (rid != null) busyRobots.remove(rid);
            }
        } catch (Throwable ignore) {}
        completedTasksList.addLast(task);
        try{
            while (this.completedTasksList.size() > MAX_COMPLETED_TASKS) {
                this.completedTasksList.removeFirst();
            }
        }
        catch (Exception e){
            ExceptionHandler.handle(e, "tasks.TaskManager.completeTask.manageCompletedList");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        try {
            if (logManager != null) {
                String date = LocalDate.now().toString();
                String fileName = String.format("TaskManagerLogs/Tasks-%s.log", date);
                String msg = String.format("%s - COMPLETED task %s order=%s robot=%s dest=%s",
                        timestamp, task.getId(), task.getOrderId(), task.getRobotId(), task.getDestination());
                logManager.writeLog(fileName, msg);
            } else {
                ExceptionHandler.handle(new RuntimeException("LogManager unavailable"), "tasks.TaskManager.completeTask.log");
            }
        } catch (Exception e) {
            ExceptionHandler.handle(e, "tasks.TaskManager.completeTask.logWrite");
        }
        // notify listeners about completed count
        int completed = completedTasksList.size();
        for (TaskListener l : listeners) {
            try { l.onCompletedCountChanged(completed); } catch (Throwable ignore) {}
        }

        // If the task belongs to an order, check if all tasks for that order are completed
        try {
            String orderId = task.getOrderId();
            if (orderId != null) {
                boolean pendingExists = false;
                // check pending queue
                for (Tasks t : taskQueue) {
                    if (orderId.equals(t.getOrderId())) { pendingExists = true; break; }
                }
                // check active tasks
                if (!pendingExists) {
                    for (Tasks t : activeTasks.values()) {
                        if (orderId.equals(t.getOrderId())) { pendingExists = true; break; }
                    }
                }
                if (!pendingExists) {
                    // all tasks for order are finished -> mark order as SHIPPED
                    OrdersStore store = OrdersStore.getInstance();
                    for (Order o : store.getOrders()) {
                        if (o.getId().equals(orderId)) {
                            o.setStatus(Order.Status.SHIPPED);
                            store.persist();

                            // remove items belonging to this order from storage units
                            try {
                                StorageUnitsStore sus = StorageUnitsStore.getInstance();
                                for (Item it : o.getItems()) {
                                    for (StorageUnit su : sus.getUnits()) {
                                        su.removeItems(it.getId());
                                    }
                                }
                                // persist storage units after removals
                                sus.persist();
                            } catch (Throwable remEx) {
                                // log or ignore but don't crash the task manager
                                remEx.printStackTrace();
                            }

                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignore) {}
    }

    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    /**
     * Clear all tasks: pending, active and completed. Notify listeners about counts.
     * Used by UI to flush all task-related data.
     */
    public void clearAllTasks() {
        try {
            taskQueue.clear();
            activeTasks.clear();
            completedTasksList.clear();
            pendingCount.set(0);
            for (TaskListener l : listeners) {
                try { l.onPendingCountChanged(0); } catch (Throwable ignore) {}
                try { l.onCompletedCountChanged(0); } catch (Throwable ignore) {}
            }
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "tasks.TaskManager.clearAllTasks");
        }
    }

    public void displayStatus() {
        System.out.println("Active Tasks: " + activeTasks.size());
        System.out.println("Pending Tasks: " + taskQueue.size());
    }


    //------------- That's me ,Artem. I created these methods for the third homework --------------


}