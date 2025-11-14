package com.warehouse.simulation.app.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import com.warehouse.simulation.robots.Robot;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.warehouse.Warehouse;
import com.warehouse.simulation.storage.StorageUnit;
import com.warehouse.simulation.charging.ChargingStation;

import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import com.warehouse.simulation.app.model.OrdersStore;
import com.warehouse.simulation.app.model.StorageUnitsStore;

public class DashboardController {

    @FXML
    private VBox contentArea;

    @FXML
    private Label simStatusLabel;

    @FXML
    private Label scenarioStatusLabel;

    @FXML
    private Label pendingTasksLabel;

    @FXML
    private Label completedTasksLabel;

    @FXML
    private ListView<String> robotListView;

    @FXML
    private Label robotsHeadingLabel;

    @FXML
    private Button scenarioAButton;

    @FXML
    private Button scenarioBButton;

    @FXML
    private Button scenarioCButton;

    private Warehouse warehouse;
    private TaskManager taskManager;
    private StorageUnit storageUnit;
    private Timeline updater;
    private boolean taskManagerListenerRegistered = false;
    private boolean scenarioListenerRegistered = false;

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        startPollingIfReady();
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
        startPollingIfReady();
        if (!taskManagerListenerRegistered) {
            registerTaskManagerListeners();
            taskManagerListenerRegistered = true;
        }
        // register scenario runner listener once we have taskManager (and thus the app wiring is done)
        if (!scenarioListenerRegistered) {
            try {
                // use reflection / dynamic proxy to avoid compile-time dependency on ScenarioRunner type
                Class<?> srClass = Class.forName("app.demo.ScenarioRunner");
                Class<?> listenerIface = Class.forName("app.demo.ScenarioRunner$ScenarioListener");
                java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                    String m = method.getName();
                    String scenarioName = (args != null && args.length > 0 && args[0] != null) ? args[0].toString() : "";
                    if ("onScenarioStarted".equals(m)) {
                        Platform.runLater(() -> {
                            scenarioStatusLabel.setText("Scenario: " + scenarioName + " (running)");
                            scenarioAButton.setDisable(true);
                            scenarioBButton.setDisable(true);
                            scenarioCButton.setDisable(true);
                        });
                    } else if ("onScenarioFinished".equals(m)) {
                        Platform.runLater(() -> {
                            scenarioStatusLabel.setText("Scenario: idle");
                            scenarioAButton.setDisable(false);
                            scenarioBButton.setDisable(false);
                            scenarioCButton.setDisable(false);
                        });
                    }
                    return null;
                };
                Object proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{listenerIface}, handler);
                java.lang.reflect.Method addListener = srClass.getMethod("addListener", listenerIface);
                addListener.invoke(null, proxy);
                scenarioListenerRegistered = true;
            } catch (ClassNotFoundException cnfe) {
                // ScenarioRunner not available - ignore
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void registerTaskManagerListeners() {
        if (this.taskManager == null) return;
        this.taskManager.addListener(new TaskManager.TaskListener() {
            @Override
            public void onPendingCountChanged(int newPending) {
                Platform.runLater(() -> pendingTasksLabel.setText("Pending tasks: " + newPending));
            }

            @Override
            public void onCompletedCountChanged(int newCompleted) {
                Platform.runLater(() -> completedTasksLabel.setText("Completed tasks: " + newCompleted));
            }
        });
    }

    public void setStorageUnit(StorageUnit storageUnit) {
        this.storageUnit = storageUnit;
    }

    private void startPollingIfReady() {
        if (this.warehouse == null || this.taskManager == null) return;
        if (this.updater != null) return;

        ObservableList<String> items = FXCollections.observableArrayList();
        robotListView.setItems(items);

        updater = new Timeline(new KeyFrame(Duration.millis(500), evt -> {
            try {
                updateUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        updater.setCycleCount(Timeline.INDEFINITE);
        updater.play();
    }

    private void updateUI() {
        // Run on JavaFX thread
        Platform.runLater(() -> {
            // simulation status
            boolean running = warehouse.isSimulationRunning();
            simStatusLabel.setText("Simulation: " + (running ? "running" : "stopped"));

            // tasks
            int pending = taskManager.getPendingTasks().size();
            int completed = taskManager.getCompletedTasksList().size();
            pendingTasksLabel.setText("Pending tasks: " + pending);
            completedTasksLabel.setText("Completed tasks: " + completed);

            // robots - heading shows base and drop-off points, rows show per-robot state
            try {
                String base = String.format("(%d,%d)", warehouse.getIdleLocation().x, warehouse.getIdleLocation().y);
                String drop = String.format("(%d,%d)", warehouse.getDropOffLocation().x, warehouse.getDropOffLocation().y);
                robotsHeadingLabel.setText(String.format("Robots (base=%s, drop=%s):", base, drop));
            } catch (Exception ignored) {}

            List<Robot> robots = warehouse.getRobots();
            ObservableList<String> items = robotListView.getItems();
            items.clear();
            for (Robot r : robots) {
                String taskId = (r.getCurrentTask() != null) ? r.getCurrentTask().getId() : "-";
                String s = String.format("%s | state=%s | battery=%.1f | pos=(%d,%d) | task=%s",
                        r.getID(), r.getState(), r.getBattery(), r.getLocation().x, r.getLocation().y, taskId);
                items.add(s);
            }
        });
    }

    @FXML
    private void showDashboard() {
        // Restore the original dashboard controls (they may have been replaced
        // when loading other views). We re-add the existing labeled nodes and
        // the ListView that the controller owns so the dashboard UI becomes
        // visible again.
        try {
            // Load full panels and embed them so Dashboard mirrors individual screens
            // Left: AGV panel (robots + charging stations + queue)
            // Right: Tasks (top) and Orders (bottom)
            FXMLLoader agvLoader = new FXMLLoader(getClass().getResource("/fxml/AGVPanel.fxml"));
            Node agvNode = agvLoader.load();
            Object agvController = agvLoader.getController();
            if (agvController instanceof AGVPanelController) {
                ((AGVPanelController) agvController).setWarehouse(this.warehouse);
                ((AGVPanelController) agvController).setTaskManager(this.taskManager);
            }

            FXMLLoader tasksLoader = new FXMLLoader(getClass().getResource("/fxml/Tasks.fxml"));
            Node tasksNode = tasksLoader.load();
            Object tasksController = tasksLoader.getController();
            if (tasksController instanceof TasksController) {
                ((TasksController) tasksController).setTaskManager(this.taskManager);
            }

            FXMLLoader ordersLoader = new FXMLLoader(getClass().getResource("/fxml/Orders.fxml"));
            Node ordersNode = ordersLoader.load();
            // OrdersController reads OrdersStore on initialize, no wiring needed

            // Layout: HBox with AGV panel on left and VBox (Tasks, Orders) on right
            javafx.scene.layout.HBox main = new javafx.scene.layout.HBox(10);
            javafx.scene.layout.VBox right = new javafx.scene.layout.VBox(10);
            right.getChildren().addAll(tasksNode, ordersNode);
            // Let children grow
            javafx.scene.layout.HBox.setHgrow(agvNode, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox.setHgrow(right, javafx.scene.layout.Priority.ALWAYS);
            main.getChildren().addAll(agvNode, right);

            contentArea.getChildren().setAll(simStatusLabel, scenarioStatusLabel, pendingTasksLabel, completedTasksLabel, main);
            // no need to call updateUI() because embedded controllers poll/update themselves
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAGVPanel() {
        loadIntoContent("/fxml/AGVPanel.fxml");
    }

    @FXML
    private void openInventory() {
        loadIntoContent("/fxml/Inventory.fxml");
    }

    @FXML
    private void openLogs() {
        loadIntoContent("/fxml/Logs.fxml");
    }

    @FXML
    private void openOrders() {
        loadIntoContent("/fxml/Orders.fxml");
    }

    @FXML
    private void openTasks() {
        loadIntoContent("/fxml/Tasks.fxml");
    }

    @FXML
    private void openStorageUnits() {
        loadIntoContent("/fxml/StorageUnits.fxml");
    }

    @FXML
    private void openChargingStations() {
        // kept for backward compatibility with Dashboard.fxml buttons
        // Charging view is largely integrated in AGV panel, but keep loader
        // so older FXML references don't break.
        loadIntoContent("/fxml/Charging.fxml");
    }


    @FXML
    private void flushAllData() {
        try {
            // stop running robots first to avoid races while we clear data
            if (warehouse != null) {
                warehouse.stopSimulation();
            }

            // Clear task manager queues and notify listeners
            if (taskManager != null) {
                taskManager.clearAllTasks();
            }

            // Reset robots to idle and full battery
            if (warehouse != null) {
                for (Robot r : warehouse.getRobots()) {
                    try { r.resetToIdle(warehouse.getIdleLocation()); } catch (Throwable ignore) {}
                }
                // release charging stations and clear queue
                for (ChargingStation s : warehouse.getStations()) {
                    try { s.release(); } catch (Throwable ignore) {}
                }
                try { warehouse.clearChargingQueue(); } catch (Throwable ignore) {}
            }

            // Clear persisted orders
            OrdersStore ordersStore = OrdersStore.getInstance();
            ordersStore.getOrders().clear();
            ordersStore.persist();

            // Clear items from all storage units but keep the units themselves
            StorageUnitsStore sus = StorageUnitsStore.getInstance();
            for (StorageUnit su : sus.getUnits()) {
                su.getItems().clear();
            }
            sus.persist();

            // Remove legacy inventory file if present
            Path inv = Path.of("data").resolve("inventory.dat");
            Files.deleteIfExists(inv);

            // Update UI to indicate success and refresh dashboard
            Platform.runLater(() -> {
                simStatusLabel.setText("Data flushed — fresh start");
                contentArea.getChildren().clear();
                showDashboard();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void runScenarioA() {
        if (warehouse != null && taskManager != null) {
        	com.warehouse.simulation.app.demo.ScenarioRunner.runScenarioA(warehouse, taskManager);
        }
    }

    @FXML
    private void runScenarioB() {
        if (warehouse != null && taskManager != null) {
        	com.warehouse.simulation.app.demo.ScenarioRunner.runScenarioB(warehouse, taskManager);
        }
    }

    @FXML
    private void runScenarioC() {
        if (warehouse != null && taskManager != null) {
        	com.warehouse.simulation.app.demo.ScenarioRunner.runScenarioC(warehouse, taskManager);
        }
    }

    private void loadIntoContent(String resource) {
        try {
            FXMLLoader f = new FXMLLoader(getClass().getResource(resource));
            Node n = f.load();
            Object controller = f.getController();

            // Provide references to controllers that accept them
            if (controller instanceof AGVPanelController) {
                ((AGVPanelController) controller).setWarehouse(this.warehouse);
                ((AGVPanelController) controller).setTaskManager(this.taskManager);
            }

            if (controller instanceof InventoryController) {
                // Provide TaskManager reference only. InventoryController aggregates
                // items from all storage units itself, so don't force a single
                // storageUnit here which would hide other units' items.
                ((InventoryController) controller).setTaskManager(this.taskManager);
            }

            if (controller instanceof com.warehouse.simulation.app.ui.TasksController) {
                ((com.warehouse.simulation.app.ui.TasksController) controller).setTaskManager(this.taskManager);
            }

            if (controller instanceof LogsController) {
                // future: pass logging service
            }

            contentArea.getChildren().setAll(n);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}