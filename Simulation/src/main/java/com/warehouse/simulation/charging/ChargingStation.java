package com.warehouse.simulation.charging;

import com.warehouse.simulation.logging.LogManager;
import java.io.IOException;

import com.warehouse.simulation.utils.IGridEntity;
import com.warehouse.simulation.robots.Robot;
import java.awt.Point;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;



public class ChargingStation implements IGridEntity {
    
    private static int num = 0;
    private final String id;
    private final Point location;
    private boolean isAvailable = true;
    private Robot occupant = null;
    private LogManager logManager;
    private final DateTimeFormatter df = DateTimeFormatter.ISO_DATE;


    public ChargingStation(Point location) {
        
        this.location = location;
        this.id = "ch_st_" + num;
        num++;
        
        try {
            this.logManager = LogManager.getInstance("logs");
        } catch (Exception e) {
            
            this.logManager = null;
            System.err.println("Warning: could not initialize LogManager: " + e.getMessage());
        }
        
        if (logManager != null) {
            String date = df.format(LocalDate.now());
            String fileName = String.format("ChargingStationLogs/%s-%s.log", this.getID(), date);
            String msg = String.format("[%s] Charging station %s is set up in the next coordinates (%d, %d)", LocalDateTime.now(), this.getID(), this.location.x, this.location.y);
            logManager.writeLog(fileName, msg);
        }
        

    }
    
    
    public synchronized boolean occupy(Robot robot) {
        if (this.isAvailable) {
            this.isAvailable = false;
            this.occupant = robot;
            
            
             if (logManager != null) {
                    String date = df.format(LocalDate.now());
                    String fileName = String.format("ChargingStationLogs/%s-%s.log", this.getID(), date);
                    String msg = String.format("[%s] Charging station %s is occupied", LocalDateTime.now(), this.getID());
                    logManager.writeLog(fileName, msg);
                }
             
             return true;

        }
        return false;
    }
    
    public synchronized void release() {
        this.isAvailable = true;
        this.occupant = null;
        
        if (logManager != null) {
            String date = df.format(LocalDate.now());
            String fileName = String.format("ChargingStationLogs/%s-%s.log", this.getID(), date);
            String msg = String.format("[%s] Charging station %s is available again", LocalDateTime.now(), this.getID());
            logManager.writeLog(fileName, msg);
        }
        
        
    }
    
    
    public boolean isAvailable() {
        return this.isAvailable;
    }

    public Robot getOccupant() { return this.occupant; }
    
    
    
    // -------- IGridEntity methods -------- 
    @Override
    public Point getLocation() {
        return this.location; 
    }

    @Override
    public String getID() {
        return this.id;
    }
    
    
}