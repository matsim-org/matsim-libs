package playground.mmoyo.analysis.counts.chen;

import playground.yu.run.TrCtl;

public class Counter {

	public static void main(String[] args) {
		String configFile;
		
		/*
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/configs/configWithPtCounts_noCar_moyo_param.xml";
		TrCtl.main(new String[]{configFile});
		
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/configs/configWithPtCounts_noCar_moyo_time.xml";
		TrCtl.main(new String[]{configFile});
		
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/configs/configWithPtCounts_noCar_rieser.xml";
		TrCtl.main(new String[]{configFile});
		*/
		
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/configs/configWithPtCounts_onlyPT_moyo_param.xml";
		TrCtl.main(new String[]{configFile});
		
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/configs/configWithPtCounts_onlyPT_moyo_time.xml";
		TrCtl.main(new String[]{configFile});
		
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/configs/configWithPtCounts_onlyPT_rieser.xml";
		TrCtl.main(new String[]{configFile});
		
	}
}
