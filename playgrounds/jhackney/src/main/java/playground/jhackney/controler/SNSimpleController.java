package playground.jhackney.controler;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

public class SNSimpleController extends Controler {

	private final Logger log = Logger.getLogger(SNSimpleController.class);
	
	public SNSimpleController(String args[]){
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
