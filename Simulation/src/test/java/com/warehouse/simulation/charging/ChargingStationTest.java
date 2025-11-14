package com.warehouse.simulation.charging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.awt.Point;
import java.io.IOException;


import com.warehouse.simulation.robots.Robot;
import com.warehouse.simulation.tasks.TaskManager;

import static org.junit.jupiter.api.Assertions.*;

class MockTaskManager extends TaskManager {
    public MockTaskManager() throws IOException { super("mock-tm"); }
}

public class ChargingStationTest {
	
	private ChargingStation station;
    private Robot dummyRobot; 
    private Robot dummyRobot2;
    
    @BeforeEach
    void setUp() throws IOException {
        
        station = new ChargingStation(new Point(5, 5));
        
       
        MockTaskManager mockTM = new MockTaskManager();
 
        dummyRobot = new Robot(mockTM); 
        dummyRobot2 = new Robot(mockTM);
    }
    
    @Test
    void testConstructor_ShouldBeAvailable() {
        assertTrue(station.isAvailable());
    }
    
    @Test
    void testConstructor_ShouldHaveNoOccupant() {
        assertNull(station.getOccupant());
    }

    @Test
    void testConstructor_SetsIdAndLocation() {
        assertNotNull(station.getID());
        assertEquals(new Point(5, 5), station.getLocation());
    }

    @Test
    void testOccupy_WhenAvailable_ShouldSucceed() {
        boolean success = station.occupy(dummyRobot);

        assertTrue(success);
        assertFalse(station.isAvailable());
        assertEquals(dummyRobot, station.getOccupant());
    }
    
    @Test
    void testOccupy_WhenAlreadyOccupied_ShouldFail() {
        station.occupy(dummyRobot); 
        
        boolean secondSuccess = station.occupy(dummyRobot2); 

        assertFalse(secondSuccess);
        assertFalse(station.isAvailable());
        assertEquals(dummyRobot, station.getOccupant());
    }
    
    @Test
    void testRelease_ShouldMakeStationAvailableAndEmpty() {
        station.occupy(dummyRobot); 
        assertFalse(station.isAvailable()); 

        station.release();

        assertTrue(station.isAvailable());
        assertNull(station.getOccupant());
    }
    
    @Test
    void testGetID_ShouldBeUnique() {
        ChargingStation station2 = new ChargingStation(new Point(1, 1));
        assertNotEquals(station.getID(), station2.getID());
    }
    

}
