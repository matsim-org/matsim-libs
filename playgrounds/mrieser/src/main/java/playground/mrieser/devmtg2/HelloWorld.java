package playground.mrieser.devmtg2;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / Senozon AG
 */
public class HelloWorld {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);

//		controler.addControlerListener(new EmissionsControlerListener());

		controler.run();

	}

}
