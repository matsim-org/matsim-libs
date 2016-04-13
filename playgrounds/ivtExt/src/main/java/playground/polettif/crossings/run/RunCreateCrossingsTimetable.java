package playground.polettif.crossings.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.polettif.crossings.CreateCrossingsTimetable;

public class RunCreateCrossingsTimetable {

	public static void main(String[] args) {

		String inputNetworkFile, inputConfigFile, inputEventsFile, inputCrossingsFile, outputNetworkChangeEventsFile;

		String base = "C:/Users/polettif/Desktop/crossings/";
		String scenarioName = "pt";
		String configName = "config";
		String networkName = "network";

		String inputBase = base + "input/" + scenarioName+"/";
		inputConfigFile = inputBase + configName + ".xml";
		inputNetworkFile = inputBase + networkName + ".xml";
		inputCrossingsFile = inputBase + "crossings.xml";
		outputNetworkChangeEventsFile = inputBase + "/networkChangeEvents.xml";
		inputEventsFile = base + "output/" + scenarioName + "_01/ITERS/it.0/0.events.xml.gz";

		// run 1 iteration of scenario
		Config config = ConfigUtils.loadConfig(inputConfigFile);
		config.setParam("controler", "outputDirectory", base+"output/"+scenarioName+"_01/");
		config.setParam("network", "inputNetworkFile", inputNetworkFile);
		config.setParam("plans", "inputPlansFile", inputBase+"population.xml");
		config.setParam("transit", "transitScheduleFile", inputBase+"transitschedule.xml");
		config.setParam("transit", "vehiclesFile", inputBase+"transitVehicles.xml");

		Scenario scenario01 = ScenarioUtils.loadScenario(config);
		Controler controler01 = new Controler(scenario01);
		controler01.run();

		// generate networkChangeEvents file
		CreateCrossingsTimetable.run(inputNetworkFile, inputCrossingsFile, inputEventsFile, outputNetworkChangeEventsFile, 80.0, 40.0);

		// run scenario again with crossings
		config.setParam("controler", "outputDirectory", base+"output/"+scenarioName+"_02/");
		config.setParam("network", "timeVariantNetwork",  "true");
		config.setParam("network", "inputChangeEventsFile",  outputNetworkChangeEventsFile);

		Scenario scenario02 = ScenarioUtils.loadScenario(config);
		Controler controler02 = new Controler(scenario02);
		controler02.run();

	}

}