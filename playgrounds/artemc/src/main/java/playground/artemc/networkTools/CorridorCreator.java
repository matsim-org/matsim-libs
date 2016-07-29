package playground.artemc.networkTools;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

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

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);

		Network network = (Network) scenario.getNetwork();

		Set<String> allowedModesAll = new HashSet<String>();
		allowedModesAll.add("car");
		allowedModesAll.add("pt");
		allowedModesAll.add("bus");

		int restLength = totalLength;
		int newLinkId = 1;
		int newNodeId = 1;
		Id<Node> fromNode = Id.create(newNodeId, Node.class);
		final Id<Node> id = fromNode;
		NetworkUtils.createAndAddNode(network, id, new Coord(10.0, 10.0));

		while (restLength > 0) {
			newNodeId++;
			Id<Node> toNode = Id.create(newNodeId, Node.class);

			if (restLength > linkLength) {
				final Id<Node> id1 = toNode;
				NetworkUtils.createAndAddNode(network, id1, new Coord(10.0 + totalLength - restLength + linkLength, 10.0));
				final double length = linkLength;
				final double freespeed1 = freespeed;
				final double numLanes1 = numLanes;
				NetworkUtils.createAndAddLink(network,Id.create(newLinkId, Link.class), network.getNodes().get(fromNode), network.getNodes().get(toNode), length, freespeed1, capacity * numLanes, numLanes1 );
				final double length1 = linkLength;
				final double freespeed2 = freespeed;
				final double numLanes2 = numLanes;
				NetworkUtils.createAndAddLink(network,Id.create(newLinkId + "r", Link.class), network.getNodes().get(toNode), network.getNodes().get(fromNode), length1, freespeed2, capacity * numLanes, numLanes2 );

			} else {
				final Id<Node> id1 = toNode;
				NetworkUtils.createAndAddNode(network, id1, new Coord((double) totalLength, 10.0));
				final double length = restLength;
				final double freespeed1 = freespeed;
				final double numLanes1 = numLanes;
				NetworkUtils.createAndAddLink(network,Id.create(newLinkId, Link.class), network.getNodes().get(fromNode), network.getNodes().get(toNode), length, freespeed1, capacity * numLanes, numLanes1 );
				final double length1 = restLength;
				final double freespeed2 = freespeed;
				final double numLanes2 = numLanes;
				NetworkUtils.createAndAddLink(network,Id.create(newLinkId + "r", Link.class), network.getNodes().get(toNode), network.getNodes().get(fromNode), length1, freespeed2, capacity * numLanes, numLanes2 );

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
