package org.matsim.contrib.locationchoice.frozenepsilons;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;


public class BestReplyIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunControler() {
		// load chessboard scenario config:
        Config config = utils.loadConfig(
        		IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml"),
				new DestinationChoiceConfigGroup()
		);
  
        // override or add some material:
		ConfigUtils.loadConfig(config, utils.getPackageInputDirectory() + "/config.xml");

		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		
		
        Scenario scenario = ScenarioUtils.loadScenario(config);
		RunLocationChoiceBestResponse.run(scenario );
	}

}
