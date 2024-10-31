package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.dsim.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class StorageCapTest {

    @Test
    public void initHighCapacity() {

        var link = TestUtils.createSingleLink();
        link.setNumberOfLanes(1);
        link.setCapacity(360000);

        var storageCapacity = new StorageCap(link, 7.5);

        assertEquals(100, storageCapacity.getMax());
    }

    @Test
    public void initLargeLinkLowCapacity() {

        var link = TestUtils.createSingleLink();
        link.setNumberOfLanes(15);
        link.setCapacity(3600);

        var storageCapacity = new StorageCap(link, 7.5);

        assertEquals(200, storageCapacity.getMax());
    }

    @Test
    public void comsumeAndRelease() {

        var link = TestUtils.createSingleLink();
        link.setNumberOfLanes(1);
        link.setCapacity(360000);
        var storageCapacity = new StorageCap(link, 7.5);

        // consumed capacity not available anymore immediately
        storageCapacity.consume(2);
        assertEquals(2, storageCapacity.getUsed());
        storageCapacity.consume(1);
        assertEquals(3, storageCapacity.getUsed());

        // released capacity becomes available only after apply updates, i.e. in the next time step
        storageCapacity.release(2);
        assertEquals(3, storageCapacity.getUsed());
        assertEquals(2, storageCapacity.getReleased());

        // apply updates applies released storage capacity and resets bookkeeping
        storageCapacity.applyUpdates();
        assertEquals(1, storageCapacity.getUsed());
        assertEquals(0, storageCapacity.getReleased());
    }

    @Test
    public void isAvailable() {

        var link = TestUtils.createSingleLink();
        link.setNumberOfLanes(1);
        link.setCapacity(36000);
        var storageCapacity = new StorageCap(link, 10);

        storageCapacity.consume(10);
        assertFalse(storageCapacity.isAvailable());
        storageCapacity.release(1);
        assertFalse(storageCapacity.isAvailable());
        storageCapacity.applyUpdates();
        assertTrue(storageCapacity.isAvailable());
    }
}