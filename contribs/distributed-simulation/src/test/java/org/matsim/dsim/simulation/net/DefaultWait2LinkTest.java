package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.vehicles.Vehicle;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class DefaultWait2LinkTest {

	private EventsManager eventsManager;
	private DefaultWait2Link wait2Link;

	@BeforeEach
	public void setUp() {
		eventsManager = mock(EventsManager.class);
		wait2Link = new DefaultWait2Link(eventsManager);
	}

	@Test
	public void testMoveWaitingCapacity() {
		DistributedMobsimVehicle vehicle = mock(DistributedMobsimVehicle.class);
		SimLink link = mock(SimLink.class);
		MobsimDriverAgent driver = mock(MobsimDriverAgent.class);
		Id<Link> linkId = Id.createLinkId("link");
		Id<Vehicle> vehicleId = Id.createVehicleId("veh");
		Id<Person> personId = Id.createPersonId("person");

		when(vehicle.getDriver()).thenReturn(driver);
		when(vehicle.getId()).thenReturn(vehicleId);
		when(link.getId()).thenReturn(linkId);
		when(driver.getId()).thenReturn(personId);
		when(driver.getMode()).thenReturn("car");

		// Case 1: Link does not accept vehicle
		when(link.isAccepting(any(), anyDouble())).thenReturn(false);
		wait2Link.accept(vehicle, link, 0.0);
		wait2Link.moveWaiting(1.0);

		verify(link, never()).pushVehicle(eq(vehicle), any(), eq(1.0));
		verify(eventsManager, never()).processEvent(any(VehicleEntersTrafficEvent.class));

		// Case 2: Link accepts vehicle
		when(link.isAccepting(any(), anyDouble())).thenReturn(true);
		wait2Link.moveWaiting(2.0);

		verify(link, times(1)).pushVehicle(eq(vehicle), any(), eq(2.0));
		verify(eventsManager, times(1)).processEvent(any(VehicleEntersTrafficEvent.class));
	}

	@Test
	public void testMoveWaitingCorrectLinks() {
		DistributedMobsimVehicle veh1 = mock(DistributedMobsimVehicle.class);
		DistributedMobsimVehicle veh2 = mock(DistributedMobsimVehicle.class);
		SimLink link1 = mock(SimLink.class);
		SimLink link2 = mock(SimLink.class);
		MobsimDriverAgent driver1 = mock(MobsimDriverAgent.class);
		MobsimDriverAgent driver2 = mock(MobsimDriverAgent.class);

		Id<Link> id1 = Id.createLinkId("link1");
		Id<Link> id2 = Id.createLinkId("link2");
		when(link1.getId()).thenReturn(id1);
		when(link2.getId()).thenReturn(id2);
		when(veh1.getDriver()).thenReturn(driver1);
		when(veh2.getDriver()).thenReturn(driver2);
		when(veh1.getId()).thenReturn(Id.createVehicleId("veh1"));
		when(veh2.getId()).thenReturn(Id.createVehicleId("veh2"));
		when(driver1.getId()).thenReturn(Id.createPersonId("p1"));
		when(driver2.getId()).thenReturn(Id.createPersonId("p2"));
		when(driver1.getMode()).thenReturn("car");
		when(driver2.getMode()).thenReturn("car");

		when(link1.isAccepting(any(), anyDouble())).thenReturn(true);
		when(link2.isAccepting(any(), anyDouble())).thenReturn(true);

		wait2Link.accept(veh1, link1, 0.0);
		wait2Link.accept(veh2, link2, 0.0);

		wait2Link.moveWaiting(1.0);

		verify(link1).pushVehicle(eq(veh1), any(), eq(1.0));
		verify(link2).pushVehicle(eq(veh2), any(), eq(1.0));
	}

	@Test
	public void testMoveWaitingFIFO() {
		DistributedMobsimVehicle veh1 = mock(DistributedMobsimVehicle.class);
		DistributedMobsimVehicle veh2 = mock(DistributedMobsimVehicle.class);
		SimLink link = mock(SimLink.class);
		MobsimDriverAgent driver1 = mock(MobsimDriverAgent.class);
		MobsimDriverAgent driver2 = mock(MobsimDriverAgent.class);

		Id<Link> linkId = Id.createLinkId("link");
		when(link.getId()).thenReturn(linkId);
		when(veh1.getDriver()).thenReturn(driver1);
		when(veh2.getDriver()).thenReturn(driver2);
		when(veh1.getId()).thenReturn(Id.createVehicleId("veh1"));
		when(veh2.getId()).thenReturn(Id.createVehicleId("veh2"));
		when(driver1.getId()).thenReturn(Id.createPersonId("p1"));
		when(driver2.getId()).thenReturn(Id.createPersonId("p2"));
		when(driver1.getMode()).thenReturn("car");
		when(driver2.getMode()).thenReturn("car");

		when(link.isAccepting(any(), anyDouble())).thenReturn(true);

		wait2Link.accept(veh1, link, 0.0);
		wait2Link.accept(veh2, link, 0.0);

		wait2Link.moveWaiting(1.0);

		ArgumentCaptor<DistributedMobsimVehicle> captor = ArgumentCaptor.forClass(DistributedMobsimVehicle.class);
		verify(link, times(2)).pushVehicle(captor.capture(), any(), eq(1.0));

		assertEquals(veh1, captor.getAllValues().get(0));
		assertEquals(veh2, captor.getAllValues().get(1));
	}

	@Test
	public void testAfterSimStuckEvents() {
		DistributedMobsimVehicle veh1 = mock(DistributedMobsimVehicle.class);
		DistributedMobsimVehicle veh2 = mock(DistributedMobsimVehicle.class);
		DistributedMobsimVehicle veh3 = mock(DistributedMobsimVehicle.class);
		SimLink link = mock(SimLink.class);
		MobsimDriverAgent driver1 = mock(MobsimDriverAgent.class);
		MobsimDriverAgent driver2 = mock(MobsimDriverAgent.class);
		MobsimDriverAgent driver3 = mock(MobsimDriverAgent.class);

		when(link.getId()).thenReturn(Id.createLinkId("link"));
		when(veh1.getDriver()).thenReturn(driver1);
		when(veh2.getDriver()).thenReturn(driver2);
		when(veh3.getDriver()).thenReturn(driver3);
		when(veh1.getId()).thenReturn(Id.createVehicleId("veh1"));
		when(veh2.getId()).thenReturn(Id.createVehicleId("veh2"));
		when(veh3.getId()).thenReturn(Id.createVehicleId("veh3"));
		when(driver1.getId()).thenReturn(Id.createPersonId("p1"));
		when(driver2.getId()).thenReturn(Id.createPersonId("p2"));
		when(driver3.getId()).thenReturn(Id.createPersonId("p3"));
		when(driver1.getMode()).thenReturn("car");
		when(driver2.getMode()).thenReturn("car");
		when(driver3.getMode()).thenReturn("car");

		// Only one vehicle can move
		when(link.isAccepting(any(), anyDouble())).thenReturn(true).thenReturn(false);

		wait2Link.accept(veh1, link, 0.0);
		wait2Link.accept(veh2, link, 0.0);
		wait2Link.accept(veh3, link, 0.0);

		wait2Link.moveWaiting(10.0);

		verify(link, times(1)).pushVehicle(eq(veh1), any(), eq(10.0));
		verify(link, never()).pushVehicle(eq(veh2), any(), anyDouble());
		verify(link, never()).pushVehicle(eq(veh3), any(), anyDouble());

		wait2Link.afterSim();

		ArgumentCaptor<PersonStuckEvent> captor = ArgumentCaptor.forClass(PersonStuckEvent.class);
		verify(eventsManager, times(2)).processEvent(captor.capture());

		assertEquals(Id.createPersonId("p2"), captor.getAllValues().get(0).getPersonId());
		assertEquals(Id.createPersonId("p3"), captor.getAllValues().get(1).getPersonId());
		assertEquals(10.0, captor.getAllValues().get(0).getTime());
	}
}
