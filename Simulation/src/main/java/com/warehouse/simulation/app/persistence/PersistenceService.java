package com.warehouse.simulation.app.persistence;

import com.warehouse.simulation.storage.StorageUnit;
import com.warehouse.simulation.storage.Order;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PersistenceService {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path INVENTORY_FILE = DATA_DIR.resolve("inventory.dat");
    private static final Path ORDERS_FILE = DATA_DIR.resolve("orders.dat");
    private static final Path STORAGE_UNITS_FILE = DATA_DIR.resolve("storage_units.dat");

    public static void ensureDataDir() throws IOException {
        if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
    }

    public static void saveInventory(StorageUnit su) throws IOException {
        ensureDataDir();
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(INVENTORY_FILE)))) {
            oos.writeObject(su);
        }
    }

    public static StorageUnit loadInventory() throws IOException, ClassNotFoundException {
        if (!Files.exists(INVENTORY_FILE)) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(INVENTORY_FILE)))) {
            Object o = ois.readObject();
            if (o instanceof StorageUnit) return (StorageUnit) o;
            return null;
        }
    }

    // New methods to persist multiple storage units (the app uses fixed set of units)
    public static void saveStorageUnits(java.util.List<StorageUnit> units) throws IOException {
        ensureDataDir();
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(STORAGE_UNITS_FILE)))) {
            oos.writeObject(new java.util.ArrayList<>(units));
        }
    }

    @SuppressWarnings("unchecked")
    public static java.util.List<StorageUnit> loadStorageUnits() throws IOException, ClassNotFoundException {
        if (!Files.exists(STORAGE_UNITS_FILE)) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(STORAGE_UNITS_FILE)))) {
            Object o = ois.readObject();
            if (o instanceof java.util.List) return (java.util.List<StorageUnit>) o;
            return null;
        }
    }

    public static void saveOrders(List<Order> orders) throws IOException {
        ensureDataDir();
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(ORDERS_FILE)))) {
            oos.writeObject(new ArrayList<>(orders));
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Order> loadOrders() throws IOException, ClassNotFoundException {
        if (!Files.exists(ORDERS_FILE)) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(ORDERS_FILE)))) {
            Object o = ois.readObject();
            if (o instanceof List) return (List<Order>) o;
            return new ArrayList<>();
        }
    }
}