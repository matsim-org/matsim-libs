package playground.andreas.intersection.dijkstra;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.misc.Time;

public class NetworkWrapper {

	final private static Logger log = Logger.getLogger(NetworkWrapper.class);

	/**
	 * Converts a network to an inverted network. Inverted nodes are situated at
	 * the end of the real link. Inverted link attributes are copied from toLink
	 * of the real network, thus every inverted link actually starts at the
	 * location of a real node.
	 * 
	 * @param networkLayer
	 *            The real network
	 * @return The converted network
	 */
	public static NetworkLayer wrapNetwork(NetworkLayer networkLayer) {

		NetworkLayer wrappedNetwork = new NetworkLayer();
		int numberOfNodesGenerated = 0;
		int numberOfLinksGenerated = 0;

		for (Link link : networkLayer.getLinks().values()) {
			wrappedNetwork.createNode(link.getId().toString(), String.valueOf(link.getToNode().getCoord().getX()),
					String.valueOf(link.getToNode().getCoord().getY()), null);
			numberOfNodesGenerated++;
		}

		for (Node node : networkLayer.getNodes().values()) {
			for (Link inLink : node.getInLinks().values()) {
				for (Link outLink : node.getOutLinks().values()) {
					wrappedNetwork.createLink(String.valueOf(numberOfLinksGenerated),
							inLink.getId().toString(), outLink.getId().toString(),
							String.valueOf(outLink.getLength()),
							String.valueOf(outLink.getFreespeed(Time.UNDEFINED_TIME)),
							String.valueOf(outLink.getCapacity(Time.UNDEFINED_TIME)),
							String.valueOf(outLink.getLanes(Time.UNDEFINED_TIME)),
							String.valueOf(numberOfLinksGenerated), outLink.getType());
					numberOfLinksGenerated++;
				}
			}
		}

		log.info("Generated " + numberOfNodesGenerated + " Nodes and " + numberOfLinksGenerated + " Links");

		// Debug only
		// NetworkWriter myNetworkWriter = new NetworkWriter(wrappedNetwork,
		// "wrappedNetwork");
		// myNetworkWriter.write();

		return wrappedNetwork;
	}

}
