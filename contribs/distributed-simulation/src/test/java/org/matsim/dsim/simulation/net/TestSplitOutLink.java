package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.simulation.SimStepMessaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestSplitOutLink {

    @Test
    public void init() {
        var toPart = 42;
        var link = TestUtils.createSingleLink(0, toPart);
        var simLink = SimLink.create(link, 0);

        assertInstanceOf(SimLink.SplitOutLink.class, simLink);
        assertEquals(link.getId(), simLink.getId());
        assertEquals(toPart, ((SimLink.SplitOutLink) simLink).getToPart());
    }

    @Test
    public void storageCapacityWhenUpdated() {

        var link = TestUtils.createSingleLink(0, 42);
        SimLink.SplitOutLink simLink = (SimLink.SplitOutLink) SimLink.create(link, SimLink.OnLeaveQueue.defaultHandler(), QSimConfigGroup.LinkDynamics.FIFO, 50, 0);

        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        simLink.applyCapacityUpdate(0, 2);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));

        simLink.applyCapacityUpdate(1, 0);
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        simLink.applyCapacityUpdate(1, 2);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));
        simLink.applyCapacityUpdate(2, 0);
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
    }

    @Test
    public void storageCapacityWhenPushed() {
        var link = TestUtils.createSingleLink(0, 42);
        var simLink = SimLink.create(link, SimLink.OnLeaveQueue.defaultHandler(), QSimConfigGroup.LinkDynamics.FIFO, 50, 0);

        // the link can take 2 vehicles. Push two and test whether there is space left.
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 50, 30), SimLink.LinkPosition.QStart, 0);
        assertTrue(simLink.isAccepting(SimLink.LinkPosition.QStart));
        simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 1, 50, 30), SimLink.LinkPosition.QStart, 0);
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));
    }

    @Test
    public void sendVehicles() {
        var link = TestUtils.createSingleLink(0, 42);
        SimLink.SplitOutLink simLink = (SimLink.SplitOutLink) SimLink.create(link, SimLink.OnLeaveQueue.defaultHandler(), QSimConfigGroup.LinkDynamics.FIFO, 50, 0);

        simLink.pushVehicle(TestUtils.createVehicle("vehicle-1", 1, 50, 30), SimLink.LinkPosition.QStart, 0);
        simLink.pushVehicle(TestUtils.createVehicle("vehicle-2", 1, 50, 30), SimLink.LinkPosition.QStart, 0);

        // call do sim step, which should pass vehicles to the messagin
        var messaging = mock(SimStepMessaging.class);
        simLink.doSimStep(messaging, 0);
        verify(messaging, times(2)).collectVehicle(any());

        // make sure that the consumed capacity was not released
        assertFalse(simLink.isAccepting(SimLink.LinkPosition.QStart));
    }
}
