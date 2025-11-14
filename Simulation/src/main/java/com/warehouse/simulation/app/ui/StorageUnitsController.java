package com.warehouse.simulation.app.ui;

import com.warehouse.simulation.app.model.StorageUnitsStore;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import com.warehouse.simulation.storage.StorageUnit;

public class StorageUnitsController {

    @FXML
    private ListView<String> unitsListView;

    @FXML
    private Label unitDetailLabel;

    private final StorageUnitsStore store = StorageUnitsStore.getInstance();

    @FXML
    public void initialize() {
        refreshList();

        store.getUnits().addListener((javafx.collections.ListChangeListener.Change<? extends StorageUnit> c) -> {
            Platform.runLater(this::refreshList);
        });

        unitsListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            int idx = newV.intValue();
            if (idx >= 0 && idx < store.getUnits().size()) {
                StorageUnit su = store.getUnits().get(idx);
                // build items available string
                String itemsStr;
                try {
                    if (su.getItems() == null || su.getItems().isEmpty()) {
                        itemsStr = "Null";
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < su.getItems().size(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(su.getItems().get(i).toString());
                        }
                        itemsStr = sb.toString();
                    }
                } catch (Throwable t) {
                    itemsStr = "<error>";
                }

                unitDetailLabel.setText(String.format("ID: %s\nCapacity: %.0f\nItems: %d\nPosition: (%d,%d)\nItems available: %s",
                        su.getId(), su.getCapacity(), su.getCurrentItemCount(), su.getPosition().x, su.getPosition().y,
                        itemsStr));
            } else {
                unitDetailLabel.setText("Select a storage unit to see details");
            }
        });
    }

    private void refreshList() {
        ObservableList<String> items = unitsListView.getItems();
        items.clear();
        for (StorageUnit su : store.getUnits()) {
            items.add(String.format("%s | items=%d | pos=(%d,%d)", su.getId(), su.getCurrentItemCount(), su.getPosition().x, su.getPosition().y));
        }
    }
}