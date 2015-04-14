package playground.tobiqui.master;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class myControler {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/config_renamed.xml");
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.run();

	}

}
