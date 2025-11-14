package com.warehouse.simulation.robots;


import com.warehouse.simulation.utils.IGridEntity;
import com.warehouse.simulation.utils.PathFinder;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.tasks.Tasks;
import java.awt.Point;
import com.warehouse.simulation.charging.ChargingStation;
import com.warehouse.simulation.warehouse.Warehouse;
import com.warehouse.simulation.logging.LogManager;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;


public class Robot implements Runnable, IGridEntity  {
    
    private static int num = 0;
    private final String id;
    private Point currentPosition;
    private double batteryLevel;
    private final TaskManager taskManager;
    private Tasks currentTask;
    private LogManager logManager;
    public enum RobotState{
        IDLE,
        WORKING,
        CHARGING,
        WAITING_FOR_CHARGE,
        MOVING_TO_CHARGE,
        MOVING_TO_IDLE_POINT
    }
    public enum WorkingState {
        GOING_TO_PICKUP,
        GOING_TO_DROPOFF
    }
    
    private RobotState state;
    private RobotState lastLoggedState = null;
    private ChargingStation currentStation;
    public Warehouse warehouse;
    private final DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
    private PathFinder pathFinder;
    private Queue<Point> currentPath;
    private Point dropOffLocation;
    private Point robotsCamp;
    private WorkingState workingState;
    
    // ----------------------------------
    private int taskTimer = 0; // just a temporary solution
    private int chargeTimer = 0; // temporary solution
    // ----------------------------------

    // ----------- for second simulation ---------
    private long waitingStartTime;
    private static final long MAX_WAIT_TIME_MS = 30000;  // 15 seconds for test
    //----------------------------------------
    
    private static final double MAX_BATTERY = 100.0;
    private static final double LOW_BATTERY_THRESHOLD = 50.0;
    private static final double BATTERY_COST_PER_MOVE = 3; // ------------------------------------------------
    private static final double CHARGE_RATE_PER_TICK = 4.0;
    // Make tasks last roughly 10 seconds: with TICK_DELAY_MS=100ms, 100 ticks ≈ 10s
    private static final int TASK_DURATION_IN_TICKS = 100; // increased to show progress in UI (~10s)
    // charging lasts ~10s as well (100 ticks × 100ms)
    private static final int CHARGING_DURATION_IN_TICKS = 100;
    private static final double BATTERY_COST_PER_TICK = 0.5;
    // Use 1000ms tick so movement/battery updates are visible in the UI (~1s per step)
    private static final int TICK_DELAY_MS = 1000;
    
    public Robot(Warehouse warehouse, Point currentPosition, TaskManager taskManager, PathFinder pathFinder) {
        this.id = "robot_" + num;
        num++;
        this.currentPosition = currentPosition;
        this.batteryLevel = MAX_BATTERY;
        this.state = RobotState.IDLE;
        this.taskManager = taskManager;
        this.warehouse = warehouse;
        this.pathFinder = pathFinder;
        this.currentPath = new LinkedList<>();
        this.dropOffLocation = warehouse.getDropOffLocation();
        this.robotsCamp = warehouse.getIdleLocation();
        
        try {
            this.logManager = LogManager.getInstance("logs");
        } catch (Exception e) {
            System.out.println("LogManager wasn't created");
            this.logManager = null;
            System.err.println("Warning: could not initialize LogManager: " + e.getMessage());
        }
        
    }
    
    @Override
    public void run() {
        try {
            System.out.println("robot is started");
            while (true) {
                
                updateState();
                performAction();
                
                    Thread.sleep(TICK_DELAY_MS );
            }
        } catch (InterruptedException e) {
            System.out.println(id + " is stopped");
        }
    }
    
    public void updateState() {

        String fileName = null;

        if (logManager != null) {
            String date = df.format(LocalDate.now());
            fileName = String.format("RobotLogs/%s-%s.log", this.getID(), date);
        }


        if (state == RobotState.WAITING_FOR_CHARGE){
            long waitTime = System.currentTimeMillis() - this.waitingStartTime;

            if (waitTime > MAX_WAIT_TIME_MS) {
                warehouse.leaveQueue(this);
                this.state = RobotState.IDLE;

                String msg = String.format("[%s] Robot %s left the queue", LocalDateTime.now(), this.getID());
                logManager.writeLog(fileName, msg);
            }
        }
        
        
        
        else if (batteryLevel < LOW_BATTERY_THRESHOLD
                && state != RobotState.CHARGING
                && state != RobotState.MOVING_TO_CHARGE
                && state != RobotState.WAITING_FOR_CHARGE) {

            if (this.currentTask != null) {
                taskManager.requeueTask(this.currentTask);
                this.currentTask = null;
            }

            ChargingStation station = warehouse.requestCharging(this);
            if (station != null) {
                this.currentStation = station;
                this.state = RobotState.MOVING_TO_CHARGE;
                this.currentPath = pathFinder.findPath(this.currentPosition, station.getLocation());
                if (fileName != null) {
                    String msg = String.format("[%s] Robot %s starts moving to the charging station %s", LocalDateTime.now(), this.getID(), this.currentStation.getID());
                    logManager.writeLog(fileName, msg);
                }
            } else {
                this.state = RobotState.WAITING_FOR_CHARGE;
                if (fileName != null) {
                    String msg = String.format("[%s] Robot %s is in the charging queue", LocalDateTime.now(), this.getID());
                    logManager.writeLog(fileName, msg);
                }
                this.waitingStartTime = System.currentTimeMillis();
            }
        }

        else if (state == RobotState.MOVING_TO_CHARGE && (currentPath == null || currentPath.isEmpty())){
        	
            this.state = RobotState.CHARGING;
            this.chargeTimer = 0;
            
            
        }

        else if (state == RobotState.CHARGING && this.batteryLevel >= MAX_BATTERY) {
        	
            this.batteryLevel = MAX_BATTERY;
            this.chargeTimer = 0;
            
            if (this.currentStation != null) {
                warehouse.releaseStation(this.currentStation);
                this.currentStation = null;
            }
            
            this.state = RobotState.MOVING_TO_IDLE_POINT;
            this.currentPath = pathFinder.findPath(this.currentPosition, warehouse.getIdleLocation());
            System.out.println(warehouse.getIdleLocation());
        }

        else if (state == RobotState.MOVING_TO_IDLE_POINT && (currentPath == null || currentPath.isEmpty())){
            this.state = RobotState.IDLE;
            try { this.currentPosition = warehouse.getIdleLocation(); } catch (Throwable ignore) {}
            if (fileName != null) {
                String msg = String.format("[%s] Robot %s is at IDLE point and ready to get new tasks (battery=%.1f)", LocalDateTime.now(), this.getID(), this.batteryLevel);
                logManager.writeLog(fileName, msg);
            }
        }

        else if (state == RobotState.WORKING && (currentPath == null || currentPath.isEmpty())) {
            if (this.workingState == WorkingState.GOING_TO_PICKUP) {
                this.workingState = WorkingState.GOING_TO_DROPOFF;
                this.currentPath = pathFinder.findPath(this.currentPosition, this.dropOffLocation);
                if (this.currentPath == null) {
                    // cannot reach drop-off — requeue task and go idle
                    System.out.println("PATH NOT FOUND for drop-off of task " + (this.currentTask != null ? this.currentTask.getId() : "-"));
                    try { taskManager.requeueTask(this.currentTask); } catch (Throwable ignore) {}
                    this.currentTask = null;
                    this.state = RobotState.IDLE;
                    return;
                }
            } else {
                if (this.currentTask != null) {
                    try { this.currentPosition = this.dropOffLocation; } catch (Throwable ignore) {}
                    taskManager.completeTask(this.currentTask);
                    this.currentTask = null;
                }
                this.state = RobotState.MOVING_TO_IDLE_POINT;
                this.currentPath = pathFinder.findPath(this.currentPosition, warehouse.getIdleLocation());
            }
        }
        
        
    }
    
    public void performAction() {

        String fileName = null;
        
        if (logManager != null) {
            String date = df.format(LocalDate.now());
            fileName = String.format("RobotLogs/%s-%s.log", this.getID(), date);
        }
        
    switch (this.state) {
        case IDLE:
            // Try to obtain a task. If a task is obtained, log it. Otherwise
            // only log IDLE when the state actually changed to avoid flooding logs.
            tryToGetNewTask();
            if (this.currentTask != null) {
                if (fileName != null) {
                    String msg = String.format("[%s] Robot %s starts executing the new task with id: %s", LocalDateTime.now(), this.getID(), this.currentTask.getId());
                    logManager.writeLog(fileName, msg);
                }
                lastLoggedState = RobotState.WORKING;
            } else {
                if (fileName != null && lastLoggedState != RobotState.IDLE) {
                    String msg = String.format("[%s] Robot %s is IDLE (battery=%.1f)", LocalDateTime.now(), this.getID(), this.batteryLevel);
                    logManager.writeLog(fileName, msg);
                    lastLoggedState = RobotState.IDLE;
                }
            }
            break;

        case WORKING:
            workOnTask();
            if (fileName != null && lastLoggedState != RobotState.WORKING) {
                String taskId = this.currentTask != null ? this.currentTask.getId() : "-";
                String msg = String.format("[%s] Robot %s is working on task with id: %s (battery=%.1f)", LocalDateTime.now(), this.getID(), taskId, this.batteryLevel);
                logManager.writeLog(fileName, msg);
                lastLoggedState = RobotState.WORKING;
            }
            break;

        case MOVING_TO_CHARGE:
            workOnTask();
            if (fileName != null && lastLoggedState != RobotState.MOVING_TO_CHARGE) {
                String msg = String.format("[%s] Robot %s moving to charge (battery=%.1f)", LocalDateTime.now(), this.getID(), this.batteryLevel);
                logManager.writeLog(fileName, msg);
                lastLoggedState = RobotState.MOVING_TO_CHARGE;
            }
            break;

        case MOVING_TO_IDLE_POINT:
            workOnTask();
            if (fileName != null && lastLoggedState != RobotState.MOVING_TO_IDLE_POINT) {
                String msg = String.format("[%s] Robot %s moving to idle (battery=%.1f)", LocalDateTime.now(), this.getID(), this.batteryLevel);
                logManager.writeLog(fileName, msg);
                lastLoggedState = RobotState.MOVING_TO_IDLE_POINT;
            }
            break;

        case CHARGING:
            chargeBattery();
            if (fileName != null && lastLoggedState != RobotState.CHARGING) {
                String msg = String.format("[%s] Robot %s is charging (battery=%.1f)", LocalDateTime.now(), this.getID(), this.batteryLevel);
                logManager.writeLog(fileName, msg);
                lastLoggedState = RobotState.CHARGING;
            }
            break;

        case WAITING_FOR_CHARGE:
            // Similar to IDLE: log only on transition
            if (fileName != null && lastLoggedState != RobotState.WAITING_FOR_CHARGE) {
                String msg = String.format("[%s] Robot %s is waiting for charge (battery=%.1f)", LocalDateTime.now(), this.getID(), this.batteryLevel);
                logManager.writeLog(fileName, msg);
                lastLoggedState = RobotState.WAITING_FOR_CHARGE;
            }
            break;
        }
    }
    
    private void workOnTask() {
        // step along current path if available
        if (currentPath != null && !currentPath.isEmpty()) {
            this.currentPosition = currentPath.poll();
            this.batteryLevel -= BATTERY_COST_PER_MOVE;
            if (this.batteryLevel < 0) this.batteryLevel = 0;
            if (logManager != null) {
                String date = df.format(LocalDate.now());
                String fileName = String.format("RobotLogs/%s-%s.log", this.getID(), date);
                String msg = String.format("[%s] Robot %s moved to %s (battery=%.1f)", LocalDateTime.now(), this.getID(), this.currentPosition.toString(), this.batteryLevel);
                logManager.writeLog(fileName, msg);
            }
        }
    }
    
    private void chargeBattery() {
        this.chargeTimer++;
        this.batteryLevel += CHARGE_RATE_PER_TICK;
        if (this.batteryLevel > MAX_BATTERY)
            this.batteryLevel = MAX_BATTERY;
    }
    
    private void tryToGetNewTask() {
        Tasks newTask = taskManager.robotGetTask(this.id, this.currentPosition, this.batteryLevel);
        if (newTask != null) {
            this.currentTask = newTask;
            this.state = RobotState.WORKING;
            this.workingState = WorkingState.GOING_TO_PICKUP;
            this.currentPath = pathFinder.findPath(this.currentPosition, newTask.getDestination());
            if (this.currentPath == null) {
                // requeue the task so others can try; avoid leaving it in active/picked state
                System.out.println("PATH NOT FOUND for task " + newTask.getId());
                try {
                    taskManager.requeueTask(newTask);
                } catch (Throwable ignore) {}
                this.currentTask = null;
                this.state = RobotState.IDLE;
            }
        }

    }

    public boolean assignStation(ChargingStation station) {
        if (this.state == RobotState.WAITING_FOR_CHARGE) {
            this.currentStation = station;
            this.state = RobotState.CHARGING;
            this.chargeTimer = 0;
            this.currentPosition = station.getLocation();

            // logs in the future
            return true;
        }
        
        return false;
    }
    
    
    public double getBattery() {
        return this.batteryLevel;
    }
    
    public RobotState getState() {
        return this.state;
    }
    
    public Tasks getCurrentTask() {
        return this.currentTask;
    }
    
    
    
    // -------- IGridEntity methods -------- 
    @Override
    public Point getLocation() {
        return this.currentPosition;
    }
    
    @Override
    public String getID() {
        return this.id;
    }
    
    
    
    // ----------- Methods for uni tests --------
    
    public void setBatteryForTest(double p) {
        this.batteryLevel = p;
    }
    
    public void setTaskTimerForTest(int t) {
        this.taskTimer = t;
    }

    public void setStateForTest(RobotState state) {
        this.state = state;
    }

    public void setWorkingStateForTest(WorkingState state) {
        this.workingState = state;
    }

    public void setStationForTest(ChargingStation station) {
        this.currentStation = station;
    }

    public void setCurrentTaskForTest(Tasks task) {
        this.currentTask = task;
    }

    public void setCurrentPathForTest(Queue<Point> path) {
        this.currentPath = path;
    }

    public void setWaitingStartTimeForTest(long timeMs) {
        this.waitingStartTime = timeMs;
    }

    public WorkingState getWorkingState() {
        return this.workingState;
    }

    public Queue<Point> getCurrentPath() {
        return this.currentPath;
    }
    
    public Robot(TaskManager tm) {
    	this.id = "test_id";
    	this.taskManager = tm;
    }

    /**
     * Reset robot to idle position and full battery. Intended for UI/debug flush operations.
     */
    public void resetToIdle(java.awt.Point idle) {
        try {
            this.currentTask = null;
            this.currentPath.clear();
            this.currentStation = null;
            this.chargeTimer = 0;
            this.state = RobotState.IDLE;
            this.batteryLevel = MAX_BATTERY;
            if (idle != null) {
                this.currentPosition = new java.awt.Point(idle.x, idle.y);
            }
        } catch (Throwable ignore) {}
    }
    
}