package com.warehouse.simulation.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {

    @Test
    public void testConstructorAndGetters() {
        System.out.println("[TEST] testConstructorAndGetters starting");
        Item it = new Item("I1", "Box", 2.5);
        assertEquals("I1", it.getId());
        assertEquals("Box", it.getName());
        assertEquals(2.5, it.getWeight(), 1e-9);
    }

    @Test
    public void testToString() {
        System.out.println("[TEST] testToString starting");
        Item it = new Item("I2", "Crate", 5.0);
        String s = it.toString();
        assertTrue(s.contains("I2"));
        assertTrue(s.contains("Crate"));
        assertTrue(s.contains("5.0"));
    }

    @Test
    public void testStorageUnitIdSetAndGet() {
        System.out.println("[TEST] testStorageUnitIdSetAndGet starting");
        Item it = new Item("I3", "Pallet", 10.0);
        assertNull(it.getStorageUnitId());
        it.setStorageUnitId("SU_1");
        assertEquals("SU_1", it.getStorageUnitId());
    }

    @Test
    public void testZeroAndNegativeWeight() {
        System.out.println("[TEST] testZeroAndNegativeWeight starting");
        Item zero = new Item("I4", "Feather", 0.0);
        assertEquals(0.0, zero.getWeight(), 1e-9);

        Item neg = new Item("I5", "Unknown", -1.25);
        // class does not validate weight, so negative values are preserved
        assertEquals(-1.25, neg.getWeight(), 1e-9);
    }

    @Test
    public void testNullNameToString() {
        System.out.println("[TEST] testNullNameToString starting");
        Item it = new Item("I6", null, 1.0);
        String s = it.toString();
        // toString should not throw and should include id
        assertTrue(s.contains("I6"));
    }
}
