package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.TestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestLocalLink {

	@Test
	public void init() {
		var link = TestUtils.createSingleLink(0, 0);
		var simLink = TestUtils.createLink(link, 0);

		assertInstanceOf(SimLink.LocalLink.class, simLink);
		assertEquals(link.getId(), simLink.getId());
		assertEquals(link.getFlowCapacityPerSec(), simLink.getMaxFlowCapacity());
	}

	@Test
	public void pushVehicleAtStartFIFOQ() {

		var link = TestUtils.createSingleLink(0, 0);
		link.setFreespeed(20);
		var activated = new AtomicInteger(0);
		var node = new SimNode(link.getToNode().getId());
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		var simLink = SimLink.create(link, node, config, 7.5, 0, _ -> activated.incrementAndGet(), _ -> {});
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 1);
		var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10);

		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));

		simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		// buffer is independent of queue
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertEquals(2, activated.get());

		assertEquals(vehicle1.getId(), simLink.peekFirstVehicle().getId());
		assertEquals(link.getLength() / vehicle1.getMaximumVelocity(), vehicle1.getEarliestLinkExitTime());
		assertEquals(link.getLength() / vehicle2.getMaximumVelocity(), vehicle2.getEarliestLinkExitTime());
	}

	@Test
	public void pushVehicleAtStartPassingQ() {
		var link = TestUtils.createSingleLink(0, 0);
		link.setFreespeed(20);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		var node = new SimNode(link.getToNode().getId());
		var activated = new AtomicInteger(0);

		var simLink = SimLink.create(link, node, config, 7.5, 0, _ -> activated.incrementAndGet(), _ -> {});
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 1);
		var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10);

		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));

		simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		// buffer is independent of queue
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertEquals(2, activated.get());

		assertEquals(vehicle2.getId(), simLink.peekFirstVehicle().getId());
		assertEquals(link.getLength() / vehicle1.getMaximumVelocity(), vehicle1.getEarliestLinkExitTime());
		assertEquals(link.getLength() / vehicle2.getMaximumVelocity(), vehicle2.getEarliestLinkExitTime());
	}

	@Test
	void pushVehicleKinematicWaves() {
		var link = TestUtils.createSingleLink(0, 0);
		link.setFreespeed(10);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		var activated = new AtomicInteger(0);
		var node = new SimNode(link.getToNode().getId());
		var simLink = SimLink.create(link, node, config, 7.5, 0, _ -> activated.incrementAndGet(), _ -> {});
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 10., 1);

		// push one vehicle. This consumes the entire inflow, but only some storage
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		assertEquals(1, activated.get());

		// after 25 seconds the inflow should have accumulated again
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 25));
	}

	@Test
	public void pushVehicleAtEnd() {

		var link = TestUtils.createSingleLink(0, 0);
		var linkActivated = new AtomicInteger(0);
		var nodeActivated = new AtomicInteger(0);
		var node = new SimNode(link.getToNode().getId());
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		var simLink = SimLink.create(link, node, config, 7.5, 0, _ -> linkActivated.incrementAndGet(), _ -> nodeActivated.incrementAndGet());
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10);
		var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10);

		// empty links should accept vehicles
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));

		// vehicle 1 blocks 10/13.3 pce
		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertEquals(1, linkActivated.get());

		// both vehicles together block the entire link.
		simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QEnd, 0);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertEquals(2, linkActivated.get());
		assertEquals(0, nodeActivated.get());

		assertEquals(link.getLength() / link.getFreespeed(), vehicle1.getEarliestLinkExitTime());
		assertEquals(0, vehicle2.getEarliestLinkExitTime());
		assertEquals(vehicle2.getId(), simLink.peekFirstVehicle().getId());
	}

	@Test
	public void pushVehicleAtBuffer() {

		var link = TestUtils.createSingleLink(0, 0);
		var linkActivated = new AtomicInteger(0);
		var nodeActivated = new AtomicInteger(0);
		var node = new SimNode(link.getToNode().getId());
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		var simLink = SimLink.create(link, node, config, 7.5, 0, _ -> linkActivated.incrementAndGet(), _ -> nodeActivated.incrementAndGet());
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10);
		var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10);
		var vehicle3 = TestUtils.createVehicle("vehicle-3", 10, 10);

		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));

		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertEquals(2, linkActivated.get());

		simLink.pushVehicle(vehicle3, SimLink.LinkPosition.Buffer, 0);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertEquals(0, vehicle3.getEarliestLinkExitTime());
		assertEquals(1, nodeActivated.get());
	}

	@Test
	public void doSimStep() {
		var link = TestUtils.createSingleLink(0, 0);
		var stuckThreshold = 42;
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setStuckTime(stuckThreshold);
		var simLink = TestUtils.createLink(link, config, 0);
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 1, 10);
		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);

		var now = vehicle1.getEarliestLinkExitTime() - 1;
		simLink.doSimStep(null, now);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertFalse(simLink.isOffering());

		now = vehicle1.getEarliestLinkExitTime();
		simLink.doSimStep(null, now);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertTrue(simLink.isOffering());
		assertFalse(simLink.isStuck(now));

		now = vehicle1.getEarliestLinkExitTime() + stuckThreshold;
		assertTrue(simLink.isStuck(now));
	}

	@Test
	public void doSimStepEnforceFlowCapacity() {

		var link = TestUtils.createSingleLink(0, 0);
		// 2 pce per second
		link.setCapacity(7200);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		config.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var simLink = TestUtils.createLink(link, config, 0);
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 1, 10);
		var vehicle2 = TestUtils.createVehicle("vehicle-2", 3, 10);
		var vehicle3 = TestUtils.createVehicle("vehicle-3", 42, 10);

		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(vehicle3, SimLink.LinkPosition.QStart, 0);

		// move vehicles 1 and 2 to the buffer
		var now = vehicle1.getEarliestLinkExitTime();
		simLink.doSimStep(null, now);
		assertTrue(simLink.isOffering());
		assertEquals(vehicle1.getId(), simLink.popVehicle().getId());
		assertTrue(simLink.isOffering());
		assertEquals(vehicle2.getId(), simLink.popVehicle().getId());

		now = now + (vehicle1.getSizeInEquivalents() + vehicle2.getSizeInEquivalents()) / simLink.getMaxFlowCapacity() - 1;
		simLink.doSimStep(null, now);
		assertFalse(simLink.isOffering());

		now = now + 1;
		simLink.doSimStep(null, now);
		assertTrue(simLink.isOffering());
		assertEquals(vehicle3.getId(), simLink.popVehicle().getId());
	}

	@Test
	void doSimStepKinematicWavesEnsureHoles() {
		var link = TestUtils.createSingleLink(0, 0);
		link.setCapacity(3600);
		link.setFreespeed(10);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		var node = new SimNode(link.getToNode().getId());
		var simLink = SimLink.create(link, node, config, 7.5, 0, _ -> {}, _ -> {});
		var vehicle = TestUtils.createVehicle("vehicle-3", 42, 10);

		simLink.pushVehicle(vehicle, SimLink.LinkPosition.QEnd, 0);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));

		// move the vehicle into the buffer, which starts a backwards travelling hole.
		assertFalse(simLink.doSimStep(null, 0));
		assertTrue(simLink.isOffering());
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));

		// remove the vehicle from the buffer - not strictly necessary
		assertEquals(vehicle.getId(), simLink.popVehicle().getId());
		assertFalse(simLink.isOffering());

		// the backward travelling hole arrives after 24 seconds
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 23));
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 23));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart, 24));
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 24));
	}

	@Test
	public void doSimStepRemoveVehicle() {
		var link = TestUtils.createSingleLink(0, 0);
		link.setCapacity(3600);
		link.setFreespeed(10);
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10);
		var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10);
		var simLink = TestUtils.createLink(link, 0);
		simLink.addLeaveHandler((v, l, n) -> {
			assertEquals(link.getId(), l.getId());
			if (v.getId().equals(vehicle1.getId())) {
				assertEquals(vehicle1.getEarliestLinkExitTime(), n);
				return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
			}
			if (v.getId().equals(vehicle2.getId())) {
				assertEquals(vehicle2.getEarliestLinkExitTime(), n);
				return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
			}

			throw new RuntimeException("unexpected vehicle with id: " + v.getId());
		});

		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 1);

		// vehicle1 is at the head of the queue/buffer
		assertEquals(vehicle1.getId(), simLink.peekFirstVehicle().getId());
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));

		// remove the first vehicle
		var now = vehicle1.getEarliestLinkExitTime();
		simLink.doSimStep(null, now);

		// vehicle2 should be at the head of the queue
		assertEquals(vehicle2.getId(), simLink.peekFirstVehicle().getId());
		assertFalse(simLink.isOffering());

		// move second vehicle into the buffer
		now = vehicle2.getEarliestLinkExitTime();
		simLink.doSimStep(null, now);
		assertTrue(simLink.isOffering());
		assertEquals(vehicle2.getId(), simLink.popVehicle().getId());
	}

	@Test
	public void doSimStepBlockQueue() {
		var link = TestUtils.createSingleLink(0, 0);
		link.setCapacity(3600);
		link.setFreespeed(10);
		var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10);
		var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10);
		var simLink = TestUtils.createLink(link, 0);
		var leaveHandlerCounter = new AtomicInteger(0);
		var blockTime = 42;
		simLink.addLeaveHandler((v, _, n) -> {
			if (leaveHandlerCounter.get() == 0) {
				leaveHandlerCounter.incrementAndGet();
				v.setEarliestLinkExitTime(n + blockTime);
				return SimLink.OnLeaveQueueInstruction.BlockQueue;
			} else {
				return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
			}
		});

		simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
		simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);

		// vehicle1 is at the head of the queue/buffer
		assertEquals(vehicle1.getId(), simLink.peekFirstVehicle().getId());
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));

		// first vehicle blocks the queue
		var now = vehicle1.getEarliestLinkExitTime();
		simLink.doSimStep(null, now);
		assertEquals(vehicle1.getId(), simLink.peekFirstVehicle().getId());
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart, 0));
		assertFalse(simLink.isOffering());

		// move the blocking vehicle
		now = now + blockTime;
		simLink.doSimStep(null, now);
		assertTrue(simLink.isOffering());
		var pop1 = simLink.popVehicle();
		assertEquals(vehicle1.getId(), pop1.getId());
		assertEquals(now, pop1.getEarliestLinkExitTime());

		// move the blocked vehicle, after flow capacity is restored
		now = now + pop1.getSizeInEquivalents() / simLink.getMaxFlowCapacity();
		simLink.doSimStep(null, now);
		assertTrue(simLink.isOffering());
		var pop2 = simLink.popVehicle();
		assertEquals(vehicle2.getId(), pop2.getId());
		assertEquals(link.getLength() / link.getFreespeed(), pop2.getEarliestLinkExitTime());
	}

	@Test
	public void popVehicleEmptyBuffer() {

		var link = TestUtils.createSingleLink(0, 0);
		var simLink = TestUtils.createLink(link, 0);
		assertThrows(RuntimeException.class, simLink::popVehicle);
	}
}
