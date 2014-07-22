package playground.artemc.networkTools;

import java.util.HashSet;
import java.util.Set;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

public class CorridorCreator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String networkPath = args[0];

		int totalLength = 20000;
		int linkLength = 600;
		double freespeed = 13.9;
		double capacity = 1000;
		int numLanes = 3;

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Set<String> allowedModesAll = new HashSet<String>();
		allowedModesAll.add("car");
		allowedModesAll.add("pt");
		allowedModesAll.add("bus");

		int restLength = totalLength;
		int newLinkId = 1;
		int newNodeId = 1;
		IdImpl fromNode = new IdImpl(newNodeId);
		network.createAndAddNode(fromNode, new CoordImpl(10.0, 10.0));

		while (restLength > 0) {
			newNodeId++;
			IdImpl toNode = new IdImpl(newNodeId);

			if (restLength > linkLength) {
				network.createAndAddNode(toNode, new CoordImpl(10.0 + totalLength - restLength + linkLength, 10.0));
				network.createAndAddLink(new IdImpl(newLinkId), network.getNodes().get(fromNode), network.getNodes().get(toNode),
						linkLength, freespeed, capacity * numLanes, numLanes);
				network.createAndAddLink(new IdImpl(newLinkId + "r"), network.getNodes().get(toNode),
						network.getNodes().get(fromNode), linkLength, freespeed, capacity * numLanes, numLanes);

			} else {
				network.createAndAddNode(toNode, new CoordImpl(totalLength, 10.0));
				network.createAndAddLink(new IdImpl(newLinkId), network.getNodes().get(fromNode), network.getNodes().get(toNode),
						restLength, freespeed, capacity * numLanes, numLanes);
				network.createAndAddLink(new IdImpl(newLinkId + "r"), network.getNodes().get(toNode),
						network.getNodes().get(fromNode), restLength, freespeed, capacity * numLanes, numLanes);

			}
			
			network.getLinks().get(new IdImpl(newLinkId)).setAllowedModes(allowedModesAll);
			network.getLinks().get(new IdImpl(newLinkId + "r")).setAllowedModes(allowedModesAll);



			
			fromNode = toNode;
			restLength = restLength - linkLength;
			newLinkId++;

		}

		System.out.println("Writting network...");
		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(networkPath);
		System.out.println("Done!");
	}

}
