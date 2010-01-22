package playground.mmoyo.analysis.comp.vis;

import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher_BerlinBrandenburg {
	
	public static void main(String[] args) {
		
		String conf;

		//no car plans
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_nocarplans/config/config_routedPlans.xml";
		conf ="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_nocarplans/config/config_routedPlans_MoyoTime.xml";
		conf="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_nocarplans/config/config_routedPlans_MoyoParameterized.xml";
		
		//only pt legs
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/config/config_routedPlans.xml";
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/config/config_routedPlans_MoyoTime.xml";
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/config/config_routedPlans_MoyoParameterized.xml";
		
		TransitControler.main(new String []{conf});
	}
}
