package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.simulation.PartitionTransfer;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestSplitInLink {

	@Test
	public void init() {
		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		var node = new SimNode(link.getToNode().getId());
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> {}, _ -> {}, mock(PartitionTransfer.class));

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
		var messaging1 = mock(PartitionTransfer.class);
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> {}, _ -> {}, messaging1);
		// push vehicles onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 10), SimLink.LinkPosition.Buffer, 0);
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 2, 10), SimLink.LinkPosition.QEnd, 0);

		simLink.doSimStep(0);

		var captor = ArgumentCaptor.forClass(SimLink.SplitInLink.CapacityUpdate.class);
		verify(messaging1).collect(captor.capture(), eq(fromPart));
		assertEquals(simLink.getId(), captor.getValue().linkId());
		assertEquals(0, captor.getValue().released());
		assertEquals(2, captor.getValue().consumed());
	}

	@Test
	public void storageCapReleasedVehicleMovedToBuffer() {

		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		link.setCapacity(3600);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		dsimConfig.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		dsimConfig.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var messaging = mock(PartitionTransfer.class);
		var node = new SimNode(link.getToNode().getId());
		var wasActivated = new AtomicInteger(0);
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> wasActivated.incrementAndGet(), _ -> {}, messaging);
		// push vehicles onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 10), SimLink.LinkPosition.QEnd, 0);
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 2, 10), SimLink.LinkPosition.QEnd, 0);
		assertEquals(4, wasActivated.get());


		simLink.doSimStep(0);

		// verify the call to the messaging:
		// 3 PCEs are consumed because of both vehicles entering the link. 2 PCE is released because vehicle-2 moved to
		// the buffer (vehicle 2 is pushed to the head of the q)
		var captor = ArgumentCaptor.forClass(SimLink.SplitInLink.CapacityUpdate.class);
		verify(messaging).collect(captor.capture(), eq(fromPart));
		assertEquals(simLink.getId(), captor.getValue().linkId());
		assertEquals(2, captor.getValue().released());
		assertEquals(3, captor.getValue().consumed());

		// verify the state of the link
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
		var messaging = mock(PartitionTransfer.class);
		var node = new SimNode(link.getToNode().getId());
		var wasActivated = new AtomicInteger(0);
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> wasActivated.incrementAndGet(), _ -> {}, messaging);
		// push vehicle onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 42, 10), SimLink.LinkPosition.QEnd, 0);
		assertEquals(2, wasActivated.get());

		assertTrue(simLink.doSimStep(0));

		// verify message transfer
		var inOrder = inOrder(messaging); // we need in order, so that we can have separate captors for verifying the calls to the messaging mock
		var captor1 = ArgumentCaptor.forClass(SimLink.SplitInLink.CapacityUpdate.class);
		inOrder.verify(messaging).collect(captor1.capture(), eq(fromPart));
		assertEquals(simLink.getId(), captor1.getValue().linkId());
		assertEquals(0, captor1.getValue().released());
		assertEquals(42, captor1.getValue().consumed());

		// verify link state
		assertTrue(simLink.isOffering());
		assertEquals(Id.createVehicleId("vehicle-1"), simLink.popVehicle().getId());

		// the hole should arrive after 24 seconds. Then it is sent upstream
		assertFalse(simLink.doSimStep(24));
		var captor2 = ArgumentCaptor.forClass(SimLink.SplitInLink.CapacityUpdate.class);
		inOrder.verify(messaging).collect(captor2.capture(), eq(fromPart));
		assertEquals(simLink.getId(), captor2.getValue().linkId());
		assertEquals(42, captor2.getValue().released());
		assertEquals(0, captor2.getValue().consumed());
	}

	@Test
	public void storageCapReleasedArrival() {

		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		link.setCapacity(3600);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		dsimConfig.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		dsimConfig.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var messaging = mock(PartitionTransfer.class);
		var node = new SimNode(link.getToNode().getId());
		var wasActivated = new AtomicInteger(0);
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> wasActivated.incrementAndGet(), _ -> {}, messaging);
		// push vehicles onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 10), SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 2, 10), SimLink.LinkPosition.QStart, 0);
		assertEquals(4, wasActivated.get());

		simLink.addLeaveHandler((_, _, _) -> SimLink.OnLeaveQueueInstruction.RemoveVehicle);
		simLink.doSimStep(99);


		simLink.doSimStep(100);
		var captor = ArgumentCaptor.forClass(SimLink.SplitInLink.CapacityUpdate.class);
		verify(messaging).collect(captor.capture(), eq(fromPart));
		assertEquals(simLink.getId(), captor.getValue().linkId());
		assertEquals(3, captor.getValue().released());
		assertEquals(0, captor.getValue().consumed());
	}

	@Test
	public void storageCapReleasedArrivalHole() {
		var fromPart = 42;
		var link = TestUtils.createSingleLink(fromPart, 0);
		link.setCapacity(3600);
		var dsimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		dsimConfig.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		dsimConfig.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var messaging = mock(PartitionTransfer.class);
		var node = new SimNode(link.getToNode().getId());
		var wasActivated = new AtomicInteger(0);
		var simLink = SimLink.create(link, node, dsimConfig, 10, 0, _ -> wasActivated.incrementAndGet(), _ -> {}, messaging);
		// push vehicles onto link and check that the consumed capacity is passed upstream
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 10), SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 2, 10), SimLink.LinkPosition.QStart, 0);
		assertEquals(4, wasActivated.get());

		simLink.addLeaveHandler((_, _, _) -> SimLink.OnLeaveQueueInstruction.RemoveVehicle);
		simLink.doSimStep(99);


		simLink.doSimStep(100);
		verify(messaging, never()).collect(any(), anyInt());

		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 124));
		assertEquals(5, wasActivated.get());
		assertFalse(simLink.doSimStep(124));

		var captor = ArgumentCaptor.forClass(SimLink.SplitInLink.CapacityUpdate.class);
		verify(messaging).collect(captor.capture(), eq(fromPart));
		assertEquals(simLink.getId(), captor.getValue().linkId());
		assertEquals(3, captor.getValue().released());
		assertEquals(0, captor.getValue().consumed());
	}
}
