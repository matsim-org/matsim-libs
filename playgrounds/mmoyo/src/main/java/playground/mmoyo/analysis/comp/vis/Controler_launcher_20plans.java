package playground.mmoyo.analysis.comp.vis;

import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher_20plans {
	
	public static void main(String[] args) {
		
		String conf;
	
		//20 plans taken from 900s_small moyo_parameterized
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/20plans/config_20plans900s_small.xml";

		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/detoured/configDetouredPopulation.xml";
		
		TransitControler.main(new String []{conf});
	}
}
