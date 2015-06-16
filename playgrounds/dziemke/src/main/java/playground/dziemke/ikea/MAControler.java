package playground.dziemke.ikea;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class MAControler {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("input/config.xml");

		Controler controler = new Controler(config);
	//	controler.setOverwriteFiles(true);
		controler.run();
	}

}
