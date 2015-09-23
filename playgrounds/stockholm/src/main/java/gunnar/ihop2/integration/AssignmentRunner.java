package gunnar.ihop2.integration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class AssignmentRunner {

	public AssignmentRunner() {
	}

	public static void main(String[] args) {

		final String configFile = "";

		final Config config = ConfigUtils.loadConfig(configFile);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);

		controler.run();
	}

}
