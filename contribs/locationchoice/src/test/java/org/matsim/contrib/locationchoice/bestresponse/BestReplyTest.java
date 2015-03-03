package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.RunLocationChoiceBestResponse;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;


public class BestReplyTest extends MatsimTestCase {

	public void testRunControler() {
        String configFile = this.getPackageInputDirectory() + "/config.xml";
        Config config = ConfigUtils.loadConfig(configFile, new DestinationChoiceConfigGroup());
        config.controler().setOutputDirectory(getOutputDirectory() + "/run1/");
        Scenario scenario = ScenarioUtils.loadScenario(config);
		RunLocationChoiceBestResponse.run(scenario);
	}

}
