package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.dsim.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class FlowCapacityTest {

    @Test
    public void init() {
        var link = TestUtils.createSingleLink();
        link.setCapacity(5432);
		var cap = FlowCapacity.createOutflowCapacity(link);

        assertEquals(1.50888, cap.getMax(), 0.00001);
        assertTrue(cap.isAvailable());
    }

    @Test
    public void consume() {
        var link = TestUtils.createSingleLink();
        link.setCapacity(36000);
		var cap = FlowCapacity.createOutflowCapacity(link);

        assertTrue(cap.isAvailable());
        cap.consume(20);
        assertFalse(cap.isAvailable());
    }

    @Test
    public void maxCapacity() {
        var link = TestUtils.createSingleLink();
        link.setCapacity(36000);
		var cap = FlowCapacity.createOutflowCapacity(link);
        assertEquals(10, cap.getMax());

        // drain the capacity
        cap.consume(20);
        assertFalse(cap.isAvailable());

        // now, the update would give 20 pce because of 20 timesteps
        // but max capacity is 10.
        cap.update(20);
        assertTrue(cap.isAvailable());
        cap.consume(9.9);
        assertTrue(cap.isAvailable());
        cap.consume(0.1);
        assertFalse(cap.isAvailable());
    }

    @Test
    public void accumulateCapacity() {

        var link = TestUtils.createSingleLink();
        link.setCapacity(900);
		var cap = FlowCapacity.createOutflowCapacity(link);
        assertEquals(0.25, cap.getMax());

        // accumulated_capacity should be at -0.75 after this.
        cap.consume(1);
        assertFalse(cap.isAvailable());

        // accumulated_capacity should be at -0.5
        cap.update(1);
        assertFalse(cap.isAvailable());

        // accumulated_capacity should be at 0.0
        cap.update(3);
        assertFalse(cap.isAvailable());

        // accumulated capacity should be at 0.5
        cap.update(5);
        assertTrue(cap.isAvailable());
    }
}
