package playground.mmoyo.analysis.comp.vis;

import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher_20plans {
	
	public static void main(String[] args) {
		
		String conf;
		
		//20 plans of 900s small
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/20plans/config_20plans900s_small.xml";
		
		//20 plans of NullFall_Alles inside investigation area
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/NullFallAlles/configRouted.insideArea20Plans.xml";
		
		TransitControler.main(new String []{conf});
	}
}
