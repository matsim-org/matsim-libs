package util;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class NodeAndLinkCount {
	
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("sensitivity/network.xml");
		
		System.out.println("#nodes: " + scenario.getNetwork().getNodes().values().size());
		System.out.println("#links: " + scenario.getNetwork().getLinks().values().size());
	}

}
