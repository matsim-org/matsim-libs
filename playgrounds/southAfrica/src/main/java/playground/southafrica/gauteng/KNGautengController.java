package playground.southafrica.gauteng;

import playground.southafrica.gauteng.GautengControler_subpopulations.User;

public class KNGautengController {
	
	final static String GAUTENG_PATH = "/Users/nagel/southafrica/MATSim-SA/data/areas/gauteng/" ;

	public static void main(String[] args) {
		
		String[] args2 = {
				GAUTENG_PATH + "config/basicConfig.xml" // config
				, GAUTENG_PATH + "population/20140124/gauteng.xml.gz" // pop
//				, "/Users/nagel/gauteng-kairuns/much_simplified_plans.xml.gz"
				, GAUTENG_PATH + "population/20140124/gautengAttr.xml.gz" // pop attribs 
//				, GAUTENG_PATH + "network/gauteng_20131210_coarseNationalNetwork_clean.xml.gz" // net 
				, "/Users/nagel/gauteng-kairuns/much_simplified_network.xml.gz"
				, GAUTENG_PATH + "toll/gauteng_toll_weekday_Existing_20131211.xml" // toll
				, "110" // base VoT
				, "1" // VoT multiplier
				, "1" // number of threads
				, User.kai.toString()
				, "/Users/nagel/gauteng-kairuns/output"
				, GAUTENG_PATH + "counts/2009/Counts_Thursday_Total.xml.gz"
				} ;
		GautengControler_subpopulations.main(args2);
	}

}
