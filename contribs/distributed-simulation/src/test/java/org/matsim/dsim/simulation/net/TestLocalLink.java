package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.TestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestLocalLink {

    @Test
    public void init() {
        var link = TestUtils.createSingleLink(0, 0);
        var simLink = SimLink.create(link, 0);

        assertInstanceOf(SimLink.LocalLink.class, simLink);
        assertEquals(link.getId(), simLink.getId());
        assertEquals(link.getToNode().getId(), simLink.getToNode());
        assertEquals(link.getFlowCapacityPerSec(), simLink.getMaxFlowCapacity());
    }

    @Test
    public void pushVehicleAtStartFIFOQ() {

        var link = TestUtils.createSingleLink(0, 0);
        link.setFreespeed(20);
        var simLink = SimLink.create(link, 0);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 1, 30);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10, 30);

        simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd));

        simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd));
        // buffer is independent of queue
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer));

        assertEquals(vehicle1.getId(), simLink.peekFirstVehicle().getId());
        assertEquals(link.getLength() / vehicle1.getMaxV(), vehicle1.getEarliestExitTime());
        assertEquals(link.getLength() / vehicle2.getMaxV(), vehicle2.getEarliestExitTime());
    }

    @Test
    public void pushVehicleAtStartPassingQ() {
        var link = TestUtils.createSingleLink(0, 0);
        link.setFreespeed(20);
        var simLink = SimLink.create(link, SimLink.OnLeaveQueue.defaultHandler(), QSimConfigGroup.LinkDynamics.PassingQ, 7.5, 0);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 1, 30);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10, 30);

        simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd));

        simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd));
        // buffer is independent of queue
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer));

        assertEquals(vehicle2.getId(), simLink.peekFirstVehicle().getId());
        assertEquals(link.getLength() / vehicle1.getMaxV(), vehicle1.getEarliestExitTime());
        assertEquals(link.getLength() / vehicle2.getMaxV(), vehicle2.getEarliestExitTime());
    }

    @Test
    public void pushVehicleAtEnd() {

        var link = TestUtils.createSingleLink(0, 0);
        var simLink = SimLink.create(link, 0);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10, 30);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10, 30);

        // empty links should accept vehicles
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer));

        // vehicle 1 blocks 10/13.3 pce
        simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer));

        // both vehicles together block the entire link.
        simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QEnd, 0);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer));

        assertEquals(link.getLength() / link.getFreespeed(), vehicle1.getEarliestExitTime());
        assertEquals(0, vehicle2.getEarliestExitTime());
        assertEquals(vehicle2.getId(), simLink.peekFirstVehicle().getId());
    }

    @Test
    public void pushVehicleAtBuffer() {

        var link = TestUtils.createSingleLink(0, 0);
        var simLink = SimLink.create(link, 0);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10, 30);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10, 30);
        var vehicle3 = TestUtils.createVehicle("vehicle-3", 10, 10, 30);

        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer));

        simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
        simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd));
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer));

        simLink.pushVehicle(vehicle3, SimLink.LinkPosition.Buffer, 0);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer));
        assertEquals(0, vehicle3.getEarliestExitTime());
    }

    @Test
    public void doSimStep() {
        var link = TestUtils.createSingleLink(0, 0);
        var simLink = SimLink.create(link, 0);
        var stuckThreshold = 42;
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 1, 10, stuckThreshold);
        simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);

        var now = vehicle1.getEarliestExitTime() - 1;
        simLink.doSimStep(null, now);
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer));
        assertFalse(simLink.isOffering());

        now = vehicle1.getEarliestExitTime();
        simLink.doSimStep(null, now);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer));
        assertTrue(simLink.isOffering());
        assertFalse(simLink.peekFirstVehicle().isStuck(now));

        now = vehicle1.getEarliestExitTime() + stuckThreshold;
        assertTrue(simLink.peekFirstVehicle().isStuck(now));
    }

    @Test
    public void doSimStepEnforceFlowCapacity() {

        var link = TestUtils.createSingleLink(0, 0);
        // 2 pce per second
        link.setCapacity(7200);
        var simLink = SimLink.create(link, 0);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 1, 10, 30);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 3, 10, 30);
        var vehicle3 = TestUtils.createVehicle("vehicle-3", 42, 10, 30);

        simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
        simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);
        simLink.pushVehicle(vehicle3, SimLink.LinkPosition.QStart, 0);

        // move vehicles 1 and 2 to the buffer
        var now = vehicle1.getEarliestExitTime();
        simLink.doSimStep(null, now);
        assertTrue(simLink.isOffering());
        assertEquals(vehicle1.getId(), simLink.popVehicle().getId());
        assertTrue(simLink.isOffering());
        assertEquals(vehicle2.getId(), simLink.popVehicle().getId());

        now = now + (vehicle1.getPce() + vehicle2.getPce()) / simLink.getMaxFlowCapacity() - 1;
        simLink.doSimStep(null, now);
        assertFalse(simLink.isOffering());

        now = now + 1;
        simLink.doSimStep(null, now);
        assertTrue(simLink.isOffering());
        assertEquals(vehicle3.getId(), simLink.popVehicle().getId());
    }

    @Test
    public void doSimStepRemoveVehicle() {
        var link = TestUtils.createSingleLink(0, 0);
        link.setCapacity(3600);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10, 30);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10, 30);
        var simLink = SimLink.create(link, 0);
        simLink.addLeaveHandler((v, l, n) -> {
            assertEquals(link.getId(), l.getId());
            if (v.getId().equals(vehicle1.getId())) {
                assertEquals(vehicle1.getEarliestExitTime(), n);
                return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
            }
            if (v.getId().equals(vehicle2.getId())) {
                assertEquals(vehicle2.getEarliestExitTime(), n);
                return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
            }

            throw new RuntimeException("unexpected vehicle with id: " + v.getId());
        });

        simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
        simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 1);

        // vehicle1 is at the head of the queue/buffer
        assertEquals(vehicle1.getId(), simLink.peekFirstVehicle().getId());
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));

        // remove the first vehicle
        var now = vehicle1.getEarliestExitTime();
        simLink.doSimStep(null, now);

        // vehicle2 should be at the head of the queue
        assertEquals(vehicle2.getId(), simLink.peekFirstVehicle().getId());
        assertFalse(simLink.isOffering());

        // move second vehicle into the buffer
        now = vehicle2.getEarliestExitTime();
        simLink.doSimStep(null, now);
        assertTrue(simLink.isOffering());
        assertEquals(vehicle2.getId(), simLink.popVehicle().getId());
    }

    @Test
    public void doSimStepBlockQueue() {
        var link = TestUtils.createSingleLink(0, 0);
        link.setCapacity(3600);
        var vehicle1 = TestUtils.createVehicle("vehicle-1", 10, 10, 30);
        var vehicle2 = TestUtils.createVehicle("vehicle-2", 10, 10, 30);
        var simLink = SimLink.create(link, 0);
        var leaveHandlerCounter = new AtomicInteger(0);
        var blockTime = 42;
        simLink.addLeaveHandler((v, _, n) -> {
            if (leaveHandlerCounter.get() == 0) {
                leaveHandlerCounter.incrementAndGet();
                v.setEarliestExitTime(n + blockTime);
                return SimLink.OnLeaveQueueInstruction.BlockQueue;
            } else {
                return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
            }
        });

        simLink.pushVehicle(vehicle1, SimLink.LinkPosition.QStart, 0);
        simLink.pushVehicle(vehicle2, SimLink.LinkPosition.QStart, 0);

        // vehicle1 is at the head of the queue/buffer
        assertEquals(vehicle1.getId(), simLink.peekFirstVehicle().getId());
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));

        // first vehicle blocks the queue
        var now = vehicle1.getEarliestExitTime();
        simLink.doSimStep(null, now);
        assertEquals(vehicle1.getId(), simLink.peekFirstVehicle().getId());
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));
        assertFalse(simLink.isOffering());

        // move the blocking vehicle
        now = now + blockTime;
        simLink.doSimStep(null, now);
        assertTrue(simLink.isOffering());
        var pop1 = simLink.popVehicle();
        assertEquals(vehicle1.getId(), pop1.getId());
        assertEquals(now, pop1.getEarliestExitTime());

        // move the blocked vehicle, after flow capacity is restored
        now = now + pop1.getPce() / simLink.getMaxFlowCapacity();
        simLink.doSimStep(null, now);
        assertTrue(simLink.isOffering());
        var pop2 = simLink.popVehicle();
        assertEquals(vehicle2.getId(), pop2.getId());
        assertEquals(link.getLength() / link.getFreespeed(), pop2.getEarliestExitTime());
    }

    @Test
    public void popVehicleEmptyBuffer() {

        var link = TestUtils.createSingleLink(0, 0);
        var simLink = SimLink.create(link, 0);
        assertThrows(RuntimeException.class, simLink::popVehicle);
    }
}
