package playground.pieter.network;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.singapore.utils.plans.MyPlansToPlans;
import playground.pieter.singapore.utils.plans.PlansAddCarAvailability;
import playground.pieter.singapore.utils.plans.PlansFilterNoRoute;

public class NetworkLinkRemover {
	public void run(final String[] args) {
		Scenario scenario;
		MatsimRandom.reset(123);
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario)
				.readFile(args[0]);
		ArrayList<String> removeLinkList = new ArrayList<String>();
		for(Link l:scenario.getNetwork().getLinks().values()){
			if(!l.getAllowedModes().contains(args[2]))
				removeLinkList.add(l.getId().toString());
		}
		for(String id:removeLinkList){
			scenario.getNetwork().removeLink(new IdImpl(id));
		}
		new NetworkWriter(scenario.getNetwork()).write(args[1]);
	}

	public static void main(final String[] args) {
		new NetworkLinkRemover().run(args);
	}
}
