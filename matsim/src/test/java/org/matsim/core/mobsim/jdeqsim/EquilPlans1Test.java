package org.matsim.core.mobsim.jdeqsim;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class EquilPlans1Test extends AbstractJDEQSimTest {

	public void test_EmptyCarRoute() {
		Scenario scenario = new ScenarioLoaderImpl("test/scenarios/equil/config_plans1.xml").loadScenario();
		this.runJDEQSim(scenario);
		
		assertEquals(1, eventsByPerson.size());
		
		super.checkAscendingTimeStamps();
		super.checkEventsCorrespondToPlans(scenario.getPopulation());
		
		// custom checks:
		boolean wasInLoop = false;
		int index = 0;
		for (LinkedList<PersonEvent> list : super.eventsByPerson.values()) {
			wasInLoop = true;
			// checking the time of the first event
			assertEquals(21600, list.get(index).getTime(), 0.9);
			assertTrue(list.get(index++) instanceof ActivityEndEventImpl);
			assertTrue(list.get(index++) instanceof AgentDepartureEventImpl);
			assertTrue(list.get(index++) instanceof AgentWait2LinkEventImpl);
			assertTrue(list.get(index++) instanceof LinkLeaveEventImpl);
			assertTrue(list.get(index++) instanceof LinkEnterEventImpl);
			assertTrue(list.get(index++) instanceof LinkLeaveEventImpl);
			assertTrue(list.get(index++) instanceof LinkEnterEventImpl);
			assertTrue(list.get(index++) instanceof LinkLeaveEventImpl);
			assertTrue(list.get(index++) instanceof LinkEnterEventImpl);
			assertTrue(list.get(index++) instanceof AgentArrivalEventImpl);
			assertTrue(list.get(index++) instanceof ActivityStartEventImpl);
			assertTrue(list.get(index++) instanceof ActivityEndEventImpl);
			assertTrue(list.get(index++) instanceof AgentDepartureEventImpl);
			assertTrue(list.get(index++) instanceof AgentArrivalEventImpl);
			assertTrue(list.get(index++) instanceof ActivityStartEventImpl);
			assertTrue(list.get(index++) instanceof ActivityEndEventImpl);
			assertTrue(list.get(index++) instanceof AgentDepartureEventImpl);
			assertTrue(list.get(index++) instanceof AgentWait2LinkEventImpl);
			assertTrue(list.get(index++) instanceof LinkLeaveEventImpl);
			assertTrue(list.get(index++) instanceof LinkEnterEventImpl);
			assertTrue(list.get(index++) instanceof LinkLeaveEventImpl);
			assertTrue(list.get(index++) instanceof LinkEnterEventImpl);
			assertTrue(list.get(index++) instanceof LinkLeaveEventImpl);
			assertTrue(list.get(index++) instanceof LinkEnterEventImpl);
			assertTrue(list.get(index++) instanceof LinkLeaveEventImpl);
			assertTrue(list.get(index++) instanceof LinkEnterEventImpl);
			assertTrue(list.get(index++) instanceof AgentArrivalEventImpl);
			assertTrue(list.get(index) instanceof ActivityStartEventImpl);
			// checking the time of the last event
			assertEquals(38039, list.get(index).getTime(), 0.9);
		}

		assertTrue(wasInLoop);
	}

}
