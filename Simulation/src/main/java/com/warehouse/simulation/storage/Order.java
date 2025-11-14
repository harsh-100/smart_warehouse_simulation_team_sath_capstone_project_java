package com.warehouse.simulation.storage;

import com.warehouse.simulation.exceptions.ExceptionHandler;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.logging.LogManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//This class will represents the Order by the customer
public class Order implements Serializable{
    private static final long serialVersionUID = 1L;
    private String id;
    public enum Status {
        PENDING,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }
    private Status status;
    private List<Item> items;
    private long timestamp;
    private transient LogManager logManager;
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_DATE;

    public Order(String id) {
        this.id = id;
        this.status = Status.PENDING;
        this.items = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        try {
            this.logManager = LogManager.getInstance("logs");
            // log creation (write into a per-day orders file)
            if (logManager != null) {
                String date = DF.format(LocalDate.now());
                String fileName = String.format("OrderLogs/Orders-%s.log", date);
                String msg = String.format("[%s] Order %s created (status=%s)", LocalDateTime.now(), this.id, this.status);
                logManager.writeLog(fileName, msg);
            }
        } catch (Exception e) {
            this.logManager = null;
        }
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public List<Item> getItems() {
        return items;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void addItem(Item item) {
        try {
            items.add(item);
            try { if (this.logManager == null) this.logManager = LogManager.getInstance("logs"); } catch (Throwable ignore) {}
            if (logManager != null) {
                String date = DF.format(LocalDate.now());
                String fileName = String.format("OrderLogs/Orders-%s.log", date);
                String msg = String.format("[%s] Item %s added to Order %s", LocalDateTime.now(), item.toString(), this.id);
                logManager.writeLog(fileName, msg);
            }
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "storage.Order.addItem");
        }
    }

    public void removeItem(Item item) {
        try {
            items.remove(item);
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "storage.Order.removeItem");
        }
    }

    public void setStatus(Status status) {
        try {
            Status prev = this.status;
            this.status = status;
            try { if (this.logManager == null) this.logManager = LogManager.getInstance("logs"); } catch (Throwable ignore) {}
            if (logManager != null) {
                String date = DF.format(LocalDate.now());
                String fileName = String.format("OrderLogs/Orders-%s.log", date);
                String msg = String.format("[%s] Order %s status changed: %s -> %s", LocalDateTime.now(), this.id, prev, status);
                logManager.writeLog(fileName, msg);
            }
        } catch (Throwable t) {
            ExceptionHandler.handle(t, "storage.Order.setStatus");
        }
    }
    @Override
    public String toString() {
        return "Order ID: " + id + ", Status: " + status + ", Items: " + items + ", Timestamp: " + timestamp;
    }
}