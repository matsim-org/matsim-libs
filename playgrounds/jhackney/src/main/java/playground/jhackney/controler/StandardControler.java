package playground.jhackney.controler;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

public class StandardControler extends Controler {

	private final Logger log = Logger.getLogger(StandardControler.class);
	
	public StandardControler(String args[]){
		super(args);
	}

	public static void main(final String[] args) {
		final Controler controler = new Controler(args);
		controler.addControlerListener(new StandardControlerListener());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
