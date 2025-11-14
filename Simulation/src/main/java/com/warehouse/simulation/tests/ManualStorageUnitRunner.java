package com.warehouse.simulation.tests;

import com.warehouse.simulation.storage.StorageUnit;
import com.warehouse.simulation.storage.Item;
import java.awt.Point;

/**
 * Small manual runner to validate StorageUnit behavior when the full test-suite
 * cannot be executed (some tests in the repository are incompatible with the
 * current codebase). This is NOT a replacement for the JUnit tests added in
 * src/test — it's a lightweight verifier you can run quickly.
 */
public class ManualStorageUnitRunner {

    public static void main(String[] args) {
        try {
            StorageUnit unit = new StorageUnit("SU1", 2.0, new Point(0,0));

            Item i1 = new Item("I1","one", 1.0);
            Item i2 = new Item("I2","two", 1.0);
            Item i3 = new Item("I3","three", 1.0);

            if (!unit.addItems(i1)) throw new RuntimeException("Failed to add first item");
            if (!unit.addItems(i2)) throw new RuntimeException("Failed to add second item");
            if (unit.addItems(i3)) throw new RuntimeException("Should not accept third item beyond capacity");

            if (unit.getCurrentItemCount() != 2) throw new RuntimeException("Unexpected item count");
            if (Math.abs(unit.getRemainingCapacity() - 0.0d) > 1e-6) throw new RuntimeException("Remaining capacity wrong");

            if (!unit.removeItems("I1")) throw new RuntimeException("Failed to remove existing item");
            if (unit.getCurrentItemCount() != 1) throw new RuntimeException("Count after remove wrong");

            // null handling
            if (unit.addItems(null)) throw new RuntimeException("addItems should return false on null");

            System.out.println("Manual StorageUnit checks passed");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

}
