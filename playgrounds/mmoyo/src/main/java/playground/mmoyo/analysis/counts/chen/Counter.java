package playground.mmoyo.analysis.counts.chen;

import playground.yu.run.TrCtl;

/**uses Yu transit controler to have counts results**/
public class Counter {

	public static void main(String[] args) {
		
		String configFile;
		if(args.length>0){ 
			configFile = args[0];
		}else{	
			configFile ="../playgrounds/mmoyo/output/comparison/Berlin/16plans/2config_5x_16plans_manuel.xml"; //"../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_5x_subset_xy2links_ptplansonly/config/config_5x_routed_time.xml";
		}
		TrCtl.main(new String[]{configFile});
		
		
		//read many configs: args[0] is the directory with many configs 
		/*
		File dir = new File(args[0]);
		for (String configFile : dir.list()){
			String completePath= args[0] + "/" + configFile;
			System.out.println("\n\n  procesing: " + completePath);
			TrCtl.main(new String[]{completePath});
		}
		*/
	}
}
