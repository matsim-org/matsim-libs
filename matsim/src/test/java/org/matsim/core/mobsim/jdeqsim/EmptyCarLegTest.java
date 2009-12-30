package org.matsim.core.mobsim.jdeqsim;

import java.util.LinkedList;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class EmptyCarLegTest extends AbstractJDEQSimTest {

	public void test_EmptyCarRoute() {
		ScenarioImpl scenario = new ScenarioLoaderImpl("test/input/org/matsim/core/mobsim/jdeqsim/config1.xml").loadScenario();
		this.runJDEQSim(scenario);
		
		// at least one event
		assertTrue(eventsByPerson.size() > 0);
		
		checkAscendingTimeStamps();
		checkEventsCorrespondToPlans(scenario.getPopulation());
		
		// custom checks:
		boolean wasInLoop = false;

		for (LinkedList<PersonEvent> list : this.eventsByPerson.values()) {
			wasInLoop = true;
			// departure and arrival time is the same
			// empty car leg or mode!=car
			assertEquals(21600.0, list.get(0).getTime());
			assertTrue(list.get(0) instanceof ActivityEndEventImpl);
			assertTrue(list.get(1) instanceof AgentDepartureEventImpl);
			assertEquals(21600.0, list.get(1).getTime());
			assertTrue(list.get(2) instanceof AgentArrivalEventImpl);
			assertEquals(21600.0, list.get(2).getTime());
			assertTrue(list.get(3) instanceof ActivityStartEventImpl);
			assertEquals(21600.0, list.get(3).getTime());
		}
		assertTrue(wasInLoop);
	}

}
