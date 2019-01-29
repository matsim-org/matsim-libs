package org.matsim.core.mobsim.jdeqsim;

import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EmptyCarLegTest extends AbstractJDEQSimTest {

	@Test
	public void test_EmptyCarRoute() {
		
		Config config = utils.loadConfig(IOUtils.newUrl(utils.packageInputResourcePath(), "config1.xml"));
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
			assertEquals(21600.0, list.get(0).getTime(), 0.0);
			assertTrue(list.get(0) instanceof ActivityEndEvent);
			assertTrue(list.get(1) instanceof PersonDepartureEvent);
			assertEquals(21600.0, list.get(1).getTime(), 0.0);
			assertTrue(list.get(2) instanceof PersonArrivalEvent);
			assertEquals(21600.0, list.get(2).getTime(), 0.0);
			assertTrue(list.get(3) instanceof ActivityStartEvent);
			assertEquals(21600.0, list.get(3).getTime(), 0.0);
		}
		assertTrue(wasInLoop);
	}

}
