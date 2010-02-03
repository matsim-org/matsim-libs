package playground.mmoyo.analysis.comp.vis;

import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher_BerlinBrandenburg {
	
	public static void main(String[] args) {
		String conf;
		
		//configs for fragmented plans
		
		//parameterized
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans_MoyoParameterized.xml";
		
		//time
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans_MoyoTime.xml";
		
		//marginalUtility
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans.xml";

		
		TransitControler.main(new String []{conf});
	}
}
