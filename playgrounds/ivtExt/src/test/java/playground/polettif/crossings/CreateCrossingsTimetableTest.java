package playground.polettif.crossings;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.Assert.*;

public class CreateCrossingsTimetableTest {

	@Test
	public void testRun() {
	/*
		String inputNetworkFile, inputConfigFile, inputEventsFile, inputCrossingsFile, outputNetworkChangeEventsFile;

		String base = "C:/Users/polettif/Desktop/";
		String scenarioName = "pt";
		String configName = "config";
		String networkName = "network_10mCrossingLength";

		inputConfigFile = base + "input/" + scenarioName + "/01_" + configName + ".xml";
		inputNetworkFile = base + "input/" + scenarioName + "/" + networkName + ".xml";
		inputEventsFile = base + "output/" + scenarioName + "_01/ITERS/it.0/0.events.xml.gz";
		inputCrossingsFile = base + "input/" + scenarioName + "/crossings.xml";
		outputNetworkChangeEventsFile = base + "input/" + scenarioName + "/networkChangeEvents.xml";

		// run 1 iteration of scenario
		Config config01 = ConfigUtils.loadConfig(inputConfigFile);
		Scenario scenario01 = ScenarioUtils.loadScenario(config01);
		Controler controler01 = new Controler(scenario01);
		controler01.run();

		// generate networkChangeEvents file
		CreateCrossingsTimetable.run(inputNetworkFile, inputCrossingsFile, inputEventsFile, outputNetworkChangeEventsFile, 80.0, 40.0);

		// run scenario again with crossings
		String filename02 = base + "input/" + scenarioName + "/02_" + configName + ".xml";
		Config config02 = ConfigUtils.loadConfig(filename02);
		Scenario scenario02 = ScenarioUtils.loadScenario(config02);
		Controler controler02 = new Controler(scenario02);
		controler02.run();
		*/
	}

}