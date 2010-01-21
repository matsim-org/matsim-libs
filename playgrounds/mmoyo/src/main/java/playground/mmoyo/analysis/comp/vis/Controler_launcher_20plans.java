package playground.mmoyo.analysis.comp.vis;

import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher_20plans {
	
	public static void main(String[] args) {
		
		String conf;
		
		//1) 20 plans of 900s small
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/20plans/config_20plans900s_small.xml";
		
		//2)  20 plans of NullFall_Alles inside investigation area
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/NullFallAlles/configRouted.insideArea20Plans.xml";
		
		
		//Berlin-Brandenburg:  baseplan_1x_xy2links.xml.gz//////////  
		//3) with standard router
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/config/config_routedPlans.xml";
		
		//4)with moyo_time_priority
		conf = "/shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/config/config_routedPlansMoyoTime.xml";

		//5) with parameterized. in this case: 95% distance priority,   05% time priority   300 transfer penalty   
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/config/config_routerPlansMoyoParameterized.xml";
		
		
		
		TransitControler.main(new String []{conf});
	}
}
