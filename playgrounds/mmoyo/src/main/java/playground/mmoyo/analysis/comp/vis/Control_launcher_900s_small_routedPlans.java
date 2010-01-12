package playground.mmoyo.analysis.comp.vis;

import playground.mzilske.bvg09.TransitControler;

public class Control_launcher_900s_small_routedPlans {
	public static void main(String[] args) {
		String configFile = null;
		
		byte routeCalcIndex = 3;     // 1=rieser    2 = moyoTime    3= moyoParameterized
		
		switch (routeCalcIndex){
			case 1:
				configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_rieser.xml";
				break;
			case 2:
				configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_moyo_time.xml";
				break;
			case 3:
				configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_moyo_parameterized.xml";
				break;
		}
		TransitControler.main(new String[]{configFile});
	}
}
