package playground.mmoyo.analysis.comp;

import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher {
	
	public static void main(String[] args) {
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_5x_subset_xy2links_ptplansonly/config/config_Berlin5x_rieser.xml";
		configFile = "../playgrounds/mmoyo/output/comparison/Berlin/config_5x_16plans_moyo.xml";
		TransitControler.main(new String []{configFile});
	}
}
