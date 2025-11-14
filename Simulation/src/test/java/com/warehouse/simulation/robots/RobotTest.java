package com.warehouse.simulation.robots;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.awt.Point;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import com.warehouse.simulation.charging.ChargingStation;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.tasks.Tasks;
import com.warehouse.simulation.utils.PathFinder;
import com.warehouse.simulation.warehouse.Warehouse;
import com.warehouse.simulation.robots.Robot.RobotState;
import com.warehouse.simulation.robots.Robot.WorkingState;

import static org.junit.jupiter.api.Assertions.*;

//------------mocks--------------


class MockTaskManager extends TaskManager {
	
    public MockTaskManager() throws IOException { 
    	super("mockTaskManager"); 
    	} 
    public Tasks taskToGive = null;
    public boolean taskWasCompleted = false;
    public boolean taskWasRequeued = false;
    public boolean robotGetTaskCalled = false;

    @Override 
    public synchronized Tasks robotGetTask() { 
    	robotGetTaskCalled = true; return taskToGive; 
    }
    
    @Override 
    public void completeTask(Tasks task) { 
    	taskWasCompleted = true; 
    }
    
    @Override 
    public synchronized void requeueTask(Tasks task) {
    	taskWasRequeued = true; 
    }
    
  
}


class MockWarehouse extends Warehouse {
	
    public MockWarehouse() throws IOException { 
    	super(); 
    	} 
    public ChargingStation stationToGive = null;
    public boolean stationWasReleased = false;
    public boolean queueWasLeft = false;

    @Override 
    public Point getIdleLocation() { 
    	return new Point(0,0); 
    }
    
    @Override 
    public synchronized ChargingStation requestCharging(Robot robot) {
    	return stationToGive; 
    }
    
    @Override 
    public synchronized void releaseStation(ChargingStation station) { 
    	stationWasReleased = true;
    	}
    
    @Override 
    public synchronized void leaveQueue(Robot robot) {
    	queueWasLeft = true; 
    	}
}

class MockPathFinder extends PathFinder {
	
    public MockPathFinder() { 
    	super(null); 
    	} 
    public Queue<Point> pathToGive = new LinkedList<>();

    @Override 
    public Queue<Point> findPath(Point start, Point end) { 
    	if (pathToGive == null) return null;
        return new LinkedList<>(pathToGive);
    	}
}

class MockTask extends Tasks {
    private Point destination;
    public MockTask(Point dest) {
        super();
        this.destination = dest;
    }
    
    @Override public Point getDestination() { 
    	return this.destination;
    	
    	}
}



//-----------------------

public class RobotTest {
	
	private Robot robot;
    private MockTaskManager mockTM;
    private MockWarehouse mockWH;
    private MockPathFinder mockPF;
    
    @BeforeEach
    void setUp() throws IOException {
        mockTM = new MockTaskManager();
        mockWH = new MockWarehouse();
        mockPF = new MockPathFinder();
        
        
        robot = new Robot(mockWH, new Point(0, 0), mockTM, mockPF);
        robot.setBatteryForTest(100.0); 
    }
    
    @Test
    void testUpdateState_WhenIdleAndBatteryLow_ChangesStateToMovingToCharge() {
        robot.setStateForTest(RobotState.IDLE);
        robot.setBatteryForTest(10.0); 
        mockWH.stationToGive = new ChargingStation(new Point(9,9));
        mockPF.pathToGive.add(new Point(1,1));
        
        robot.updateState();
        
        assertEquals(RobotState.MOVING_TO_CHARGE, robot.getState());
    }
    
    @Test
    void testUpdateState_WhenSeekingChargeAndStationBusy_ChangesStateToWaitingInQueue() {
        robot.setBatteryForTest(10.0);
        mockWH.stationToGive = null; 
        
        robot.updateState();
        
        assertEquals(RobotState.WAITING_FOR_CHARGE, robot.getState());
    }
    
    @Test
    void testUpdateState_WhenSeekingChargeAndStationFound_CalculatesPathToStation() {
        robot.setBatteryForTest(10.0);
        mockWH.stationToGive = new ChargingStation(new Point(9,9));
        mockPF.pathToGive.add(new Point(1,1)); 
        
        robot.updateState();
        
        assertFalse(robot.getCurrentPath().isEmpty());
        assertEquals(new Point(1,1), robot.getCurrentPath().peek());
    }
    
    
    @Test
    void testUpdateState_WhenWaitingInQueueAndTimeout_ChangesStateToIdle() {
        robot.setStateForTest(RobotState.WAITING_FOR_CHARGE);
        long twoSecondsAgo = System.currentTimeMillis() - 40000;
        robot.setWaitingStartTimeForTest(twoSecondsAgo); 
        
        robot.updateState();
        
        assertEquals(RobotState.IDLE, robot.getState());
    }
    
    @Test
    void testUpdateState_WhenWaitingInQueueAndTimeout_CallsLeaveQueue() {
        robot.setStateForTest(RobotState.WAITING_FOR_CHARGE);
        long twoSecondsAgo = System.currentTimeMillis() - 40000;
        robot.setWaitingStartTimeForTest(twoSecondsAgo);

        robot.updateState();
        
        assertTrue(mockWH.queueWasLeft);
    }
    
    @Test
    void testUpdateState_WhenWorkingAndAtPickup_ChangesWorkingStateToDropoff() {
        robot.setStateForTest(RobotState.WORKING);
        robot.setWorkingStateForTest(WorkingState.GOING_TO_PICKUP);
        robot.setCurrentPathForTest(new LinkedList<>());
        
        robot.updateState();
        
        assertEquals(WorkingState.GOING_TO_DROPOFF, robot.getWorkingState());
    }
    
    @Test
    void testUpdateState_WhenWorkingAndBatteryLow_ReturnsTaskToQueue() {
        robot.setStateForTest(RobotState.WORKING);
        robot.setCurrentTaskForTest(new MockTask(null));
        robot.setBatteryForTest(10.0);

        robot.updateState();
        
        assertTrue(mockTM.taskWasRequeued);
        assertNull(robot.getCurrentTask());
    }

    
    @Test
    void testUpdateState_WhenMovingToChargeAndPathIsEmpty_ChangesStateToCharging() {
        robot.setStateForTest(RobotState.MOVING_TO_CHARGE);
        robot.setCurrentPathForTest(new LinkedList<>());
        
        robot.updateState();
        
        assertEquals(RobotState.CHARGING, robot.getState());
    }
    
    @Test
    void testUpdateState_WhenWorkingAndBatteryLow_ChangesStateToMovingToCharge() {
        robot.setStateForTest(RobotState.WORKING);
        robot.setCurrentTaskForTest(new Tasks());
        robot.setBatteryForTest(10.0);
        mockWH.stationToGive = new ChargingStation(new Point(9,9));
        mockPF.pathToGive.add(new Point(1,1));

        robot.updateState();
        
        assertEquals(RobotState.MOVING_TO_CHARGE, robot.getState());
    }
    
    @Test
    void testUpdateState_WhenWorkingAndAtPickup_CalculatesPathToDropoff() {
        robot.setStateForTest(RobotState.WORKING);
        robot.setWorkingStateForTest(WorkingState.GOING_TO_PICKUP);
        robot.setCurrentPathForTest(new LinkedList<>());
        mockPF.pathToGive.add(new Point(1,1)); 
        
        robot.updateState();
        
        assertFalse(robot.getCurrentPath().isEmpty());
        assertEquals(new Point(1,1), robot.getCurrentPath().peek());
    }
    
    @Test
    void testUpdateState_WhenWorkingAndAtDropoff_ChangesStateToMovingToIdle() {
        robot.setStateForTest(RobotState.WORKING);
        robot.setWorkingStateForTest(WorkingState.GOING_TO_DROPOFF);
        robot.setCurrentPathForTest(new LinkedList<>());
        
        robot.updateState();
        
        assertEquals(RobotState.MOVING_TO_IDLE_POINT, robot.getState());
    }
    
    @Test
    void testUpdateState_WhenWorkingAndAtDropoff_CompletesTaskInManager() {
        robot.setStateForTest(RobotState.WORKING);
        robot.setWorkingStateForTest(WorkingState.GOING_TO_DROPOFF);
        robot.setCurrentPathForTest(new LinkedList<>());
        robot.setCurrentTaskForTest(new MockTask(null));
        
        robot.updateState();
        
        assertTrue(mockTM.taskWasCompleted);
    }
    
    @Test
    void testRobotCreation_ShouldBeIdleWithFullBattery() {
        assertEquals(Robot.RobotState.IDLE, robot.getState());
        assertEquals(100.0, robot.getBattery());
        assertNull(robot.getCurrentTask());
    }
    
    @Test
    void testUpdateState_WhenMovingToIdleAndPathIsEmpty_ChangesStateToIdle() {
        robot.setStateForTest(RobotState.MOVING_TO_IDLE_POINT);
        robot.setCurrentPathForTest(new LinkedList<>());
        
        robot.updateState();
        
        assertEquals(RobotState.IDLE, robot.getState());
    }
    
    @Test
    void testPerformAction_WhenCharging_CallsChargeBattery() {
        robot.setStateForTest(RobotState.CHARGING);
        robot.setBatteryForTest(50.0);
        robot.performAction();
        assertEquals(54.0, robot.getBattery()); 
    }
    
    @Test
    void testPerformAction_WhenWorking_MovesOneStep() {
        robot.setStateForTest(RobotState.WORKING);
        robot.setCurrentPathForTest(new LinkedList<>(List.of(new Point(1,1))));
        robot.performAction();
        assertEquals(new Point(1,1), robot.getLocation());
    }
    
    @Test
    void testPerformAction_WhenMovingToCharge_MovesOneStep() {
        robot.setStateForTest(RobotState.MOVING_TO_CHARGE);
        robot.setCurrentPathForTest(new LinkedList<>(List.of(new Point(2,2))));
        robot.performAction();
        assertEquals(new Point(2,2), robot.getLocation());
    }
    
    @Test
    void testPerformAction_WhenMovingToIdle_MovesOneStep() {
        robot.setStateForTest(RobotState.MOVING_TO_IDLE_POINT);
        robot.setCurrentPathForTest(new LinkedList<>(List.of(new Point(3,3))));
        robot.performAction();
        assertEquals(new Point(3,3), robot.getLocation());
    }
    
    @Test

    void testWorkOnTask_WhenPathExists_MovesOneStep() {
        Queue<Point> path = new LinkedList<>(List.of(new Point(1,1), new Point(1,2)));
        robot.setCurrentPathForTest(path);
        robot.setStateForTest(RobotState.WORKING);
        
        robot.performAction(); 
        
        assertEquals(new Point(1,1), robot.getLocation());
        assertEquals(1, path.size());
    }

    @Test
    void testWorkOnTask_WhenPathExists_DecreasesBattery() {
        robot.setCurrentPathForTest(new LinkedList<>(List.of(new Point(1,1))));
        robot.setBatteryForTest(100.0);
        robot.setStateForTest(RobotState.WORKING);
        
        robot.performAction();
        
        assertEquals(97.0, robot.getBattery()); 
    }
    
    @Test
    void testChargeBattery_IncreasesBattery() {
        robot.setBatteryForTest(50.0);
        robot.setStateForTest(RobotState.CHARGING);
        
        robot.performAction();
        
        assertEquals(54.0, robot.getBattery()); 
    }
    
    @Test
    void testIdleRobot_WhenBatteryLow_ShouldMoveToCharge() {
       
        robot.setBatteryForTest(10.0); 
        mockWH.stationToGive = new ChargingStation(new Point(6,1)); 
        mockPF.pathToGive.add(new Point(1,1)); 
       
        robot.updateState(); 
        
        assertEquals(Robot.RobotState.MOVING_TO_CHARGE, robot.getState());
    }
    
    @Test

    void testChargeBattery_DoesNotExceedMaxBattery() {
        robot.setBatteryForTest(98.0);
        robot.setStateForTest(RobotState.CHARGING);

        robot.performAction(); 
        
        assertEquals(100.0, robot.getBattery()); 
    }
   


}
