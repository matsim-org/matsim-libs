package org.matsim.core.mobsim.jdeqsim;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.Assert.assertEquals;


public class TestDESStarter_Berlin extends AbstractJDEQSimTest {

	@Test
	public void test_Berlin_TestHandlerDetailedEventChecker() {
		Config config = ConfigUtils.loadConfig("test/scenarios/berlin/config.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);

		this.runJDEQSim(scenario);
		
		assertEquals(scenario.getPopulation().getPersons().size(), super.eventsByPerson.size());
		super.checkAscendingTimeStamps();
		super.checkEventsCorrespondToPlans(scenario.getPopulation());
	}

}
