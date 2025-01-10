package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.simulation.SimStepMessaging;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestSplitInLink {

	@Test
	public void init() {
		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		var node = new SimNode(link.getToNode().getId());
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> {}, _ -> {});

		assertInstanceOf(SimLink.SplitInLink.class, simLink);
		assertEquals(fromPart, ((SimLink.SplitInLink) simLink).getFromPart());
	}

	@Test
	public void storageCapConsumed() {
		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		link.setCapacity(3600);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		var node = new SimNode(link.getToNode().getId());
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> {}, _ -> {});
		// push vehicles onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 10), SimLink.LinkPosition.Buffer, 0);
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 2, 10), SimLink.LinkPosition.QEnd, 0);

		var messaging1 = mock(SimStepMessaging.class);
		simLink.doSimStep(messaging1, 0);
		verify(messaging1).collectStorageCapacityUpdate(
			simLink.getId(), 0, 2, fromPart
		);
	}

	@Test
	public void storageCapReleasedVehicleMovedToBuffer() {

		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		link.setCapacity(3600);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		dsimConfig.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		dsimConfig.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var node = new SimNode(link.getToNode().getId());
		var wasActivated = new AtomicInteger(0);
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> wasActivated.incrementAndGet(), _ -> {});
		// push vehicles onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 10), SimLink.LinkPosition.QEnd, 0);
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 2, 10), SimLink.LinkPosition.QEnd, 0);
		assertEquals(4, wasActivated.get());

		var messaging = mock(SimStepMessaging.class);
		simLink.doSimStep(messaging, 0);
		// 3 PCEs are consumed because of both vehicles entering the link. 2 PCE is released because vehicle-2 moved to
		// the buffer (vehicle 2 is pushed to the head of the q)
		verify(messaging).collectStorageCapacityUpdate(
			simLink.getId(), 2, 3, fromPart
		);
		assertTrue(simLink.isOffering());
		assertEquals(Id.createVehicleId("vehicle-2"), simLink.popVehicle().getId());
		assertFalse(simLink.isOffering());
		assertEquals(Id.createVehicleId("vehicle-1"), simLink.peekFirstVehicle().getId());
	}

	@Test
	public void storageCapReleasedVehicleMovedToBufferKinematicWaves() {

		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		link.setCapacity(3600);
		link.setFreespeed(10);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		dsimConfig.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		var node = new SimNode(link.getToNode().getId());
		var wasActivated = new AtomicInteger(0);
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> wasActivated.incrementAndGet(), _ -> {});
		// push vehicle onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 42, 10), SimLink.LinkPosition.QEnd, 0);
		assertEquals(2, wasActivated.get());

		var messaging = mock(SimStepMessaging.class);
		assertTrue(simLink.doSimStep(messaging, 0));
		verify(messaging).collectStorageCapacityUpdate(
			simLink.getId(), 0, 42, fromPart);
		assertTrue(simLink.isOffering());
		assertEquals(Id.createVehicleId("vehicle-1"), simLink.popVehicle().getId());

		// the hole should arrive after 24 seconds. Then it is sent upstream
		assertFalse(simLink.doSimStep(messaging, 24));
		verify(messaging).collectStorageCapacityUpdate(
			simLink.getId(), 42, 0, fromPart);
	}

	@Test
	public void storageCapReleasedArrival() {

		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		link.setCapacity(3600);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		dsimConfig.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		dsimConfig.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var node = new SimNode(link.getToNode().getId());
		var wasActivated = new AtomicInteger(0);
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> wasActivated.incrementAndGet(), _ -> {});
		// push vehicles onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 10), SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 2, 10), SimLink.LinkPosition.QStart, 0);
		assertEquals(4, wasActivated.get());

		simLink.addLeaveHandler((_, _, _) -> SimLink.OnLeaveQueueInstruction.RemoveVehicle);
		simLink.doSimStep(mock(SimStepMessaging.class), 99);

		var messaging = mock(SimStepMessaging.class);
		simLink.doSimStep(messaging, 100);
		verify(messaging).collectStorageCapacityUpdate(
			simLink.getId(), 3, 0, fromPart
		);
	}
}
