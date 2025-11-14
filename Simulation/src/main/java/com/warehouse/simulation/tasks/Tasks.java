package com.warehouse.simulation.tasks;

import java.util.List;
// import utils.Position;
import com.warehouse.simulation.storage.Item;
import java.awt.Point;
import com.warehouse.simulation.exceptions.ExceptionHandler;

public class Tasks{

    private static int num = 0;
    private String id;
    private Point destination;
    private String robotId;
    private String orderId;
    private Item item;

    public Tasks() {
        this.id = "Task №" + num;
        num++;
    }

    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
    private TaskStatus status;
    private int attempts = 0;

    public Tasks(String id, Point destination, Item item){
        try {
            this.id = id;
            this.destination = destination;
            this.robotId = null;
            this.item = item;
            this.orderId = null;
            this.status = TaskStatus.PENDING;
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "tasks.Tasks.<init>");
            this.id = id == null ? "<unknown>" : id;
            this.destination = destination;
            this.robotId = null;
            this.item = item;
            this.status = TaskStatus.FAILED;
        }
    }

    public Tasks(String id, Item item){
        try {
            this.id = id;
            this.destination = null;
            this.robotId = null;
            this.item = item;
            this.orderId = null;
            this.status = TaskStatus.PENDING;
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "tasks.Tasks.<init>");
            this.id = id == null ? "<unknown>" : id;
            this.destination = null;
            this.robotId = null;
            this.item = item;
            this.status = TaskStatus.FAILED;
        }
    }

    public String getId(){
        return id;
    }

    public Point getDestination(){
        return destination;
    }

    public String getRobotId(){
        return robotId;
    }

    public Item getItems(){
        return item;
    }

    public String getOrderId() { return orderId; }

    public void setOrderId(String orderId) { this.orderId = orderId; }

    public TaskStatus getStatus(){
        return status;
    }

    public void setStatus(TaskStatus status){
        try {
            this.status = status;
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "tasks.Tasks.setStatus");
        }
    }
    public void setRobotId(String robotId){
        try {
            this.robotId = robotId;
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "tasks.Tasks.setRobotId");
        }
    }

    public boolean isComplete(){
        return status == TaskStatus.COMPLETED;
    }

    public int getAttempts() { return attempts; }
    public void incrementAttempts() { this.attempts++; }

    @Override
    public String toString(){
        return "Task ID: " + id + ", Destination: " + destination + ", Robot ID: " + robotId + ", Item: " + item + ", Status: " + status;
    }
}