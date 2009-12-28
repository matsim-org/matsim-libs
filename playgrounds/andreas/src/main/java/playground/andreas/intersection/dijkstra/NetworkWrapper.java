package playground.andreas.intersection.dijkstra;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;

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
	public static NetworkLayer wrapNetwork(Network networkLayer) {

		NetworkLayer wrappedNetwork = new NetworkLayer();
		int numberOfNodesGenerated = 0;
		int numberOfLinksGenerated = 0;

		for (Link link : networkLayer.getLinks().values()) {
			wrappedNetwork.createAndAddNode(link.getId(), link.getToNode().getCoord());
			numberOfNodesGenerated++;
		}

		for (Node node : networkLayer.getNodes().values()) {
			for (Link inLink : node.getInLinks().values()) {
				for (Link outLink : node.getOutLinks().values()) {
					Link link = wrappedNetwork.createAndAddLink(new IdImpl(numberOfLinksGenerated),
							wrappedNetwork.getNode(inLink.getId()), wrappedNetwork.getNode(outLink.getId().toString()),
							outLink.getLength(),
							outLink.getFreespeed(Time.UNDEFINED_TIME),
							outLink.getCapacity(Time.UNDEFINED_TIME),
							outLink.getNumberOfLanes(Time.UNDEFINED_TIME));
					((LinkImpl) link).setType(((LinkImpl) outLink).getType());
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
