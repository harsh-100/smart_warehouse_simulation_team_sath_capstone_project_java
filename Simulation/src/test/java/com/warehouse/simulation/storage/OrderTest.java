package com.warehouse.simulation.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

    @Test
    public void testAddItem() {
        System.out.println("[TEST] testAddItem starting");
        Order o = new Order("O1");
        Item it = new Item("I1", "Box", 2.0);
        o.addItem(it);
        assertEquals(1, o.getItems().size());
        assertTrue(o.getItems().contains(it));
    }

    @Test
    public void testRemoveItem_existing() {
        System.out.println("[TEST] testRemoveItem_existing starting");
        Order o = new Order("O2");
        Item a = new Item("A", "One", 1.0);
        Item b = new Item("B", "Two", 2.0);
        o.addItem(a);
        o.addItem(b);
        assertEquals(2, o.getItems().size());
        o.removeItem(a);
        assertEquals(1, o.getItems().size());
        assertFalse(o.getItems().contains(a));
    }

    @Test
    public void testSetStatus() {
        System.out.println("[TEST] testSetStatus starting");
        Order o = new Order("O3");
        assertEquals(Order.Status.PENDING, o.getStatus());
        o.setStatus(Order.Status.SHIPPED);
        assertEquals(Order.Status.SHIPPED, o.getStatus());
        o.setStatus(Order.Status.DELIVERED);
        assertEquals(Order.Status.DELIVERED, o.getStatus());
    }

    @Test
    public void testTimestampAndId() {
        System.out.println("[TEST] testTimestampAndId starting");
        Order o = new Order("O4");
        assertEquals("O4", o.getId());
        assertTrue(o.getTimestamp() > 0);
    }

    @Test
    public void testToStringContainsInfo() {
        System.out.println("[TEST] testToStringContainsInfo starting");
        Order o = new Order("O5");
        Item it = new Item("I9", "Sample", 3.3);
        o.addItem(it);
        String s = o.toString();
        assertTrue(s.contains("O5"));
        assertTrue(s.contains("PENDING") || s.contains("SHIPPED") || s.contains("DELIVERED") || s.contains("CANCELLED"));
        assertTrue(s.contains("Items"));
    }
}
