package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SimQueueTest {

	@Test
	void linkDynamicsFifoTrafficDynamicsQueue() {

		var link = TestUtils.createSingleLink();
		var config = ConfigUtils.createConfig();
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var vehicle1 = TestUtils.createVehicle("veh-1", 4, 10, 30);
		var vehicle2 = TestUtils.createVehicle("veh-2", 4, 10, 30);
		var vehicle3 = TestUtils.createVehicle("veh-3", 4, 10, 30);
		var queue = SimQueue.create(link, config.qsim(), 10);

		assertTrue(queue.isEmpty());
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));

		// capacity should be consumed immediately
		vehicle1.setEarliestExitTime(10);
		queue.add(vehicle1, SimLink.LinkPosition.QStart);
		assertFalse(queue.isEmpty());
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));
		vehicle2.setEarliestExitTime(9);
		queue.add(vehicle2, SimLink.LinkPosition.QStart);
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));
		vehicle3.setEarliestExitTime(10);
		queue.add(vehicle3, SimLink.LinkPosition.QEnd);
		assertFalse(queue.isAccepting(SimLink.LinkPosition.QStart, 0));

		// we are expecting veh3, veh1, veh2, as veh3 was added at the downstream end, veh1 and veh2 should keep the
		// insertion order, as they were added at the upstream end
		assertEquals(vehicle3.getId(), queue.poll(0).getId());
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertEquals(vehicle1.getId(), queue.poll(0).getId());
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertEquals(vehicle2.getId(), queue.poll(0).getId());
	}

	@Test
	void linkDynamicsPassingTrafficDynamicsQueue() {
		var link = TestUtils.createSingleLink();
		var config = ConfigUtils.createConfig();
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
		var vehicle1 = TestUtils.createVehicle("veh-1", 5, 10, 30);
		var vehicle2 = TestUtils.createVehicle("veh-2", 5, 10, 30);
		var queue = SimQueue.create(link, config.qsim(), 10);

		assertTrue(queue.isEmpty());
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));

		// capacity should be consumed immediately
		vehicle1.setEarliestExitTime(10);
		queue.add(vehicle1, SimLink.LinkPosition.QStart);
		assertFalse(queue.isEmpty());
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));
		vehicle2.setEarliestExitTime(9);
		queue.add(vehicle2, SimLink.LinkPosition.QStart);
		assertFalse(queue.isAccepting(SimLink.LinkPosition.QStart, 0));

		// we are using fifo queue veh1 should be at the head of the queue even though its exit time is later than veh2
		assertEquals(vehicle2.getId(), queue.peek().getId());

		// released capacity should be available immediately
		assertEquals(vehicle2.getId(), queue.poll(0).getId());
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertEquals(vehicle1.getId(), queue.poll(0).getId());
	}

	@Test
	void linkDynamicsFifoTrafficDynamicsKinematicWaves() {

		var link = TestUtils.createSingleLink();
		link.setLength(15);
		link.setCapacity(1800);
		link.setFreespeed(10);
		var holeTravelTime = link.getLength() / KinematicWavesStorageCapacity.HOLE_SPEED;
		var config = ConfigUtils.createConfig();
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var vehicle1 = TestUtils.createVehicle("veh-1", 2, 10, 30);
		var vehicle2 = TestUtils.createVehicle("veh-2", 1, 10, 30);
		var queue = SimQueue.create(link, config.qsim(), 5);

		assertTrue(queue.isEmpty());
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 0));
		// the link should accept vehicles according to its max inflow capacity
		// vehicle-1 consumes 2pcu. Max inflow should be: 1/cellSize / (1/vHole + 1/vMax) = 0.588...
		queue.add(vehicle1, SimLink.LinkPosition.QStart);
		// acc inflow is -1.422
		assertFalse(queue.isEmpty());
		assertFalse(queue.isAccepting(SimLink.LinkPosition.QStart, 0));
		// acc inflow is 1.422 + 2 * 0.588 = -0.236
		assertFalse(queue.isAccepting(SimLink.LinkPosition.QStart, 2));
		// acc inflow is -0.236 + 0.588 = + 0.352 > 0
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 3));

		// add another vehicle
		queue.add(vehicle2, SimLink.LinkPosition.QEnd);
		assertFalse(queue.isAccepting(SimLink.LinkPosition.QStart, 3));

		// remove the first vehicle from the queue which should create a hole.
		assertEquals(vehicle2.getId(), queue.poll(3).getId());
		assertFalse(queue.isAccepting(SimLink.LinkPosition.QStart, 3));

		assertFalse(queue.isAccepting(SimLink.LinkPosition.QStart, 3 + holeTravelTime - 1));
		assertTrue(queue.isAccepting(SimLink.LinkPosition.QStart, 3 + holeTravelTime));
	}
}
