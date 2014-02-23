package playground.southafrica.gauteng;

import playground.southafrica.gauteng.GautengControler_subpopulations.User;

public class JWJGautengController_Toll {
	
	static String GAUTENG_PATH = "/Users/jwjoubert/Documents/workspace/data-gauteng/" ;

	public static void main(String[] args) {
		if(args.length > 0 &&
				args[0] != null &&
				args[0].length() > 0){
			GAUTENG_PATH = args[0];
		}
		
		String[] args2 = {
				GAUTENG_PATH + "config/basicConfig.xml", // config
				"output_base/output_plans.xml.gz", // pop
				GAUTENG_PATH + "population/20140208/gautengAttr.xml.gz", // pop attribs 
				GAUTENG_PATH + "network/gauteng_20131210_coarseNationalNetwork_clean.xml.gz", // net 
				GAUTENG_PATH + "toll/gauteng_toll_weekday_Combined_20131211.xml", // toll
				"110", // base VoT
				"3", // VoT multiplier
				"8", // number of threads
				User.johan.toString(),
//				"/Users/jwjoubert/Documents/Temp/sanral-runs/output/"
				"./output_toll/"
				} ;
		GautengControler_subpopulations.main(args2);
	}

}
