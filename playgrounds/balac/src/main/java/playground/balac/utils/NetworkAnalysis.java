package playground.balac.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class NetworkAnalysis {

	public static void main(String[] args) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);

		int numberOfLinks = 0;
		double length = 0.0;
		double length1 = 0.0;
		double length2 = 0.0;
		double length3 = 0.0;
		Node previousNode1 = null;
		Node previousNode2 = null;
		for(Link l:scenario.getNetwork().getLinks().values()) {
			if (previousNode1 != null) {
				
				if (l.getFromNode().getId() != previousNode2.getId() && l.getToNode().getId() != previousNode1.getId()) {
					numberOfLinks++;
				length += l.getLength();
				if (l.getFreespeed() > 24.99) {
					length1 += l.getLength();
				}
				else if (l.getFreespeed() < 13.88) {
					length3 += l.getLength();
				
				}
				else
					length2 += l.getLength();
				}
		}
			else {
				numberOfLinks++;
				length += l.getLength();
				if (l.getFreespeed() > 24.99) {
					length1 += l.getLength();
				}
				else if (l.getFreespeed() < 13.88) {
					length3 += l.getLength();
				
				}
				else
					length2 += l.getLength();
				
			}
			previousNode1 = l.getFromNode();
			previousNode2 = l.getToNode();
		}
		System.out.println(numberOfLinks);
	System.out.println(length/1000);	
	System.out.println(length1/1000);
	System.out.println(length2/1000);
	System.out.println(length3/1000);
	}

}
