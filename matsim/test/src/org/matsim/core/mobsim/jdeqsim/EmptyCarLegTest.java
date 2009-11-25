package org.matsim.core.mobsim.jdeqsim;


import java.util.LinkedList;

import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.jdeqsim.util.TestHandlerDetailedEventChecker;
import org.matsim.core.population.PopulationImpl;

public class EmptyCarLegTest extends TestHandlerDetailedEventChecker {

	public void test_EmptyCarRoute() {
		Gbl.reset();
		
		EmptyCarLegTest emptyCarLegTest = new EmptyCarLegTest();
		emptyCarLegTest.startTestDES("test/input/org/matsim/core/mobsim/jdeqsim/config1.xml", false, null, null);
	}
	
	public void checkAssertions(final PopulationImpl population) {
		boolean wasInLoop=false;
		
		for (LinkedList<PersonEvent> list : events.values()) {
			wasInLoop=true;
			assertTrue(list.get(0) instanceof ActivityEndEventImpl);
			assertTrue(list.get(1) instanceof AgentDepartureEventImpl);
			assertTrue(list.get(2) instanceof AgentArrivalEventImpl);
			assertTrue(list.get(3) instanceof ActivityStartEventImpl);
		}
		assertTrue(wasInLoop);
	}
	
}
