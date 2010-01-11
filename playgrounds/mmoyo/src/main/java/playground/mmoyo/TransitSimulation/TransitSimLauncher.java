package playground.mmoyo.TransitSimulation;

import playground.mzilske.bvg09.TransitControler;

public class TransitSimLauncher {

	public static void main(final String[] args) {
		String configFile = null;
		
		//900s small rieser
		//configFile= "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_rieser.xml";
		
		//90s_small moyo time
		//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_moyo_time.xml";
		
		//90s_small moyo parameterized
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_moyo_parameterized.xml";

		//90s_big rieser
		//configFile = "";

		//90s_big moyo time
		//configFile = "";

		//90s_big moyo parameterized
		//configFile = "";

		
		
		
		TransitControler.main(new String[]{configFile});
	}
}
