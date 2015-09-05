package org.matsim.core.mobsim.jdeqsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.jdeqsim.scenarios.EquilPopulationPlans1Modified1;
import org.matsim.core.scenario.ScenarioUtils;

public class TestDESStarter_EquilPopulationPlans1Modified1 extends AbstractJDEQSimTest {

	public void test_EquilPopulationPlans1Modified1_TestHandlerDetailedEventChecker() {
		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		config.plans().setInputFile("test/scenarios/equil/plans1.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
		new EquilPopulationPlans1Modified1().modifyPopulation(scenario.getPopulation());
		this.runJDEQSim(scenario);
		
		assertEquals(scenario.getPopulation().getPersons().size(), super.eventsByPerson.size());
		super.checkAscendingTimeStamps();
		super.checkEventsCorrespondToPlans(scenario.getPopulation());
		super.compareToDEQSimTravelTimes(getPackageInputDirectory() + "deq_events.txt", 1.0);
	}

}
