package playground.mmoyo.analysis.comp.vis;

import playground.mmoyo.TransitSimulation.MMoyoTransitControler;

public class Controler_launcher_20plans {
	
	public static void main(String[] args) {

		String conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/nullFall/config_20RoutedPlans.xml";
		MMoyoTransitControler.main(new String []{conf});
		
		}
}
