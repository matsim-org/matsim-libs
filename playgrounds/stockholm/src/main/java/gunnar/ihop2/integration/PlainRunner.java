package gunnar.ihop2.integration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class PlainRunner {

	public static void main(String[] args) {

		String configFileName = "./input/matsim-config.xml";
		Config config = ConfigUtils.loadConfig(configFileName);
		Controler controler = new Controler(config);

		controler.run();

	}

}
