package playground.dhosse.gap.run;

import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.runExample.RunCarsharing;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class GAPScenarioRunnerWithCarSharing {
	
	public static void main(String args[]){
		
		final Config config = ConfigUtils.createConfig();
		CarsharingUtils.addConfigModules(config);
		
		new ConfigWriter(config).write("/home/dhosse/Dokumente/01_eGAP/config_cs.xml");
		
//		RunCarsharing.main(args);
		
	}

}
