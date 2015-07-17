package org.matsim.contrib.signals.router;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.router.util.NetworkTurnInfoBuilder;

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
class NetworkInverter {

	final private static Logger log = Logger.getLogger(NetworkInverter.class);

	private Network originalNetwork;

	private NetworkImpl invertedNetwork = null;

	private Map<Id<Link>, List<TurnInfo>>  inLinkTurnInfoMap = null;

	NetworkInverter(Network originalNet, Map<Id<Link>, List<TurnInfo>>  inLinkTurnInfoMap) {
		this.originalNetwork = originalNet;
		this.inLinkTurnInfoMap = inLinkTurnInfoMap;
	}

	Network getInvertedNetwork() {
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
			this.invertedNetwork.createAndAddNode(Id.create(link.getId(), Node.class), link.getToNode().getCoord());
			numberOfNodesGenerated++;
		}

		NetworkTurnInfoBuilder turnInfoBuilder = new NetworkTurnInfoBuilder();
		for (Node node : this.originalNetwork.getNodes().values()) {
			for (Link inLink : node.getInLinks().values()) {
				for (Link outLink : node.getOutLinks().values()) {
					List<TurnInfo> turnInfos = this.inLinkTurnInfoMap.get(inLink.getId());
					TurnInfo ti = turnInfoBuilder.getTurnInfoForOutlinkId(turnInfos, outLink.getId());
					if (ti != null){
						numberOfLinksGenerated = this.createInvertedLink(inLink, outLink, numberOfLinksGenerated, ti.getModes());
					}
				}
			}
		}

		log.info("Generated " + numberOfNodesGenerated + " Nodes and " + numberOfLinksGenerated + " Links");

		// Debug only
		// NetworkWriter myNetworkWriter = new NetworkWriter(wrappedNetwork,
		// "wrappedNetwork");
		// myNetworkWriter.write();
	}

	private int createInvertedLink(Link inLink, Link outLink, int numberOfLinksGenerated, Set<String> modes){
		Link link = this.invertedNetwork.createAndAddLink(Id.create(numberOfLinksGenerated + 1, Link.class), // start counting link ids with 1 instead of 0
				this.invertedNetwork.getNodes().get(Id.create(inLink.getId(), Node.class)), this.invertedNetwork.getNodes().get(Id.create(outLink.getId(), Node.class)),
				outLink.getLength(),
				outLink.getFreespeed(),
				outLink.getCapacity(),
				outLink.getNumberOfLanes());
		link.setAllowedModes(modes);
//		log.error("created inverted link " + link.getId() + " from " + inLink.getId() + " to " + outLink.getId() + " with modes " + modes);
		((LinkImpl) link).setType(((LinkImpl) outLink).getType());
		return numberOfLinksGenerated + 1;
	}

//	public List<Link> convertInvertedNodesToLinks(List<Node> nodes) {
//		List<Link> ret = new ArrayList<Link>(nodes.size());
//		for (Node n : nodes){
//			ret.add(this.originalNetwork.getLinks().get(Id.create(n.getId(), Link.class)));
//		}
//		return ret;
//	}
	// never used. kai, jul'15

}
