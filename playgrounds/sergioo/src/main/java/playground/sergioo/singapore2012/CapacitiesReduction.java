package playground.sergioo.singapore2012;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class CapacitiesReduction {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Map<Id, Set<Id>> intersections = new HashMap<Id, Set<Id>>();
		Set<Id> noLinks = new HashSet<Id>();
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String line = reader.readLine();
		line = reader.readLine();
		do {
			String[] parts = line.split(",");
			Id intId = new IdImpl(parts[4]);
			Set<Id> intersection = intersections.get(intId);
			if(intersection==null) {
				intersection = new HashSet<Id>();
				intersections.put(intId, intersection);
			}
			intersection.add(new IdImpl(parts[1]));
			line = reader.readLine();
		} while(line!=null);
		reader = new BufferedReader(new FileReader(args[1]));
		line = reader.readLine();
		line = reader.readLine();
		do {
			String[] parts = line.split(";");
			noLinks.add(new IdImpl(parts[1]));
			line = reader.readLine();
		} while(line!=null);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(scenario)).readFile(args[2]);
		double factor = new Double(args[3]);
		for(Set<Id> intersection:intersections.values())
			for(Id node:intersection)
				for(Link link:scenario.getNetwork().getNodes().get(node).getInLinks().values())
					if(!intersection.contains(link.getFromNode()) && !noLinks.contains(link.getId()))
						link.setCapacity(link.getCapacity()*factor);
		(new NetworkWriter(scenario.getNetwork())).write(args[4]);
	}

}
