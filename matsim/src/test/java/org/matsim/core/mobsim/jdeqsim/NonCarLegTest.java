package org.matsim.core.mobsim.jdeqsim;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class NonCarLegTest extends AbstractJDEQSimTest {

	public void test_EmptyCarRoute() {
		Scenario scenario = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed("test/input/org/matsim/core/mobsim/jdeqsim/config2.xml").loadScenario();
		this.runJDEQSim(scenario);
		
		// at least one event
		assertTrue(eventsByPerson.size() > 0);
		
		// super.checkAscendingTimeStamps: intentionally not executed, because of -infinity time step
		checkEventsCorrespondToPlans(scenario.getPopulation());
		
		// custom checks:
		// problem in that method (and not required).
		boolean wasInLoop=false;
		
		for (List<Event> list : super.eventsByPerson.values()) {
			wasInLoop=true;
			assertTrue(list.get(0) instanceof ActivityEndEvent);
			assertTrue(list.get(1) instanceof AgentDepartureEvent);
			assertTrue(list.get(2) instanceof AgentArrivalEvent);
			assertTrue(list.get(3) instanceof ActivityStartEvent);
			
		}
		assertTrue(wasInLoop);
	}

}
