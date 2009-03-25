package playground.kai.usecases.basicmentalmodule;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

public class MyControler {

	public static void main(final String[] args) {

		if ( args.length==0 ) {
			Gbl.createConfig(new String[] {"./examples/equil/myconfig.xml"});
		} else {
			Gbl.createConfig(args) ;
		}

		final Controler controler = new Controler(Gbl.getConfig());
		controler.setOverwriteFiles(true);
		controler.run();
		
	}
}
