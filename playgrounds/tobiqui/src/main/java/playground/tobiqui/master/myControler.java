package playground.tobiqui.master;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class myControler {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("../matsim/examples/siouxfalls-2014/config_default.xml");
		config.controler().setLastIteration(0);
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();

	}

}
