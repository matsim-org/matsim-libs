package playground.wrashid.ABMT.vehicleShare;

import org.matsim.core.config.Config;

public class GlobalTESFParameters {

	public static double tollAreaRadius;
	
	public static void init(Config config){
		tollAreaRadius=Double.parseDouble(config.getParam("TESF", "tollAreaRadius"));
	}
	
}
