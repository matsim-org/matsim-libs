package playground.polettif.crossings.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.polettif.crossings.CreateCrossingsTimetable;

/**
 * Runs an initial simulation run to create an events files. Then, a network change
 * events file is generated based on the network file, a crossing file, and the previously
 * created events file. The scenario is run again, this time with crossings.
 *
 * @author polettif
 */
public class RunCreateCrossingsTimetable {

	public static void main(String[] args) {

		String inputNetworkFile, inputConfigFile, inputEventsFile, inputCrossingsFile, outputNetworkChangeEventsFile;

		String base = "";
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
		config.controler().setOutputDirectory(base+"output/"+scenarioName+"_01/");
		config.network().setInputFile(inputNetworkFile);
		config.plans().setInputFile(inputBase+"population.xml");
		config.transit().setTransitScheduleFile(inputBase+"transitschedule.xml");
		config.transit().setVehiclesFile(inputBase+"transitVehicles.xml");

		Scenario scenario01 = ScenarioUtils.loadScenario(config);
		Controler controler01 = new Controler(scenario01);
		controler01.run();

		// generate networkChangeEvents file
		CreateCrossingsTimetable.run(inputNetworkFile, inputCrossingsFile, inputEventsFile, outputNetworkChangeEventsFile, 80.0, 40.0);

		// run scenario again with crossings
		config.controler().setOutputDirectory(base+"output/"+scenarioName+"_02/");
		config.network().setTimeVariantNetwork(true);
		config.network().setChangeEventsInputFile(outputNetworkChangeEventsFile);

		Scenario scenario02 = ScenarioUtils.loadScenario(config);
		Controler controler02 = new Controler(scenario02);
		controler02.run();

	}

}