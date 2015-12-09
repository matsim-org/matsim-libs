package playground.sergioo.capstone2015;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class TaxiNetwork {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		for(Link link:scenario.getNetwork().getLinks().values())
			if(link.getAllowedModes().contains("car")) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				modes.add("taxi");
				link.setAllowedModes(modes);
			}
		new NetworkWriter(scenario.getNetwork()).write(args[1]);
	}

}
