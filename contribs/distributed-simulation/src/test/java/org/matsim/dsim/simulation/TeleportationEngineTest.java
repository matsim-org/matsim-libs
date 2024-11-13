package org.matsim.dsim.simulation;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.dsim.QSimCompatibility;
import org.matsim.dsim.messages.PersonMsg;
import org.matsim.dsim.messages.SimStepMessage;
import org.matsim.dsim.messages.Teleportation;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeleportationEngineTest {

	private static PersonMsg createPerson() {
		return PersonMsg.builder()
			.setId(Id.createPersonId(0))
			.setPlan(List.of(
				PopulationUtils.createActivityFromCoord("some", new Coord(0, 0)),
				SimpleLeg.builder()
					.setMode("car")
					.setRoutingMode("car")
					.setDepartureTime(OptionalTime.defined(10))
					.setTravelTime(OptionalTime.defined(42))
					.setRoute(createRoute("l1", "l2", -1))
					.build()
			))
			.setCurrentPlanElement(1)
			.build();
	}

	private static Route createRoute(String startLinkId, String endLinkId, double distance) {
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId(startLinkId), Id.createLinkId(endLinkId));
		route.setDistance(distance);
		return route;
	}

	@Test
	public void singlePersonLocal() {

		var simPerson = new SimPerson(createPerson());
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var em = mock(EventsManager.class);
		var engine = new TeleportationEngine(em, messaging, mock(QSimCompatibility.class));
		var internalInterface = mock(InternalInterface.class);
		engine.setInternalInterface(internalInterface);
//		engine.setNextStateHandler((person, now) -> {
//			assertEquals(10 + 42, now);
//			assertEquals(simPerson.getId(), person.getId());
//		});

		engine.accept(simPerson, 10);
		// this should not fetch the person from the teleportation queue
		engine.doSimStep(10 + 41);
		verify(internalInterface, never()).arrangeNextAgentState(any());
		// this should fetch the person and call 'nextStateHandler' above
		engine.doSimStep(10 + 42);
		verify(internalInterface, times(1)).arrangeNextAgentState(assertArg(agent -> assertEquals(simPerson.getId(), agent.getId())));

		var inOrder = inOrder(em);
		inOrder.verify(em).processEvent(any(TeleportationArrivalEvent.class));
		inOrder.verify(em).processEvent(any(PersonArrivalEvent.class));
		verify(em, times(2)).processEvent(assertArg(e -> assertEquals(10 + 42, e.getTime())));
	}

	@Test
	public void singlePersonLocalNoTravelTime() {
		var person = new SimPerson(PersonMsg.builder()
			.setId(Id.createPersonId("test-person"))
			.setPlan(List.of(
				PopulationUtils.createActivityFromCoord("start", new Coord(0, 0)),
				SimpleLeg.builder()
					.setMode("test-mode")
					.setTravelTime(OptionalTime.undefined())
					.setDepartureTime(OptionalTime.defined(10))
					.setRoute(RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("start-link"), Id.createLinkId("end-link")))
					.build(),
				PopulationUtils.createActivityFromCoord("end", new Coord(300, 400))
			))
			.setCurrentPlanElement(1)
			.build());
		var em = mock(EventsManager.class);
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var config = ConfigUtils.createConfig();
		config.routing().addTeleportedModeParams(new RoutingConfigGroup.TeleportedModeParams()
			.setMode("test-mode")
			.setBeelineDistanceFactor(3.145)
			.setTeleportedModeSpeed(100.));
		var engine = new TeleportationEngine(em, messaging, config);
		var startTime = 0;
		engine.setNextStateHandler((finishedPerson, time) -> {
			assertEquals(Math.round(startTime + 500 * 3.145 / 100.), time);
			assertEquals(person.getId(), finishedPerson.getId());
		});
		engine.accept(person, startTime);

		// iterate some simulation steps
		for (var now = startTime; now < 1000; now += 1) {
			engine.doSimStep(now);
		}

		var inOrder = inOrder(em);
		inOrder.verify(em).processEvent(any(TeleportationArrivalEvent.class));
		inOrder.verify(em).processEvent(any(PersonArrivalEvent.class));
	}

	@Test
	public void multiPersonLocal() {

		PersonMsg person1 = createPerson();

		List<PlanElement> plan2 = new ArrayList<>(person1.getPlan());
		plan2.add(1, ((SimpleLeg) plan2.get(1)).toBuilder()
			.setRoute(createRoute("l1", "l3", -1))
			.setTravelTime(OptionalTime.defined(7)).build());

		PersonMsg person2 = person1.toBuilder()
			.setPlan(plan2)
			.build();

		var simPerson1 = new SimPerson(person1);
		var simPerson2 = new SimPerson(person2);

		var messaging = Mockito.mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var engine = new TeleportationEngine(Mockito.mock(EventsManager.class), messaging, ConfigUtils.createConfig());

		engine.accept(simPerson1, 10);
		engine.accept(simPerson2, 10);
		// use a timestep where both persons could be ready. person2 should finish first,
		// as it has a shorter travel time
		engine.setNextStateHandler((person, now) -> {
			assertEquals(10 + 42, now);
			assertEquals(simPerson2.getId(), person.getId());
		});
		engine.doSimStep(10 + 42);

		engine.setNextStateHandler((person, now) -> {
			assertEquals(10 + 42, now);
			assertEquals(simPerson1.getId(), person.getId());
		});
		engine.doSimStep(10 + 42);
	}

	@Test
	public void sendPersonWithSplitLeg() {
		var simPerson = new SimPerson(createPerson());

		var messaging = Mockito.mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(false);
		var engine = new TeleportationEngine(Mockito.mock(EventsManager.class), messaging, ConfigUtils.createConfig());

		engine.accept(simPerson, 11);
		verify(messaging).collectTeleportation(argThat(person -> person.getId().equals(simPerson.getId())), eq((double) 11 + 42));
	}

	@Test
	public void receivePersonWithSplitLeg() {

		var simPerson = new SimPerson(createPerson());
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var em = mock(EventsManager.class);
		var engine = new TeleportationEngine(em, messaging, ConfigUtils.createConfig());
		engine.setNextStateHandler((_, now) -> assertEquals(42, now));

		SimStepMessage message = SimStepMessage.builder()
			.setTeleportationMsg(Teleportation.builder().setPersonMessage(simPerson.toMessage()).setExitTime(42).build())
			.build();

		engine.process(message, 20);

		for (var i = 20; i < 100; i++) {
			engine.doSimStep(i);
		}

		var inOrder = inOrder(em);
		inOrder.verify(em).processEvent(any(TeleportationArrivalEvent.class));
		inOrder.verify(em).processEvent(any(PersonArrivalEvent.class));
		verify(em, times(2)).processEvent(assertArg(e -> assertEquals(42, e.getTime())));
	}

	@Test
	void receivePersonTooLate() {
		var simPerson = new SimPerson(createPerson());
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var em = mock(EventsManager.class);
		var engine = new TeleportationEngine(em, messaging, ConfigUtils.createConfig());
		var message = SimStepMessage.builder()
			.setTeleportationMsg(Teleportation.builder().setPersonMessage(simPerson.toMessage()).setExitTime(42).build())
			.build();

		assertThrows(IllegalStateException.class, () -> engine.process(message, 100));
	}
}
