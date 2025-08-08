package org.matsim.pt.transitSchedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

/**
 * Tests if chained departures are correctly simulated.
 */
public class ChainedDepartureIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void scenario() throws Exception {

		Config config = ConfigUtils.createConfig();
		config.setContext(new File("test/input/ch/sbb/matsim/routing/pt/raptor/").toURI().toURL());

		config.network().setInputFile("network.xml");
		config.transit().setTransitScheduleFile("schedule.xml");
		config.transit().setVehiclesFile("vehicles.xml");
		config.transit().setUseTransit(true);
		config.plans().setInputFile("population.xml");
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.none);
		config.qsim().setEndTime(Time.parseTime("36:00:00"));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(8 * 3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.run();

		// TODO: assertions that events are as expected and agents arrived correctly at their destination


	}
}
