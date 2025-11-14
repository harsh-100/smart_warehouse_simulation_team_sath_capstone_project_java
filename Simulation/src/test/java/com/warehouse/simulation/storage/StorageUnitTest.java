package com.warehouse.simulation.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.List;

public class StorageUnitTest {

    private StorageUnit unit;

    @BeforeEach
    public void setup() {
        unit = new StorageUnit("SU1", 2.0, new Point(0,0));
    }

    @Test
    public void testAddItems_success() {
        System.out.println("[TEST] testAddItems_success starting");
        Item it = new Item("I1", "Widget", 1.2);
        boolean added = unit.addItems(it);
        assertTrue(added, "Item should be added when below capacity");
        assertEquals(1, unit.getCurrentItemCount());
        List<Item> items = unit.getItems();
        assertNotNull(items);
        assertEquals("I1", items.get(0).getId());
    }

    @Test
    public void testAddItems_reachCapacity() {
        System.out.println("[TEST] testAddItems_reachCapacity starting");
        Item a = new Item("A", "A", 0.1);
        Item b = new Item("B", "B", 0.2);
        Item c = new Item("C", "C", 0.3);

        assertTrue(unit.addItems(a));
        assertTrue(unit.addItems(b));
        // capacity is 2.0 -> a third item should not be accepted
        assertFalse(unit.addItems(c), "Should not add item when capacity reached");
        assertEquals(2, unit.getCurrentItemCount());
    }

    @Test
    public void testRemoveItems_existing() {
        System.out.println("[TEST] testRemoveItems_existing starting");
        Item a = new Item("R1", "Rem", 0.5);
        Item b = new Item("R2", "Rem2", 0.6);
        unit.addItems(a);
        unit.addItems(b);

        boolean removed = unit.removeItems("R1");
        assertTrue(removed, "removeItems should return true for existing id");
        assertEquals(1, unit.getCurrentItemCount());
        assertFalse(unit.getItems().stream().anyMatch(i -> "R1".equals(i.getId())));
    }

    @Test
    public void testRemoveItems_nonExisting() {
        System.out.println("[TEST] testRemoveItems_nonExisting starting");
        Item a = new Item("X1", "X", 0.1);
        unit.addItems(a);
        boolean removed = unit.removeItems("NOPE");
        assertFalse(removed, "removeItems should return false when id not found");
        assertEquals(1, unit.getCurrentItemCount());
    }

    @Test
    public void testGetRemainingCapacity_andToString() {
        System.out.println("[TEST] testGetRemainingCapacity_andToString starting");
        Item a = new Item("T1", "T", 1.0);
        unit.addItems(a);
        double remaining = unit.getRemainingCapacity();
        assertEquals(1.0, remaining, 0.0001, "Remaining capacity should be capacity - itemCount");
        String s = unit.toString();
        assertTrue(s.contains("SU1"), "toString should contain the storage unit id");
        assertTrue(s.contains("Capacity"));
    }

    @Test
    public void testAddItems_nullItem() {
        System.out.println("[TEST] testAddItems_nullItem starting");
        // adding null should be handled and return false (no NPE escapes)
        boolean added = unit.addItems(null);
        assertFalse(added, "Adding null should return false");
        assertEquals(0, unit.getCurrentItemCount());
    }

}
