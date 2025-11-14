package com.warehouse.simulation.app.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import com.warehouse.simulation.warehouse.Warehouse;
import com.warehouse.simulation.charging.ChargingStation;
import com.warehouse.simulation.robots.Robot;

public class ChargingController {

    @FXML
    private ListView<String> stationsListView;

    @FXML
    private ListView<String> queueListView;

    private Warehouse warehouse;
    private Timeline updater;
    private ObservableList<String> stationsItems = FXCollections.observableArrayList();
    private ObservableList<String> queueItems = FXCollections.observableArrayList();

    public void setWarehouse(Warehouse w) {
        this.warehouse = w;
        start();
    }

    private void start() {
        if (warehouse == null || updater != null) return;
        stationsListView.setItems(stationsItems);
        queueListView.setItems(queueItems);
        updater = new Timeline(new KeyFrame(Duration.millis(500), evt -> refresh()));
        updater.setCycleCount(Timeline.INDEFINITE);
        updater.play();
    }

    private void refresh() {
        Platform.runLater(() -> {
            try {
                stationsItems.clear();
                for (ChargingStation s : warehouse.getStations()) {
                    String occ = s.isAvailable() ? "free" : "occupied";
                    stationsItems.add(s.getID() + " - " + occ + " @ " + s.getLocation());
                }
                queueItems.clear();
                for (Robot r : warehouse.getChargingQueueSnapshot()) {
                    queueItems.add(r.getID() + " | battery=" + String.format("%.1f", r.getBattery()));
                }
            } catch (Throwable t) {
                stationsItems.add("Failed to list stations: " + t.getMessage());
            }
        });
    }
}