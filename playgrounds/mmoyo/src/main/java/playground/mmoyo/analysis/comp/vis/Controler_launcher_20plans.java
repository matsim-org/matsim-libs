package playground.mmoyo.analysis.comp.vis;

import playground.mmoyo.TransitSimulation.MMoyoTransitControler;

public class Controler_launcher_20plans {
	
	public static void main(String[] args) {
		
		//nullfall version
		//String conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/nullFall/config_20RoutedPlans.xml";
		
		//20 plans of 900s small
		String conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/20plans/config_20plans900s_small.xml";
		MMoyoTransitControler.main(new String []{conf});
		
		}
}
