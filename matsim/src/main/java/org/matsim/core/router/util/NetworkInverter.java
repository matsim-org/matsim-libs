package org.matsim.core.router.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;

/**
 * Converts a network to an inverted network. Inverted nodes are situated at
 * the end of the real link. Inverted link attributes are copied from toLink
 * of the real network, thus every inverted link actually starts at the
 * location of a real node.
 * Each link Id of the real network corresponds to a node Id of the inverted network.
 *
 * @author aneumann
 * @author dgrether
 */
public class NetworkInverter {

	final private static Logger log = Logger.getLogger(NetworkInverter.class);

	private Network originalNetwork;

	private NetworkImpl invertedNetwork = null;

	public NetworkInverter(Network originalNet) {
		this.originalNetwork = originalNet;
	}

	public Network getInvertedNetwork() {
		if (this.invertedNetwork == null){
			invertNetwork();
		}
		return this.invertedNetwork;
	}

	private void invertNetwork(){
		this.invertedNetwork = NetworkImpl.createNetwork();
		int numberOfNodesGenerated = 0;
		int numberOfLinksGenerated = 0;

		for (Link link : this.originalNetwork.getLinks().values()) {
			this.invertedNetwork.createAndAddNode(link.getId(), link.getToNode().getCoord());
			numberOfNodesGenerated++;
		}

		for (Node node : this.originalNetwork.getNodes().values()) {
			for (Link inLink : node.getInLinks().values()) {
				for (Link outLink : node.getOutLinks().values()) {
					Link link = this.invertedNetwork.createAndAddLink(new IdImpl(numberOfLinksGenerated + 1), // start counting link ids with 1 instead of 0
							this.invertedNetwork.getNodes().get(inLink.getId()), this.invertedNetwork.getNodes().get(outLink.getId()),
							outLink.getLength(),
							outLink.getFreespeed(),
							outLink.getCapacity(),
							outLink.getNumberOfLanes());
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
	}

	public List<Link> convertInvertedNodesToLinks(List<Node> nodes) {
		List<Link> ret = new ArrayList<Link>(nodes.size());
		for (Node n : nodes){
			ret.add(this.originalNetwork.getLinks().get(n.getId()));
		}
		return ret;
	}

}
