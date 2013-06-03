package playground.wrashid.msimoni.roadpricing;

import org.matsim.core.controler.Controler;
import org.matsim.roadpricing.RoadPricing;

public class Main {

	public static void main(String[] args) {
		Controler controler=new Controler(args);
		controler.addControlerListener(new RoadPricing());
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
}
