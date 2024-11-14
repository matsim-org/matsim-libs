package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.simulation.SimPerson;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.dsim.simulation.SimpleAgent;
import org.matsim.dsim.simulation.SimpleLeg;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Wait2LinkTest {

    @Test
    void vehicleIntoBuffer() {

        var em = mock(EventsManager.class);
        var messaging = mock(SimStepMessaging.class);
        var activeLinks = new ActiveLinks(messaging);
        var wait2link = new Wait2Link(em, activeLinks);
        var link = TestUtils.createSingleLink(0, 0);
        link.setCapacity(3600);
        var simLink = TestUtils.createLink(link, 0, 10);
        var simPerson1 = mock(SimpleAgent.class);

        when(simPerson1.chooseNextLinkId()).thenReturn(Id.createLinkId("any-next-link"));

		// TODO: leg won't be defined
//        when(simPerson1.getCurrentLeg()).thenReturn(SimpleLeg.builder().setMode("some-mode").build());

        var simPerson2 = mock(SimpleAgent.class);
        when(simPerson2.chooseNextLinkId()).thenReturn(null);
        var simVehicle1 = TestUtils.createVehicle("veh-1", simPerson1, 2, 1);
        var simVehicle2 = TestUtils.createVehicle("veh-2", simPerson1, 2, 1);
        var blockingVehicle = TestUtils.createVehicle("blocking-vehicle", simPerson2, 2, 1);

        simLink.pushVehicle(blockingVehicle, SimLink.LinkPosition.Buffer, 0);

		assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
        assertTrue(simLink.isOffering());

        wait2link.accept(simVehicle1, simLink);
        // add a second vehicle to assert the correct order.
        wait2link.accept(simVehicle2, simLink);
        // the vehicle can't go onto the link, as the buffer is blocked by another vehicle
        wait2link.doSimStep(0);
        verify(em, times(0)).processEvent(any());

        // free the link
        assertEquals(blockingVehicle.getId(), simLink.popVehicle().getId());
        simLink.doSimStep(null, 2);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 2));
        assertFalse(simLink.isOffering());

        // try again
        wait2link.doSimStep(2);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer, 2));
        assertTrue(simLink.isOffering());
        assertEquals(simVehicle1.getId(), simLink.popVehicle().getId());
        verify(em, times(1)).processEvent(any());

        simLink.doSimStep(null, 4);
        wait2link.doSimStep(4);
        assertEquals(simVehicle2.getId(), simLink.popVehicle().getId());
        verify(em, times(2)).processEvent(any());
    }

    @Test
    void vehicleIntoQueue() {

        var em = mock(EventsManager.class);
        var messaging = mock(SimStepMessaging.class);
        var activeLinks = new ActiveLinks(messaging);
        var wait2link = new Wait2Link(em, activeLinks);
        var link = TestUtils.createSingleLink(0, 0);
        link.setCapacity(3600);
        var simLink = TestUtils.createLink(link, 0, 1);
        var simPerson1 = mock(SimpleAgent.class);
        when(simPerson1.chooseNextLinkId()).thenReturn(null);
//        when(simPerson1.getCurrentLeg()).thenReturn(SimpleLeg.builder().setMode("some-mode").build());

        var simPerson2 = mock(SimpleAgent.class);
        when(simPerson2.chooseNextLinkId()).thenReturn(null);

        var simVehicle1 = TestUtils.createVehicle("veh-1", simPerson1, 2, 1);
        var simVehicle2 = TestUtils.createVehicle("veh-2", simPerson1, 2, 1);
        var blockingVehicle = TestUtils.createVehicle("blocking-vehicle", simPerson2, 100, 1);

        simLink.pushVehicle(blockingVehicle, SimLink.LinkPosition.QEnd, 0);

		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));

        wait2link.accept(simVehicle1, simLink);
        // add a second vehicle to assert the correct order.
        wait2link.accept(simVehicle2, simLink);
        // the vehicle can't go onto the link, as the queue is blocked by another vehicle
        wait2link.doSimStep(0);
        verify(em, times(0)).processEvent(any());

        // free the link
        simLink.doSimStep(null, 0);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
        assertTrue(simLink.isOffering());
        // this should put both vehicles onto the link
        wait2link.doSimStep(0);
        verify(em, times(2)).processEvent(any());

        var expectedIt = List.of(Id.createVehicleId("veh-2"), Id.createVehicleId("veh-1")).iterator();
        simLink.addLeaveHandler((v, _, _) -> {
            var expectedId = expectedIt.next();
            assertEquals(expectedId, v.getId());
            return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
        });
        assertFalse(simLink.doSimStep(null, 0));
    }

    @Test
    void vehiclesOntoMultipleLinks() {
        var em = mock(EventsManager.class);
        var messaging = mock(SimStepMessaging.class);
        var activeLinks = new ActiveLinks(messaging);
        var wait2link = new Wait2Link(em, activeLinks);
        var link1 = TestUtils.createSingleLink(0, 0);
        link1.setCapacity(3600);
        var link2 = TestUtils.createSingleLink(0, 0);
        link2.setCapacity(3600);
        var simLink1 = TestUtils.createLink(link1, 0, 1);
        var simLink2 = TestUtils.createLink(link2, 0, 1);
        var simPerson1 = mock(SimpleAgent.class);
        when(simPerson1.chooseNextLinkId()).thenReturn(Id.createLinkId("some-element"));
//        when(simPerson1.getCurrentLeg()).thenReturn(SimpleLeg.builder().setMode("some-mode").build());

        var simPerson2 = mock(SimpleAgent.class);
        when(simPerson2.chooseNextLinkId()).thenReturn(null);

        var simVehicle1 = TestUtils.createVehicle("veh-1", simPerson1, 2, 1);
		var simVehicle2 = TestUtils.createVehicle("veh-2", simPerson1, 2, 1);

        assertFalse(simLink1.isOffering());
        assertFalse(simLink2.isOffering());

        wait2link.accept(simVehicle1, simLink1);
        wait2link.accept(simVehicle2, simLink2);
        wait2link.doSimStep(0);

        assertEquals(simVehicle1.getId(), simLink1.popVehicle().getId());
        assertEquals(simVehicle2.getId(), simLink2.popVehicle().getId());
    }
}
