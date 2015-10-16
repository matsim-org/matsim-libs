package org.matsim.core.mobsim.jdeqsim;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

public class EmptyCarLegTest extends AbstractJDEQSimTest {

	public void test_EmptyCarRoute() {
		
		Config config = ConfigUtils.loadConfig("test/input/org/matsim/core/mobsim/jdeqsim/config1.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
		this.runJDEQSim(scenario);
		
		// at least one event
		assertTrue(eventsByPerson.size() > 0);
		
		checkAscendingTimeStamps();
		checkEventsCorrespondToPlans(scenario.getPopulation());
		
		// custom checks:
		boolean wasInLoop = false;

		for (List<Event> list : this.eventsByPerson.values()) {
			wasInLoop = true;
			// departure and arrival time is the same
			// empty car leg or mode!=car
			assertEquals(21600.0, list.get(0).getTime());
			assertTrue(list.get(0) instanceof ActivityEndEvent);
			assertTrue(list.get(1) instanceof PersonDepartureEvent);
			assertEquals(21600.0, list.get(1).getTime());
			assertTrue(list.get(2) instanceof PersonArrivalEvent);
			assertEquals(21600.0, list.get(2).getTime());
			assertTrue(list.get(3) instanceof ActivityStartEvent);
			assertEquals(21600.0, list.get(3).getTime());
		}
		assertTrue(wasInLoop);
	}

}
