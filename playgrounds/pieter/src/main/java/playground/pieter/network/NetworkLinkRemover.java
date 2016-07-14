package playground.pieter.network;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkLinkRemover {
	void run(final String[] args) {
		Scenario scenario;
		MatsimRandom.reset(123);
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork())
				.readFile(args[0]);
		ArrayList<String> removeLinkList = new ArrayList<>();
		for(Link l:scenario.getNetwork().getLinks().values()){
			if(!l.getAllowedModes().contains(args[2]))
				removeLinkList.add(l.getId().toString());
		}
		for(String id:removeLinkList){
			scenario.getNetwork().removeLink(Id.createLinkId(id));
		}
		new NetworkWriter(scenario.getNetwork()).write(args[1]);
	}

	public static void main(final String[] args) {
		new NetworkLinkRemover().run(args);
	}
}
