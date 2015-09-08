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

public class NonCarLegTest extends AbstractJDEQSimTest {

	public void test_EmptyCarRoute() {
		Config config = ConfigUtils.loadConfig("test/input/org/matsim/core/mobsim/jdeqsim/config2.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);

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
			assertTrue(list.get(1) instanceof PersonDepartureEvent);
			assertTrue(list.get(2) instanceof PersonArrivalEvent);
			assertTrue(list.get(3) instanceof ActivityStartEvent);
			
		}
		assertTrue(wasInLoop);
	}

}
