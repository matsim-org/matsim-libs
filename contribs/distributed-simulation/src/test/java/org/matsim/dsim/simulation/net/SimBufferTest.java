package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.dsim.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SimBufferTest {

    @Test
    void init() {
        var buffer = new SimBuffer(23);
        assertEquals(23, buffer.getMaxFlowCapacity());
        assertEquals(0, buffer.getPceInBuffer());
        assertTrue(buffer.isAvailable());
    }

    @Test
    void addVehicle() {
        var stuckThreshold = 11;
        var buffer = new SimBuffer(2);
        var vehicle = TestUtils.createVehicle("vehicle-1", 10, 10, stuckThreshold);

        // push vehicle which consumes flow capacity and starts the stuck timer of the vehicle.
        assertTrue(buffer.isAvailable());
        buffer.add(vehicle, 0);
        assertFalse(buffer.isAvailable());
        assertEquals(vehicle.getPce(), buffer.getPceInBuffer());
        assertFalse(buffer.peek().isStuck(0));
        assertTrue(buffer.peek().isStuck(stuckThreshold));

        // remove the vehicle which resets the stuck timer
        var polled = buffer.pollFirst();
        assertFalse(polled.isStuck(stuckThreshold));

        // make sure that flow capacity is rebuild slowly
        var now = vehicle.getPce() / buffer.getMaxFlowCapacity() - 1;
        buffer.updateFlowCapacity(now);
        assertFalse(buffer.isAvailable());

        now = vehicle.getPce() / buffer.getMaxFlowCapacity();
        buffer.updateFlowCapacity(now);
        assertTrue(buffer.isAvailable());
    }

    @Test
    void addVehicles() {

        var buffer = new SimBuffer(15);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10, 10);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10, 10);

        // add vehicles
        assertTrue(buffer.isAvailable());
        buffer.add(vehicle1, 0);
        assertTrue(buffer.isAvailable());
        buffer.add(vehicle2, 0);
        assertFalse(buffer.isAvailable());

        // build up accumulated flow capacity
        buffer.updateFlowCapacity(2);
        // the buffer could accept more vehicles, but is blocked by the vehicles still inside the buffer
        assertFalse(buffer.isAvailable());

        // remove vehicles
        assertEquals(vehicle1.getId(), buffer.peek().getId());
        assertEquals(vehicle1.getId(), buffer.pollFirst().getId());
        assertTrue(buffer.isAvailable());
        assertEquals(vehicle2.getId(), buffer.peek().getId());
        assertEquals(vehicle2.getId(), buffer.pollFirst().getId());
    }
}