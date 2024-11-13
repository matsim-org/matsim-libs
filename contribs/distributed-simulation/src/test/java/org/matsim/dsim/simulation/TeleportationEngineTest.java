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
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentMessage;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeInterpretationModule;
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

	private static BasicPlanAgentImpl createPerson() {

		var config = ConfigUtils.createConfig();
		var scenario = ScenarioUtils.createScenario(config);
		var em = mock(EventsManager.class);
		var timer = new MobsimTimer();
		var timeInterpretation = TimeInterpretation.create(config);
		var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(0));
		var plan = PopulationUtils.createPlan(person);
		var leg = PopulationUtils.createLeg("car");
		leg.setRoutingMode("car");
		leg.setDepartureTime(10);
		leg.setTravelTime(42);
		leg.setRoute(new GenericRouteImpl(Id.createLinkId("l1"), Id.createLinkId("l2")));
		plan.addLeg(leg);
		var msg = new BasicPlanAgentMessage(plan, 0, 0, MobsimAgent.State.ACTIVITY, Id.createLinkId("bla"), 0);

		return new BasicPlanAgentImpl(msg, scenario, em, timer, timeInterpretation);
	}

	private static Route createRoute(String startLinkId, String endLinkId, double distance) {
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId(startLinkId), Id.createLinkId(endLinkId));
		route.setDistance(distance);
		return route;
	}

	@Test
	public void singlePersonLocal() {

		var simPerson = createPerson();
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var em = mock(EventsManager.class);
		var engine = new TeleportationEngine(em, messaging, mock(QSimCompatibility.class));
		var internalInterface = mock(InternalInterface.class);
		engine.setInternalInterface(internalInterface);

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
	public void multiPersonLocal() {

		var agent1 = mock(DistributedMobsimAgent.class);
		when(agent1.getExpectedTravelTime()).thenReturn(OptionalTime.defined(42));
		var agent2 = mock(DistributedMobsimAgent.class);
		when(agent2.getExpectedTravelTime()).thenReturn(OptionalTime.defined(31));

		var messaging = Mockito.mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var internalInterface = mock(InternalInterface.class);
		var engine = new TeleportationEngine(Mockito.mock(EventsManager.class), messaging, mock(QSimCompatibility.class));
		engine.setInternalInterface(internalInterface);

		engine.accept(agent1, 10);
		engine.accept(agent2, 10);

		// use a timestep where both persons could be ready. person2 should finish first,
		// as it has a shorter travel time
		engine.doSimStep(10 + 42);
		verify(internalInterface).arrangeNextAgentState(assertArg(agent -> assertEquals(agent1.getId(), agent.getId())));
		engine.doSimStep(10 + 42);
		verify(internalInterface).arrangeNextAgentState(assertArg(agent -> assertEquals(agent2.getId(), agent.getId())));
		// check that the engine is empty
		engine.doSimStep(10 + 100);
		verify(internalInterface, times(2));
	}

	@Test
	public void sendPersonWithSplitLeg() {
		var agent = mock(DistributedMobsimAgent.class);
		var messaging = Mockito.mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(false);
		var engine = new TeleportationEngine(Mockito.mock(EventsManager.class), messaging, mock(QSimCompatibility.class));

		engine.accept(agent, 11);
		verify(messaging).collectTeleportation(assertArg(a -> assertEquals(agent.getId(), a.getId())), assertArg(now -> assertEquals(11 + 42, now)));
	}

	@Test
	public void receivePersonWithSplitLeg() {

		var agent = createPerson();
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var em = mock(EventsManager.class);
		var qsimCompatibility = mock(QSimCompatibility.class);
		when(qsimCompatibility.agentFromMessage(any())).thenReturn(agent);
		var engine = new TeleportationEngine(em, messaging, mock(QSimCompatibility.class));

		SimStepMessage message = SimStepMessage.builder()
			.setTeleportationMsg(Teleportation.builder()
				.setPersonMessage(agent.toMessage())
				.setExitTime(42)
				.build())
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
		var agent = createPerson();
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var em = mock(EventsManager.class);
		var qsimCompatibility = mock(QSimCompatibility.class);
		when(qsimCompatibility.agentFromMessage(any())).thenReturn(agent);
		var engine = new TeleportationEngine(em, messaging, qsimCompatibility);
		var message = SimStepMessage.builder()
			.setTeleportationMsg(Teleportation.builder()
				.setPersonMessage(agent.toMessage())
				.setExitTime(42)
				.build())
			.build();

		assertThrows(IllegalStateException.class, () -> engine.process(message, 100));
	}
}
