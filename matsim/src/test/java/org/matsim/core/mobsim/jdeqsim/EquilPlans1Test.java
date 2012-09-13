package org.matsim.core.mobsim.jdeqsim;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class EquilPlans1Test extends AbstractJDEQSimTest {

	public void test_EmptyCarRoute() {
		Scenario scenario = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed("test/scenarios/equil/config_plans1.xml").loadScenario();
		this.runJDEQSim(scenario);
		
		assertEquals(1, eventsByPerson.size());
		
		super.checkAscendingTimeStamps();
		super.checkEventsCorrespondToPlans(scenario.getPopulation());
		
		// custom checks:
		boolean wasInLoop = false;
		int index = 0;
		for (List<Event> list : super.eventsByPerson.values()) {
			wasInLoop = true;
			// checking the time of the first event
			assertEquals(21600, list.get(index).getTime(), 0.9);
			assertTrue(list.get(index++) instanceof ActivityEndEvent);
			assertTrue(list.get(index++) instanceof AgentDepartureEvent);
			assertTrue(list.get(index++) instanceof AgentWait2LinkEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof AgentArrivalEvent);
			assertTrue(list.get(index++) instanceof ActivityStartEvent);
			assertTrue(list.get(index++) instanceof ActivityEndEvent);
			assertTrue(list.get(index++) instanceof AgentDepartureEvent);
			assertTrue(list.get(index++) instanceof AgentArrivalEvent);
			assertTrue(list.get(index++) instanceof ActivityStartEvent);
			assertTrue(list.get(index++) instanceof ActivityEndEvent);
			assertTrue(list.get(index++) instanceof AgentDepartureEvent);
			assertTrue(list.get(index++) instanceof AgentWait2LinkEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof AgentArrivalEvent);
			assertTrue(list.get(index) instanceof ActivityStartEvent);
			// checking the time of the last event
			assertEquals(38039, list.get(index).getTime(), 0.9);
		}

		assertTrue(wasInLoop);
	}

}
