package com.warehouse.simulation.app.ui;

import com.warehouse.simulation.exceptions.ExceptionStore;
import com.warehouse.simulation.exceptions.ExceptionStore.ExceptionRecord;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import com.warehouse.simulation.logging.LogService;
import com.warehouse.simulation.logging.LogManager;


import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class LogsController {

    @FXML
    private ComboBox<String> logFileCombo;

    @FXML
    private TextArea logsArea;

    private final LogService logService = new LogService("logs");
    private Timeline refresher;

    @FXML
    public void initialize() {
        // populate available log files
        List<String> files = logService.listLogFiles();
        logFileCombo.getItems().addAll(files);

        // try to select today's system log by default
        String todayName = String.format("SYSTEM-%s.log", LocalDate.now());
        if (files.contains(todayName)) logFileCombo.getSelectionModel().select(todayName);
        else if (!files.isEmpty()) logFileCombo.getSelectionModel().select(0);

        logFileCombo.setOnAction(evt -> refreshLogs());

        // auto-refresh timeline: periodically refresh logs
        refresher = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshLogs()));
        refresher.setCycleCount(Timeline.INDEFINITE);
        refresher.play();

        // initial load
        refreshLogs();

        // register a live log listener so UI can append new lines without polling
        try {
            LogManager.getInstance().addListener((fileName, line) -> {
                String selected = logFileCombo.getSelectionModel().getSelectedItem();
                if (selected != null && selected.equals(fileName)) {
                    Platform.runLater(() -> logsArea.appendText(line + "\n"));
                }
            });
        } catch (Throwable ignore) {}
    }

    @FXML
    public void onRefresh() {
        refreshLogs();
    }


    private void refreshLogs() {
        String sel = logFileCombo.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        List<String> lines = logService.readLastLines(sel, 1000);
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l).append('\n');
        }
        Platform.runLater(() -> {
            logsArea.setText(sb.toString());
            logsArea.setScrollTop(Double.MAX_VALUE);
        });
    }

}