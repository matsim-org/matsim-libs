package city2000w;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import utils.NetworkFuser;

public class FuseBerlin {
	
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		String networkFilename = "networks/berlinRoads.xml";
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		NetworkFuser fuser = new NetworkFuser(scenario.getNetwork());
		fuser.fuse();
		new NetworkWriter(scenario.getNetwork()).write("networks/berlinRoadsFused.xml");
	}

}
