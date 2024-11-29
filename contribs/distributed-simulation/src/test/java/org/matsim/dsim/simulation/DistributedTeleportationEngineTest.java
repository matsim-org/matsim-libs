package org.matsim.dsim.simulation;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.dsim.Teleportation;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DistributedTeleportationEngineTest {

	private static BasicPlanAgentImpl createPerson(String id, EventsManager em) {

		var config = ConfigUtils.createConfig();
		var scenario = ScenarioUtils.createScenario(config);
		var timer = new MobsimTimer();
		var timeInterpretation = TimeInterpretation.create(config);
		var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(id));
		scenario.getPopulation().addPerson(person);
		var plan = PopulationUtils.createPlan(person);
		var leg = PopulationUtils.createLeg("car");
		leg.setRoutingMode("car");
		leg.setDepartureTime(10);
		leg.setTravelTime(42);
		leg.setRoute(new GenericRouteImpl(Id.createLinkId("l1"), Id.createLinkId("l2")));
		plan.addLeg(leg);
		var msg = new BasicPlanAgentImpl.BasicPlanAgentMessage(person.getId(), plan.getPlanElements(), 0, 0, MobsimAgent.State.ACTIVITY, Id.createLinkId("bla"), 0);

		return new BasicPlanAgentImpl(msg, scenario, em, timer, timeInterpretation);
	}

	@Test
	public void singlePersonLocal() {

		var em = mock(EventsManager.class);
		var simPerson = createPerson("some", em);
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var engine = new DistributedTeleportationEngine(em, messaging, mock(AgentSourcesContainer.class));
		var internalInterface = mock(InternalInterface.class);
		engine.setInternalInterface(internalInterface);

		engine.handleDeparture(10, simPerson, simPerson.getCurrentLinkId());
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

		var em = mock(EventsManager.class);
		var agent1 = createPerson("one", em);
		var agent2 = createPerson("two", em);
		var leg2 = (Leg) agent2.getCurrentPlan().getPlanElements().getFirst();
		leg2.setTravelTime(31);

		var messaging = Mockito.mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var internalInterface = mock(InternalInterface.class);
		var engine = new DistributedTeleportationEngine(em, messaging, mock(AgentSourcesContainer.class));
		engine.setInternalInterface(internalInterface);

		engine.handleDeparture(10, agent1, agent1.getCurrentLinkId());
		engine.handleDeparture(10, agent2, agent2.getCurrentLinkId());

		// use a timestep where both persons could be ready. person2 should finish first,
		// as it has a shorter travel time
		engine.doSimStep(10 + 42);
		var inOrder = inOrder(internalInterface);
		inOrder.verify(internalInterface).arrangeNextAgentState(eq(agent2));
		inOrder.verify(internalInterface).arrangeNextAgentState(eq(agent1));

		// check that the engine is empty
		engine.doSimStep(10 + 100);
		verify(internalInterface, times(2)).arrangeNextAgentState(any());
	}

	@Test
	public void sendPersonWithSplitLeg() {
		var em = mock(EventsManager.class);
		var agent = createPerson("some", em);
		var messaging = Mockito.mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(false);
		var engine = new DistributedTeleportationEngine(em, messaging, mock(AgentSourcesContainer.class));

		engine.handleDeparture(11, agent, agent.getCurrentLinkId());
		verify(messaging).collectTeleportation(eq(agent), eq(11 + 42.));
	}

	@Test
	public void receivePersonWithSplitLeg() {

		var em = mock(EventsManager.class);
		var agent = createPerson("some", em);
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var asc = mock(AgentSourcesContainer.class);
		when(asc.agentFromMessage(any(), any())).thenReturn(agent);
		var engine = new DistributedTeleportationEngine(em, messaging, asc);

		engine.setInternalInterface(mock(InternalInterface.class));

		SimStepMessage message = SimStepMessage.builder()
			.addTeleportation(new Teleportation(
				agent.getClass(), agent.toMessage(), 42
			))
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
		var em = mock(EventsManager.class);
		var agent = createPerson("some", em);
		var messaging = mock(SimStepMessaging.class);
		when(messaging.isLocal(any())).thenReturn(true);
		var asc = mock(AgentSourcesContainer.class);
		when(asc.agentFromMessage(any(), any())).thenReturn(agent);
		var engine = new DistributedTeleportationEngine(em, messaging, asc);
		var message = SimStepMessage.builder()
			.addTeleportation(new Teleportation(
				agent.getClass(), agent.toMessage(), 42
			))
			.build();

		assertThrows(IllegalStateException.class, () -> engine.process(message, 100));
	}
}
