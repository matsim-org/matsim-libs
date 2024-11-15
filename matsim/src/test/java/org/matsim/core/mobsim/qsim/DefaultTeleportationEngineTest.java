package org.matsim.core.mobsim.qsim;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentMessage;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

class DefaultTeleportationEngineTest {

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

	@Test
	public void singlePersonLocal() {

		var simPerson = createPerson();
		var em = mock(EventsManager.class);
		var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		var engine = new DefaultTeleportationEngine(scenario, em, false);
		var internalInterface = mock(InternalInterface.class);
		engine.setInternalInterface(internalInterface);

		engine.handleDeparture(10, simPerson, Id.createLinkId("l1"));
		// this should not fetch the person from the teleportation queue
		engine.doSimStep(10 + 41);
		verify(internalInterface, never()).arrangeNextAgentState(any());
		// this should fetch the person and call 'nextStateHandler' above
		engine.doSimStep(10 + 42);
		verify(internalInterface, times(1)).arrangeNextAgentState(assertArg(agent -> assertEquals(simPerson.getId(), agent.getId())));

		var inOrder = inOrder(em);
		inOrder.verify(em).processEvent(any(TeleportationArrivalEvent.class));
		inOrder.verify(em).processEvent(any(PersonArrivalEvent.class));

		verify(em, times(1)).processEvent(assertArg(e -> assertEquals(10 + 42, e.getTime())));
	}

}
