package playground.jhackney.controler;

import org.matsim.controler.Controler;

public class SNControllerRePlanSecLoc extends Controler {

	public SNControllerRePlanSecLoc(String args[]){
		super(args);
	}
	public static void main(final String[] args) {
		final Controler controler = new SNControllerRePlanSecLoc(args);
		controler.addControlerListener(new SNControllerListenerRePlanSecLoc());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
