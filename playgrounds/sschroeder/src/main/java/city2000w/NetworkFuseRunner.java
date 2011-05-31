package city2000w;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import utils.NetworkFuser;


public class NetworkFuseRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		String networkFilename = "output/germany_bigroads.xml";
		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		
		NetworkFuser networkFuser = new NetworkFuser(scenario.getNetwork());
		networkFuser.fuse();
		
		new NetworkWriter(scenario.getNetwork()).write("output/germany_bigroads_testFusion.xml");
	}

}
