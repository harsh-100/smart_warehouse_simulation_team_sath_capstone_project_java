package com.warehouse.simulation.warehouse;

import java.awt.Point;
import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;


import com.warehouse.simulation.charging.ChargingStation;
import com.warehouse.simulation.robots.Robot;
import com.warehouse.simulation.storage.Item;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.tasks.Tasks;
import com.warehouse.simulation.utils.PathFinder;
import com.warehouse.simulation.utils.WarehouseGrid;

public class Warehouse {
    
    private WarehouseGrid grid;
    private TaskManager taskManager;
    private List<Robot> robots;
    private List<ChargingStation> stations;
    private PathFinder pathFinder;
    private List<Thread> robotThreads = new ArrayList<>();
    private boolean simulationRunning = false;
    private Random random = new Random();
    private Queue<Robot> chargingQueue = new LinkedList<>();
    private Point idleLocation;
    private Point dropOffLocation;
    private Point chargingLocation;
    
    public Warehouse() {

        grid = new WarehouseGrid(10, 10); 
        try {
         taskManager = new TaskManager("TM1"); 
        } catch (IOException e) {
         System.err.println("TaskManager wasn't loaded");
            e.printStackTrace();
        }
        robots = new ArrayList<>();
        stations = new ArrayList<>();


    this.pathFinder = new PathFinder(grid);
    this.dropOffLocation = new Point(1,1);
    this.idleLocation = new Point(3,1);
    this.chargingLocation = new Point(2,1);
    createStations();
    createRobots();
        
    }

    /**
     * Create a Warehouse that uses an externally provided TaskManager.
     * Useful when UI wants to share the same TaskManager instance.
     */
    public Warehouse(TaskManager externalTaskManager) {
        grid = new WarehouseGrid(10, 10);
        this.taskManager = externalTaskManager;
        robots = new ArrayList<>();
        stations = new ArrayList<>();

        // initialize pathfinder and fixed locations so stations/robots get valid points
        this.pathFinder = new PathFinder(grid);
        this.dropOffLocation = new Point(1,1);
        this.idleLocation = new Point(3,1);
    this.chargingLocation = new Point(2,1);

        createStations();
        createRobots();
    }
    
    private void createStations() {
        ChargingStation station1 = new ChargingStation(this.chargingLocation);
        ChargingStation station2 = new ChargingStation(this.chargingLocation);

        stations.add(station1);
        stations.add(station2);

        grid.placeObject(station1, station1.getLocation());
        grid.placeObject(station2, station2.getLocation());
        
    }
    
    private void createRobots() {
        // create 5 fixed robots all starting at the idle/base location
        for (int i = 0; i < 5; i++) {
            Robot r = new Robot(this, new Point(idleLocation.x, idleLocation.y), taskManager, pathFinder);
            robots.add(r);
            try { grid.placeObject(r, r.getLocation()); } catch (Throwable ignore) {}
        }

    }

    public Point getIdleLocation() { return this.idleLocation; }
    public Point getDropOffLocation() { return this.dropOffLocation; }
    public Point getChargingLocation() { return this.chargingLocation; }


    
//    public synchronized ChargingStation getRandomAvailableStation() {

//     List<ChargingStation> available = stations.stream()
//             .filter(station -> station.isAvailable()).collect(Collectors.toList());
//
//     if (!available.isEmpty()) {
//            int randomIndex = random.nextInt(available.size());
//            return available.get(randomIndex);
//        }
//
//     return null;
//    }

    public synchronized ChargingStation requestCharging(Robot robot) {
        for (ChargingStation station : stations) {
            if (station.isAvailable()) {
                station.occupy(robot);
                return station;
            }
        }

        if (!chargingQueue.contains(robot))
            chargingQueue.add(robot);

        return null;
    }

    public synchronized void leaveQueue(Robot robot) {
        chargingQueue.remove(robot);
        // LOG IN THE FUTURE
    }

    public synchronized void releaseStation(ChargingStation station) {
        if (!chargingQueue.isEmpty()) {
            // find the first robot in the queue that is still waiting-for-charge
            Robot toAssign = null;
            for (Robot candidate : chargingQueue) {
                if (candidate == null) continue;
                try {
                    if (candidate.getState() == Robot.RobotState.WAITING_FOR_CHARGE) {
                        candidate.assignStation(station);
                        toAssign = candidate;
                        break;
                    }
                } catch (Throwable ignore) {
                    // ignore and continue
                }
            }
            if (toAssign != null) {
                // remove only the selected robot from the queue and occupy
                chargingQueue.remove(toAssign);
                station.occupy(toAssign);
            } else {
                // no valid waiting robot found -> release station
                station.release();
            }
        } else {
            station.release();
        }
    }
    
    /** Clear the charging queue (used when flushing data). */
    public synchronized void clearChargingQueue() {
        chargingQueue.clear();
    }
    
    public void startSimulation() {
        System.out.println("Sumulation is running");
        System.out.println(robots.size() + " robots were created");
        System.out.println(stations.size() + " stations were created");
        this.simulationRunning = true;
        
        // Start threads and keep references so we can stop them later
        for (Robot robot : robots) {
            Thread robotThread = new Thread(robot);
            robotThreads.add(robotThread);
            robotThread.start(); 
        }
    }

    /**
     * Stop the running robot threads by interrupting them.
     * This is a best-effort stop; robots should handle InterruptedException.
     */
    public void stopSimulation() {
        for (Thread t : robotThreads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        robotThreads.clear();
        System.out.println("Simulation stopped");
        this.simulationRunning = false;
    }

    /**
     * Return a copy of the robot list for UI/inspection.
     */
    public List<Robot> getRobots() {
        return new ArrayList<>(this.robots);
    }

    /** Return copy of charging stations for UI inspection */
    public List<ChargingStation> getStations() {
        return new ArrayList<>(this.stations);
    }

    /** Return snapshot of the charging queue */
    public List<Robot> getChargingQueueSnapshot() {
        return new ArrayList<>(this.chargingQueue);
    }

    /**
     * Return whether simulation is currently running (best-effort flag).
     */
    public boolean isSimulationRunning() {
        return this.simulationRunning;
    }

    public void task1_simulation(){

        ChargingStation station3 = new ChargingStation(new Point(9, 0));
        ChargingStation station4 = new ChargingStation(new Point(0, 0));

        stations.add(station3);
        stations.add(station4);

        grid.placeObject(station3, station3.getLocation());
        grid.placeObject(station4, station3.getLocation());

    Robot robot3 = new Robot(this, new Point(5, 5), taskManager, pathFinder);
    Robot robot4 = new Robot(this, new Point(0, 5), taskManager, pathFinder);
    Robot robot5 = new Robot(this, new Point(1, 5), taskManager, pathFinder);

        robots.add(robot3);
        robots.add(robot4);
        robots.add(robot5);

        System.out.println("Sumulation of the first subtask is running");
        System.out.println(robots.size() + " robots were created");
        System.out.println(stations.size() + " stations were created");

        for (Robot robot : robots) {
            robot.setBatteryForTest(19.0);
            Thread robotThread = new Thread(robot);
            robotThread.start();
        }

    }

    public void task2_simulation(){

    Robot robot3 = new Robot(this, new Point(5, 5), taskManager, pathFinder);
    Robot robot4 = new Robot(this, new Point(0, 5), taskManager, pathFinder);
    Robot robot5 = new Robot(this, new Point(1, 5), taskManager, pathFinder);

        robots.add(robot3);
        robots.add(robot4);
        robots.add(robot5);

        System.out.println("Sumulation of the first subtask is running");
        System.out.println(robots.size() + " robots were created");
        System.out.println(stations.size() + " stations were created");

        for (Robot robot : robots) {
            robot.setBatteryForTest(19.0);
            Thread robotThread = new Thread(robot);

            try {
                Thread.sleep(random.nextInt(2000));
            } catch (InterruptedException e) {}

            robotThread.start();
        }

    }

    public void task3_simulation(){

        System.out.println("Sumulation with tasks is running");
        System.out.println(robots.size() + " robots were created");
        System.out.println(stations.size() + " stations were created");

        for (int i = 0; i < 10; i++) {
            taskManager.addTask(new Tasks());
        }

        for (Robot robot : robots) {
            Thread robotThread = new Thread(robot);
            robotThread.start();
        }

        System.out.println("10 tasks have been added");
    }
    public static void main(String[] args) {
        Warehouse warehouse = new Warehouse();
        warehouse.task3_simulation();
    }

}