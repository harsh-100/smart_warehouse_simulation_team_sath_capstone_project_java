package com.warehouse.simulation.app.model;

import com.warehouse.simulation.app.persistence.PersistenceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.warehouse.simulation.storage.StorageUnit;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageUnitsStore {
    private static final StorageUnitsStore INSTANCE = new StorageUnitsStore();

    private final ObservableList<StorageUnit> units = FXCollections.observableArrayList();

    private StorageUnitsStore() {
        try {
            List<StorageUnit> loaded = PersistenceService.loadStorageUnits();
            if (loaded != null && !loaded.isEmpty()) {
                units.addAll(loaded);
                return;
            }
        } catch (Exception e) {
            // ignore and create defaults
        }

        // create 5 default storage units at fixed coordinates with capacity 10
        units.addAll(defaultUnits());
        persist();
    }

    private List<StorageUnit> defaultUnits() {
        List<StorageUnit> d = new ArrayList<>();
        d.add(new StorageUnit("SU-1", 10.0, new Point(1,1)));
        d.add(new StorageUnit("SU-2", 10.0, new Point(1,5)));
        d.add(new StorageUnit("SU-3", 10.0, new Point(5,1)));
        d.add(new StorageUnit("SU-4", 10.0, new Point(5,5)));
        d.add(new StorageUnit("SU-5", 10.0, new Point(3,3)));
        return d;
    }

    public static StorageUnitsStore getInstance() { return INSTANCE; }

    public ObservableList<StorageUnit> getUnits() { return units; }

    public synchronized void persist() {
        try {
            PersistenceService.saveStorageUnits(new ArrayList<>(units));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}