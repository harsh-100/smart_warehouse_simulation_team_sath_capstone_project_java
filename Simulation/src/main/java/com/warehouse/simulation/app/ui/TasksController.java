package com.warehouse.simulation.app.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.tasks.Tasks;
import java.util.List;

public class TasksController {

    @FXML
    private ListView<String> tasksListView;

    private TaskManager taskManager;
    private Timeline updater;
    private ObservableList<String> items = FXCollections.observableArrayList();

    public void setTaskManager(TaskManager tm) {
        this.taskManager = tm;
        startPolling();
    }

    private void startPolling() {
        if (taskManager == null || updater != null) return;
        tasksListView.setItems(items);
        updater = new Timeline(new KeyFrame(Duration.millis(500), evt -> refresh()));
        updater.setCycleCount(Timeline.INDEFINITE);
        updater.play();
    }

    private void refresh() {
        Platform.runLater(() -> {
            items.clear();
            try {
                // pending
                List<Tasks> pending = taskManager.getPendingTasks();
                for (Tasks t : pending) items.add(t.getId() + " | PENDING | item=" + t.getItems());

                // active
                for (Tasks t : taskManager.getActiveTasks().values()) items.add(t.getId() + " | PICKED | robot=" + t.getRobotId() + " | item=" + t.getItems());

                // completed
                for (Tasks t : taskManager.getCompletedTasksList()) items.add(t.getId() + " | COMPLETED | item=" + t.getItems());
            } catch (Throwable t) {
                items.add("Failed to read tasks: " + t.getMessage());
            }
        });
    }
}