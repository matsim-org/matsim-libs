package playground.southafrica.gauteng;

import playground.southafrica.gauteng.GautengControler_subpopulations.User;

public class KNGautengController {
	
	final static String GAUTENG_PATH = "/Users/nagel/southafrica/MATSim-SA/data/areas/gauteng/" ;

	public static void main(String[] args) {
		
		String[] args2 = {
				GAUTENG_PATH + "config/basicConfig.xml" // config
//				, GAUTENG_PATH + "population/20140124/gauteng.xml.gz" // pop
				, "/Users/nagel/gauteng-kairuns/plans-w-routes.xml.gz"
				, GAUTENG_PATH + "population/20140124/gautengAttr.xml.gz" // pop attribs 
				, GAUTENG_PATH + "network/gauteng_20131210_coarseNationalNetwork_clean.xml.gz" // net 
				, GAUTENG_PATH + "toll/gauteng_toll_weekday_Existing_20131211.xml" // toll
				, "110" // base VoT
				, "4" // VoT multiplier
				, "1" // number of threads
				, User.kai.toString()
				} ;
		GautengControler_subpopulations.main(args2);
	}

}
