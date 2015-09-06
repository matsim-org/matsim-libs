package playground.wrashid.msimoni.analyses;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class Coordinate {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("H:/thesis/output_no_pricing_v3_subtours/output_network.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Coord center = new Coord(684281.6, 246570.3);
		double distance = 1500.0;
		double length = 50.0;
		
		selectLinks(scenario.getNetwork(), center, distance, length);
	}
	
	public static Map<Id, Link> selectLinks(Network network, Coord center, double distance, double length) {
		Map<Id, Link> selectedLinks = new TreeMap<Id, Link>();
		
		for (Link link : network.getLinks().values()) {
			if (link.getLength() < length) continue;
			
			double d = CoordUtils.calcDistance(center, link.getCoord());
			if (d < distance) {
				System.out.println(link.getId().toString());
				selectedLinks.put(link.getId(), link);
			}
		}
		
		System.out.println("Selected " + selectedLinks.size() + " links for further analysis.");
		return selectedLinks;
	}
	
}
