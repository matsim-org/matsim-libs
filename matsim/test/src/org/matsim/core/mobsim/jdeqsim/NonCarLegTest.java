package org.matsim.core.mobsim.jdeqsim;

import java.util.LinkedList;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class NonCarLegTest extends AbstractJDEQSimTest {

	public void test_EmptyCarRoute() {
		ScenarioImpl scenario = new ScenarioLoaderImpl("test/input/org/matsim/core/mobsim/jdeqsim/config2.xml").loadScenario();
		this.runJDEQSim(scenario);
		
		// at least one event
		assertTrue(eventsByPerson.size() > 0);
		
		// super.checkAscendingTimeStamps: intentionally not executed, because of -infinity time step
		checkEventsCorrespondToPlans(scenario.getPopulation());
		
		// custom checks:
		// problem in that method (and not required).
		boolean wasInLoop=false;
		
		for (LinkedList<PersonEvent> list : super.eventsByPerson.values()) {
			wasInLoop=true;
			assertTrue(list.get(0) instanceof ActivityEndEventImpl);
			assertTrue(list.get(1) instanceof AgentDepartureEventImpl);
			assertTrue(list.get(2) instanceof AgentArrivalEventImpl);
			assertTrue(list.get(3) instanceof ActivityStartEventImpl);
			
		}
		assertTrue(wasInLoop);
	}

}
