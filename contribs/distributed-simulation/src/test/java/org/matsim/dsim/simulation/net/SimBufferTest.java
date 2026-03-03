package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.dsim.TestUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimBufferTest {

	@Test
	void init() {
		Link link = TestUtils.createSingleLink();
		link.setCapacity(23 * 3600);
		var node = new SimNode(link.getToNode().getId());
		var buffer = new SimBuffer(FlowCapacity.createOutflowCapacity(link), 30, node, _ -> {
		});
		assertEquals(23, buffer.getMaxFlowCapacity());
		assertEquals(0, buffer.getPceInBuffer());
		assertTrue(buffer.isAvailable());
	}

	@Test
	void addVehicle() {
		var stuckThreshold = 11;
		var link = TestUtils.createSingleLink();
		link.setCapacity(7200);
		var node = new SimNode(link.getToNode().getId());
		var wasActivated = new AtomicBoolean(false);
		var buffer = new SimBuffer(FlowCapacity.createOutflowCapacity(link), stuckThreshold, node, n -> {
			assertEquals(node.getId(), n.getId());
			wasActivated.set(true);
		});
		var vehicle = TestUtils.createVehicle("vehicle-1", 10, 10);

		// push vehicle which consumes flow capacity and starts the stuck timer
		assertTrue(buffer.isAvailable());
		buffer.add(vehicle, 0);
		assertTrue(wasActivated.get());
		assertFalse(buffer.isAvailable());
		assertEquals(vehicle.getSizeInEquivalents(), buffer.getPceInBuffer());
		assertFalse(buffer.isStuck(0));
		assertTrue(buffer.isStuck(stuckThreshold));

		// Remove the vehicle
		buffer.pollFirst();

		// make sure that flow capacity is rebuild slowly
		var now = vehicle.getSizeInEquivalents() / buffer.getMaxFlowCapacity() - 1;
		buffer.update(now);
		assertFalse(buffer.isAvailable());

		now = vehicle.getSizeInEquivalents() / buffer.getMaxFlowCapacity();
		buffer.update(now);
		assertTrue(buffer.isAvailable());
	}

	@Test
	void addVehicles() {

		var link = TestUtils.createSingleLink();
		link.setCapacity(15 * 3500);
		var wasActivated = new AtomicInteger(0);
		var node = new SimNode(link.getToNode().getId());
		var buffer = new SimBuffer(FlowCapacity.createOutflowCapacity(link), 10, node, _ -> wasActivated.incrementAndGet());
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10);
		var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10);

		// add vehicles
		assertTrue(buffer.isAvailable());
		buffer.add(vehicle1, 0);
		assertTrue(buffer.isAvailable());
		buffer.add(vehicle2, 0);
		assertFalse(buffer.isAvailable());
		assertEquals(2, wasActivated.get());

		// the buffer could accept more vehicles according to flow capacity, but is blocked by the vehicles still inside the buffer
		buffer.update(2);
		assertFalse(buffer.isAvailable());

		// remove vehicles
		assertEquals(vehicle1.getId(), buffer.peek().getId());
		assertEquals(vehicle1.getId(), buffer.pollFirst().getId());
		buffer.update(2);
		assertTrue(buffer.isAvailable());
		assertEquals(vehicle2.getId(), buffer.peek().getId());
		assertEquals(vehicle2.getId(), buffer.pollFirst().getId());
	}
}
