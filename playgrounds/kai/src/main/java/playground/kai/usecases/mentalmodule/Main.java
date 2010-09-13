package playground.kai.usecases.mentalmodule;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

class Main {

	public static void main(final String[] args) {
	
		Config config;
		if ( args.length==0 ) {
			config = Gbl.createConfig(new String[] {"./kai/src/main/java/playground/kai/other/myconfig.xml"});
		} else {
			config = Gbl.createConfig(args) ;
		}
	
		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.run();
	
	}

}
