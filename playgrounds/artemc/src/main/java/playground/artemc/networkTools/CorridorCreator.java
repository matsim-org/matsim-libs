package playground.artemc.networkTools;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
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
		scenario.getConfig().transit().setUseTransit(true);

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Set<String> allowedModesAll = new HashSet<String>();
		allowedModesAll.add("car");
		allowedModesAll.add("pt");
		allowedModesAll.add("bus");

		int restLength = totalLength;
		int newLinkId = 1;
		int newNodeId = 1;
		Id<Node> fromNode = Id.create(newNodeId, Node.class);
		network.createAndAddNode(fromNode, new CoordImpl(10.0, 10.0));

		while (restLength > 0) {
			newNodeId++;
			Id<Node> toNode = Id.create(newNodeId, Node.class);

			if (restLength > linkLength) {
				network.createAndAddNode(toNode, new CoordImpl(10.0 + totalLength - restLength + linkLength, 10.0));
				network.createAndAddLink(Id.create(newLinkId, Link.class), network.getNodes().get(fromNode), network.getNodes().get(toNode),
						linkLength, freespeed, capacity * numLanes, numLanes);
				network.createAndAddLink(Id.create(newLinkId + "r", Link.class), network.getNodes().get(toNode),
						network.getNodes().get(fromNode), linkLength, freespeed, capacity * numLanes, numLanes);

			} else {
				network.createAndAddNode(toNode, new CoordImpl(totalLength, 10.0));
				network.createAndAddLink(Id.create(newLinkId, Link.class), network.getNodes().get(fromNode), network.getNodes().get(toNode),
						restLength, freespeed, capacity * numLanes, numLanes);
				network.createAndAddLink(Id.create(newLinkId + "r", Link.class), network.getNodes().get(toNode),
						network.getNodes().get(fromNode), restLength, freespeed, capacity * numLanes, numLanes);

			}
			
			network.getLinks().get(Id.create(newLinkId, Link.class)).setAllowedModes(allowedModesAll);
			network.getLinks().get(Id.create(newLinkId + "r", Link.class)).setAllowedModes(allowedModesAll);



			
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
