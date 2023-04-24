package org.matsim.simwrapper.analysis;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.TestScenario;
import org.matsim.simwrapper.dashboard.StuckAgentDashboard;
import org.matsim.testcases.MatsimTestUtils;


public class StuckAgentAnalysisTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void run() {

		Config config = TestScenario.loadConfig(utils);

		SimWrapper sw = SimWrapper.create()
				.addDashboard(new StuckAgentDashboard());

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);
		controler.run();

		Assertions.fail("Implement assertions and remove this line.");
	}
}
