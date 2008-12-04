package playground.jhackney.controler;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;

public class SNController2 extends Controler {

	private final Logger log = Logger.getLogger(SNController2.class);
	
	public SNController2(String args[]){
		super(args);
	}

	public static void main(final String[] args) {
		final Controler controler = new SNController2(args);
		controler.addControlerListener(new SNControllerListener2());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
