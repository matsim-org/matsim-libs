package org.matsim.core.mobsim.jdeqsim;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.Assert.assertEquals;

public class TestDESStarter_equilPlans100 extends AbstractJDEQSimTest {

	@Test
	public void test_equilPlans100_TestHandlerDetailedEventChecker() {
		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
		this.runJDEQSim(scenario);
		
		assertEquals(scenario.getPopulation().getPersons().size(), super.eventsByPerson.size());
		super.checkAscendingTimeStamps();
		super.checkEventsCorrespondToPlans(scenario.getPopulation());
	}
	
	/* 
	 * This test is turned off, because it cannot pass.
	 * Reason: Different priorities possible at junctions, which (almost) always result in different event order.
	 */ 
	
	/*
	public void test_equilPlans100_DEQSimEventFileComparator() {
		DEQSimEventFileComparator deqSimComparator = new DEQSimEventFileComparator("test/src/playground/wrashid/input/deqsim/deq_events100.txt");
		deqSimComparator.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans100.xml",
				null);
	}
	*/
	
	/*
	 * This test was turn off, because java deqsim is based on time specified on the acts (as mobsim)
	 * and is not based on times specified on the leg as in c++ deqsim
	 */
	
	/*
	public void test_equilPlans100_DEQSimEventFileTravelTimeComparator() {
		DEQSimEventFileTravelTimeComparator deqSimTravelTimeComparator = new DEQSimEventFileTravelTimeComparator("test/input/org/matsim/mobsim/deqsim/deq_events_100.txt",1);
		deqSimTravelTimeComparator.startTestDES("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans100.xml",
				null);
	}
*/
}
