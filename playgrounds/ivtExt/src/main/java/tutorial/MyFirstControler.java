package tutorial;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class MyFirstControler {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("input/config.xml");
		Controler controler = new Controler(config) ;
		//controler.setOverwriteFiles(true) ;
		controler.run() ;
	}
	
}
