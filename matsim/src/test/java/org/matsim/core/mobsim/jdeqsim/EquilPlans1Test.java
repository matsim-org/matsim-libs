package org.matsim.core.mobsim.jdeqsim;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

public class EquilPlans1Test extends AbstractJDEQSimTest {

	public void test_EmptyCarRoute() {
		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config_plans1.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);

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
			assertTrue(list.get(index++) instanceof PersonDepartureEvent);
			assertTrue(list.get(index++) instanceof Wait2LinkEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof PersonArrivalEvent);
			assertTrue(list.get(index++) instanceof ActivityStartEvent);
			assertTrue(list.get(index++) instanceof ActivityEndEvent);
			assertTrue(list.get(index++) instanceof PersonDepartureEvent);
			assertTrue(list.get(index++) instanceof PersonArrivalEvent);
			assertTrue(list.get(index++) instanceof ActivityStartEvent);
			assertTrue(list.get(index++) instanceof ActivityEndEvent);
			assertTrue(list.get(index++) instanceof PersonDepartureEvent);
			assertTrue(list.get(index++) instanceof Wait2LinkEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof PersonArrivalEvent);
			assertTrue(list.get(index) instanceof ActivityStartEvent);
			// checking the time of the last event
			assertEquals(38039, list.get(index).getTime(), 0.9);
		}

		assertTrue(wasInLoop);
	}

}
