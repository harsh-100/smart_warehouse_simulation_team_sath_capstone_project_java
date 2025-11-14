package com.warehouse.simulation.app.ui;

import com.warehouse.simulation.app.model.OrdersStore;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import com.warehouse.simulation.storage.Order;

public class OrdersController {

    @FXML
    private ListView<String> ordersListView;

    @FXML
    private Label orderDetailLabel;

    private final OrdersStore store = OrdersStore.getInstance();

    @FXML
    public void initialize() {
        ObservableList<Order> orders = store.getOrders();
        // map to simple strings
        ordersListView.getItems().clear();
        for (Order o : orders) ordersListView.getItems().add(formatSummary(o));

        // listen for changes
        orders.addListener((javafx.collections.ListChangeListener.Change<? extends Order> c) -> {
            Platform.runLater(() -> {
                ordersListView.getItems().clear();
                for (Order o : orders) ordersListView.getItems().add(formatSummary(o));
            });
        });

        ordersListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            int idx = newV.intValue();
            if (idx >= 0 && idx < orders.size()) {
                Order o = orders.get(idx);
                orderDetailLabel.setText("Order: " + o.getId() + "\nStatus: " + o.getStatus() + "\nTotal items: " + o.getItems().size());
            } else {
                orderDetailLabel.setText("Select an order to see details");
            }
        });
    }

    private String formatSummary(Order o) {
        return String.format("%s | %s | items=%d", o.getId(), o.getStatus(), o.getItems().size());
    }
}