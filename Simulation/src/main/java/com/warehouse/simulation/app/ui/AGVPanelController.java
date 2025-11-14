package com.warehouse.simulation.app.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
 
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import com.warehouse.simulation.robots.Robot;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.warehouse.Warehouse;

import java.util.List;

public class AGVPanelController {

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<String> robotListView;

    @FXML
    private Label robotsHeadingLabel;

    @FXML
    private ListView<String> stationsListView;

    @FXML
    private ListView<String> queueListView;

    private Warehouse warehouse;
    private TaskManager taskManager;
    private Timeline updater;

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        startUpdaterIfReady();
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
        startUpdaterIfReady();
    }

    private void startUpdaterIfReady() {
        if (warehouse == null || taskManager == null) return;
        if (updater != null) return;

    ObservableList<String> items = FXCollections.observableArrayList();
    robotListView.setItems(items);

    // setup charging station and queue lists
    stationsListView.setItems(FXCollections.observableArrayList());
    queueListView.setItems(FXCollections.observableArrayList());

    // color occupied stations green
    stationsListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item);
                if (item.contains("occupied")) {
                    setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724;"); // green
                } else {
                    setStyle("");
                }
            }
        }
    });

    updater = new Timeline(new KeyFrame(Duration.millis(500), e -> refreshRobots()));
        updater.setCycleCount(Timeline.INDEFINITE);
        updater.play();
        // cell factory to color rows by robot state
        robotListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // determine state from the text (state=<STATE> in formatted string)
                    if (item.contains("| IDLE |")) {
                        setStyle("");
                    } else if (item.contains("| WORKING |")) {
                        setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724;"); // green
                    } else if (item.contains("| CHARGING |")) {
                        setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404;"); // yellow
                    } else if (item.contains("| WAITING_FOR_CHARGE |")) {
                        setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24;"); // light red
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void refreshRobots() {
        Platform.runLater(() -> {
            try {
                List<Robot> robots = warehouse.getRobots();
                ObservableList<String> items = robotListView.getItems();
                items.clear();
                // set robots heading once (shows warehouse base and drop-off points)
                try {
                    String base = String.format("(%d,%d)", warehouse.getIdleLocation().x, warehouse.getIdleLocation().y);
                    String drop = String.format("(%d,%d)", warehouse.getDropOffLocation().x, warehouse.getDropOffLocation().y);
                    robotsHeadingLabel.setText(String.format("Robots (base=%s, drop=%s):", base, drop));
                } catch (Exception ignored) {}

                for (Robot r : robots) {
                    String taskId = (r.getCurrentTask() != null) ? r.getCurrentTask().getId() : "-";
                    // per-row: show id, state, battery, position and current task
                    String s = String.format("%s | %s | batt=%.1f | pos=(%d,%d) | task=%s",
                            r.getID(), r.getState(), r.getBattery(), r.getLocation().x, r.getLocation().y, taskId);
                    items.add(s);
                }
                // refresh charging stations (format points as (x,y))
                ObservableList<String> stItems = stationsListView.getItems();
                stItems.clear();
                warehouse.getStations().forEach(s -> {
                    String point = String.format("(%d,%d)", s.getLocation().x, s.getLocation().y);
                    stItems.add(s.getID() + " - " + (s.isAvailable() ? "free" : "occupied") + " @ " + point);
                });
                // refresh charging queue
                ObservableList<String> qItems = queueListView.getItems();
                qItems.clear();
                warehouse.getChargingQueueSnapshot().forEach(rq -> qItems.add(rq.getID() + " | batt=" + String.format("%.1f", rq.getBattery())));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @FXML
    private void startSimulation() {
        if (warehouse != null) {
            warehouse.startSimulation();
            statusLabel.setText("Simulation started");
        } else {
            statusLabel.setText("Warehouse not initialized");
        }
    }

    @FXML
    private void stopSimulation() {
        if (warehouse != null) {
            warehouse.stopSimulation();
            statusLabel.setText("Simulation stopped");
        } else {
            statusLabel.setText("Warehouse not initialized");
        }
    }

    @FXML
    private void addManualTask() {
        // Manual task input removed; kept for future use if needed
        statusLabel.setText("Manual task disabled in this panel");
    }
}