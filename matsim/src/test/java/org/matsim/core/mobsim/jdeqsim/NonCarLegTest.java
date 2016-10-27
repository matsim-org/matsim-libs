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

import static org.junit.Assert.assertTrue;

public class NonCarLegTest extends AbstractJDEQSimTest {

	@Test
	public void test_EmptyCarRoute() {
		Config config = utils.loadConfig(IOUtils.newUrl(utils.packageInputResourcePath(), "config2.xml"));
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
