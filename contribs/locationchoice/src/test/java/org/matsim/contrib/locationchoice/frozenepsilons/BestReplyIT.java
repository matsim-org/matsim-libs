package org.matsim.contrib.locationchoice.frozenepsilons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;


public class BestReplyIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRunControler() {
		// load chessboard scenario config:
        Config config = utils.loadConfig(
        		IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml"),
				new FrozenTastesConfigGroup()
		);

        // override or add some material:
		ConfigUtils.loadConfig(config, utils.getPackageInputDirectory() + "/config.xml");

		config.controller().setOutputDirectory( utils.getOutputDirectory() );
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );


        Scenario scenario = ScenarioUtils.loadScenario(config);
		RunLocationChoiceBestResponse.run(scenario );
	}

}
