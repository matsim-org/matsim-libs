package playground.wrashid.kti;

import org.matsim.core.controler.Controler;
import org.matsim.roadpricing.RoadPricing;

import playground.meisterk.kti.controler.KTIControler;

public class KTIControler_v2_withRoadPricing {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: KtiControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new KTIControler_v2(args);
			controler.setOverwriteFiles(true);
			controler.addControlerListener(new RoadPricing());
			controler.run();
		}
		System.exit(0);
	}
	
}
