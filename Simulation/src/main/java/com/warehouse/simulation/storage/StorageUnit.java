package com.warehouse.simulation.storage;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.awt.Point;
import com.warehouse.simulation.exceptions.ExceptionHandler;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.logging.LogManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StorageUnit implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private double capacity;
    private Point position;
    private List<Item> items;
    private transient LogManager logManager;
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_DATE;

    public StorageUnit(String id, double capacity , Point position){
        this.id = id;
        this.capacity = capacity;
        this.position = position;
        this.items = new ArrayList<>();
        try {
            this.logManager = LogManager.getInstance("logs");
        } catch (Exception e) {
            this.logManager = null;
        }
    }
    public String getId(){
        return id;
    }
    public double getCapacity(){
        return capacity;
    }
    public Point getPosition(){
        return position;
    }
    public List<Item> getItems(){
        return items;
    }

    public boolean addItems(Item item){
        try {
            // reject null items early to avoid storing nulls and causing unexpected NPEs
            if (item == null) return false;
            if( items.size() < capacity){
                items.add(item);
                // Log inventory addition (ensure logManager initialized after deserialization)
                try {
                    if (this.logManager == null) this.logManager = LogManager.getInstance("logs");
                } catch (Throwable ignore) {}
                if (logManager != null) {
                    String date = DF.format(LocalDate.now());
                    String fileName = String.format("InventoryLogs/%s-%s.log", this.id, date);
                    String msg = String.format("[%s] Item added to StorageUnit %s: %s", LocalDateTime.now(), this.id, item.toString());
                    logManager.writeLog(fileName, msg);
                }
                return true;
            }
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "storage.StorageUnit.addItems");
        }
        return false;
    }

    public boolean removeItems(String itemid){
        try {
            return items.removeIf(i -> i.getId().equals(itemid));
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "storage.StorageUnit.removeItems");
            return false;
        }
    }

    public double getRemainingCapacity(){
        return capacity - items.size();
    }


    public int getCurrentItemCount(){
        return items.size();
    }


    @Override
    public  String toString(){
        return "StorageUnit ID: " + id + ", Capacity: " + capacity + ", Position: " + position + ", Items: " + items;
    }


}