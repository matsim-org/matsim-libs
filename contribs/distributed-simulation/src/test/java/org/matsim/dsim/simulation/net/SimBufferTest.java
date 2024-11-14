package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.dsim.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SimBufferTest {

    @Test
    void init() {
		Link link = TestUtils.createSingleLink();
		link.setCapacity(23 * 3600);
		var buffer = new SimBuffer(FlowCapacity.createOutflowCapacity(link), 30);
        assertEquals(23, buffer.getMaxFlowCapacity());
        assertEquals(0, buffer.getPceInBuffer());
		assertTrue(buffer.isAvailable(0));
    }

    @Test
    void addVehicle() {
        var stuckThreshold = 11;
		var link = TestUtils.createSingleLink();
		link.setCapacity(7200);
		var buffer = new SimBuffer(FlowCapacity.createOutflowCapacity(link), stuckThreshold);
        var vehicle = TestUtils.createVehicle("vehicle-1", 10, 10);

        // push vehicle which consumes flow capacity and starts the stuck timer of the vehicle.
		assertTrue(buffer.isAvailable(0));
        buffer.add(vehicle, 0);
		assertFalse(buffer.isAvailable(0));
        assertEquals(vehicle.getSizeInEquivalents(), buffer.getPceInBuffer());
        assertFalse(buffer.isStuck(0));
        assertTrue(buffer.isStuck(stuckThreshold));

        // make sure that flow capacity is rebuild slowly
        var now = vehicle.getSizeInEquivalents() / buffer.getMaxFlowCapacity() - 1;
		assertFalse(buffer.isAvailable(now));

        now = vehicle.getSizeInEquivalents() / buffer.getMaxFlowCapacity();
		assertTrue(buffer.isAvailable(now));
    }

    @Test
    void addVehicles() {

		var link = TestUtils.createSingleLink();
		link.setCapacity(15 * 3500);
		var buffer = new SimBuffer(FlowCapacity.createOutflowCapacity(link), 10);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10);

        // add vehicles
		assertTrue(buffer.isAvailable(0));
        buffer.add(vehicle1, 0);
		assertTrue(buffer.isAvailable(0));
        buffer.add(vehicle2, 0);
		assertFalse(buffer.isAvailable(0));

		// the buffer could accept more vehicles according to flow capacity, but is blocked by the vehicles still inside the buffer
		assertFalse(buffer.isAvailable(2));

        // remove vehicles
        assertEquals(vehicle1.getId(), buffer.peek().getId());
        assertEquals(vehicle1.getId(), buffer.pollFirst().getId());
		assertTrue(buffer.isAvailable(2));
        assertEquals(vehicle2.getId(), buffer.peek().getId());
        assertEquals(vehicle2.getId(), buffer.pollFirst().getId());
    }
}
