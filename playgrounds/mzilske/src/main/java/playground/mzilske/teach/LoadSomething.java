package playground.mzilske.teach;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class LoadSomething {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("../../matsim/examples/equil/network.xml");
		config.plans().setInputFile("../../matsim/examples/equil/plans2.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
	}

}
