package com.warehouse.simulation.app.model;

import com.warehouse.simulation.storage.Order;
import com.warehouse.simulation.app.persistence.PersistenceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.List;

public class OrdersStore {
    private static final OrdersStore INSTANCE = new OrdersStore();

    private final ObservableList<Order> orders = FXCollections.observableArrayList();

    private OrdersStore() {
        // load persisted orders if available
        try {
            List<Order> loaded = PersistenceService.loadOrders();
            if (loaded != null) orders.addAll(loaded);
        } catch (Exception e) {
            // ignore and start fresh
        }
    }

    public static OrdersStore getInstance() { return INSTANCE; }

    public ObservableList<Order> getOrders() { return orders; }

    public synchronized void addOrder(Order o) {
        orders.add(o);
        persist();
    }

    public synchronized void persist() {
        try {
            PersistenceService.saveOrders(orders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}