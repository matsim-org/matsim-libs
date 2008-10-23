package playground.jhackney.controler;

import org.matsim.controler.Controler;

public class SNController extends Controler {

	public SNController(String args[]){
		super(args);
	}
	public static void main(final String[] args) {
		final Controler controler = new SNController(args);
		controler.addControlerListener(new SNControllerListener2());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
