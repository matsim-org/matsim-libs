package gunnar.ihop2.integration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class SaleemRunner {

	public static void main(String[] args) {

		String configFileName = "./data/saleem/config.xml";
		Config config = ConfigUtils.loadConfig(configFileName);
		Controler controler = new Controler(config);

		controler.run();

	}

}
