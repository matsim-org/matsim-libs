package playground.vbmh.controler;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class BasicControler {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Config config = ConfigUtils.loadConfig(args[0]);
		Controler controler = new Controler(config);
		controler.run();

	}

}
