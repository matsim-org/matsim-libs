package playground.dhosse.prt.launch;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PrtLauncher {
	
	public static void main(String args[]){
		
		PrtParameters params = new PrtParameters(args);
		VrpLauncher launcher = new VrpLauncher(params);
		launcher.run();
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/config.xml");
		Scenario sc = ScenarioUtils.loadScenario(config);
		System.out.println(sc.getPopulation().getPersons().size());
		
//		VrpLauncher launcher = new VrpLauncher(netFile, plansFile, eventsFileName);
//		launcher.run();
        
	}
	
}
