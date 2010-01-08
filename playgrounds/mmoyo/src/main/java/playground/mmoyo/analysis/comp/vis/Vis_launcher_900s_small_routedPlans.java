package playground.mmoyo.analysis.comp.vis;

import org.matsim.run.OTFVis;

public class Vis_launcher_900s_small_routedPlans {
	public static void main(String[] args) {
		String otfVisConfigFile = null;
		
		byte routeCalcIndex = 3;     // 1=rieser    2 = moyoTime    3= moyoParameterized
		
		switch (routeCalcIndex){
			case 1:
				otfVisConfigFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/vis_configs/otfvis_config_900s_small_rieser.xml";
				break;
			case 2:
				otfVisConfigFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/vis_configs/otfvis_config_900s_small_moyo_time.xml";
				break;
			case 3:
				otfVisConfigFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/vis_configs/otfvis_config_900s_small_moyo_parameterized.xml";
				break;
		}
		OTFVis.main(new String[]{otfVisConfigFile});
	}
}
