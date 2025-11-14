package com.warehouse.simulation.app;

import com.warehouse.simulation.tasks.TaskManager;
import com.warehouse.simulation.warehouse.Warehouse;
import com.warehouse.simulation.storage.StorageUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX entry point that loads the Dashboard FXML. Controllers are lightweight stubs
 * that can be extended to call backend services.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // create shared TaskManager and Warehouse and pass them to controllers
            TaskManager tm = null;
            try {
                tm = new TaskManager("TM-UI");
            } catch (Exception e) {
                e.printStackTrace();
            }

            Warehouse wh;
            if (tm != null) {
                wh = new Warehouse(tm);
            } else {
                wh = new Warehouse();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Parent root = loader.load();

            // load or create a fixed set of storage units (5 units) and pick the first for initial views
            com.warehouse.simulation.app.model.StorageUnitsStore sus = com.warehouse.simulation.app.model.StorageUnitsStore.getInstance();
            StorageUnit su = sus.getUnits().isEmpty() ? null : sus.getUnits().get(0);

            // inject backend references into controller
            Object controller = loader.getController();
                if (controller instanceof com.warehouse.simulation.app.ui.DashboardController) {
                	com.warehouse.simulation.app.ui.DashboardController dc = (com.warehouse.simulation.app.ui.DashboardController) controller;
                    dc.setTaskManager(tm);
                    dc.setWarehouse(wh);
                    dc.setStorageUnit(su);
                }

            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Smart Warehouse Demo");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}