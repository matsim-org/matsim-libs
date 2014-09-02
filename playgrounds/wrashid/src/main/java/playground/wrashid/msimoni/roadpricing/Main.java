package playground.wrashid.msimoni.roadpricing;

import org.matsim.contrib.roadpricing.RoadPricing;
import org.matsim.core.controler.Controler;

public class Main {

	public static void main(String[] args) {
		Controler controler=new Controler(args);
		controler.addControlerListener(new RoadPricing());
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
}
