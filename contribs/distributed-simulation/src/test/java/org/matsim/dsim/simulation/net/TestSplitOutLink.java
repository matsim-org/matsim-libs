package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.simulation.SimStepMessaging;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestSplitOutLink {

	@Test
	public void init() {
		var toPart = 42;
		var link = TestUtils.createSingleLink(0, toPart);
		var simLink = TestUtils.createLink(link, 0);

		assertInstanceOf(SimLink.SplitOutLink.class, simLink);
		assertEquals(link.getId(), simLink.getId());
		assertEquals(toPart, ((SimLink.SplitOutLink) simLink).getToPart());
	}

	@Test
	public void initStorageCapIncreasedForKinematicWaves() {

		var toPart = 42;
		var link = TestUtils.createSingleLink(0, toPart);
		link.setCapacity(1800);
		link.setFreespeed(10);
		link.setLength(20);
		var node = new SimNode(link.getToNode().getId());
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);

		var simLink = SimLink.create(link, node, config, 10, toPart, _ -> {}, _ -> {});
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		// simple storage capacity should be 2. With increased capacity it should be 3.4. Push a vehicle with pce=3, which would occupy
		// the link for a simple storage capacity, but laves some space when kinematicWaves is used
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 3, 50), SimLink.LinkPosition.QStart, 0);
		// inflow capacity should be recovered after 10 seconds. The link should accept vehicles at t=10 as there is 0.4 pce available
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 9));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 10));
	}

	@Test
	public void storageCapacityWhenUpdated() {

		var link = TestUtils.createSingleLink(0, 42);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		var node = new SimNode(link.getToNode().getId());
		SimLink.SplitOutLink simLink = (SimLink.SplitOutLink) SimLink.create(link, node, config, 50, 0, _ -> {}, _ -> {});

		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		simLink.applyCapacityUpdate(0, 2);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));

		simLink.applyCapacityUpdate(1, 0);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		simLink.applyCapacityUpdate(1, 2);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		simLink.applyCapacityUpdate(2, 0);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
	}

	@Test
	public void storageCapacityWhenPushedQueue() {

		var link = TestUtils.createSingleLink(0, 42);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		var node = new SimNode(link.getToNode().getId());
		var activated = new AtomicInteger(0);
		SimLink.SplitOutLink simLink = (SimLink.SplitOutLink) SimLink.create(link, node, config, 50, 0, _ -> activated.incrementAndGet(), _ -> {});

		// the link can take 2 vehicles. Push two and test whether there is space left.
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 50), SimLink.LinkPosition.QStart, 0);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 1, 50), SimLink.LinkPosition.QStart, 0);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertEquals(2, activated.get());
	}

	@Test
	void storageCapacityWhenPushedKinematicWaves() {

		var link = TestUtils.createSingleLink(0, 42);
		link.setCapacity(1800);
		link.setFreespeed(10);
		link.setLength(20);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		var activated = new AtomicInteger(0);
		var node = new SimNode(link.getToNode().getId());
		SimLink.SplitOutLink simLink = (SimLink.SplitOutLink) SimLink.create(link, node, config, 10, 0, _ -> activated.incrementAndGet(), _ -> {});

		// push one vehicle which consumes inflow capacity
		var now = 0;
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, now));
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 2, 50), SimLink.LinkPosition.QStart, now);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, now));
		// inflow is restored after 8 seconds
		now = 8;
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, now));

		// push another vehicle
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 2, 50), SimLink.LinkPosition.QStart, now);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, now));
		// inflow is restored after 8 seconds, but storage capacity is exhausted
		now = 16;
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, now));
		assertEquals(2, activated.get());
	}

	@Test
	public void sendVehicles() {
		var link = TestUtils.createSingleLink(0, 42);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		var activated = new AtomicInteger(0);
		var node = new SimNode(link.getToNode().getId());
		SimLink.SplitOutLink simLink = (SimLink.SplitOutLink) SimLink.create(link, node, config, 50, 0, _ -> activated.incrementAndGet(), _ -> {});

		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 50), SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 1, 50), SimLink.LinkPosition.QStart, 0);

		// call do sim step, which should pass vehicles to the messagin
		var messaging = mock(SimStepMessaging.class);
		simLink.doSimStep(messaging, 0);
		verify(messaging, times(2)).collectVehicle(any());

		// make sure that the consumed capacity was not released
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertEquals(2, activated.get());
	}
}
