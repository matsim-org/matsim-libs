package playground.sergioo.NetworksMatcher;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;


public class MatchingStep {


	//Attributes

	private Network networkA;

	private Network networkB;

	private final MatchingAlgorithm algorithm;

	private Region region;

	private Collection<NodesMatching> nodesMatchings;
	
	
	//Methods

	public MatchingStep(MatchingAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	public Network getNetworkA() {
		return networkA;
	}

	public Network getNetworkB() {
		return networkB;
	}

	public Collection<NodesMatching> getNodesMatchings() {
		return nodesMatchings;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public Network[] execute(Network networkA, Network networkB) {
		this.networkA = networkA;
		this.networkB = networkB;
		if(region == null)
			nodesMatchings = algorithm.execute(networkA, networkB);
		else
			nodesMatchings = algorithm.execute(networkA, networkB, region);
		Network[] networks = new Network[2];
		networks[0] = NetworkImpl.createNetwork();
		networks[1] = NetworkImpl.createNetwork();
		for(Node node:networkA.getNodes().values())
			networks[0].addNode(node);
		for(Node node:networkB.getNodes().values())
			networks[1].addNode(node);
		for(Link link:networkA.getLinks().values())
			networks[0].addLink(link);
		for(Link link:networkB.getLinks().values())
			networks[1].addLink(link);
		for(NodesMatching matching:nodesMatchings) {
			NodeNetwork nodeNetworkA = matching.getSubNodeNetworkA();
			networks[0].addNode(nodeNetworkA);
			for(Id linkId:matching.getSubNodeNetworkA().getSubNetwork().getLinks().keySet())
				networks[0].removeLink(linkId);
			for(Node node:matching.getSubNodeNetworkA().getSubNetwork().getNodes().values()) {
				for(Link link:node.getOutLinks().values())
					link.setFromNode(nodeNetworkA);
				for(Link link:node.getInLinks().values())
					link.setToNode(nodeNetworkA);
				networks[0].removeNode(node.getId());
			}
			NodeNetwork nodeNetworkB = matching.getSubNodeNetworkB();
			networks[1].addNode(nodeNetworkB);
			for(Id linkId:matching.getSubNodeNetworkB().getSubNetwork().getLinks().keySet())
				networks[1].removeLink(linkId);
			for(Node node:matching.getSubNodeNetworkB().getSubNetwork().getNodes().values()) {
				for(Link link:node.getOutLinks().values())
					link.setFromNode(nodeNetworkB);
				for(Link link:node.getInLinks().values())
					link.setToNode(nodeNetworkB);
				networks[1].removeNode(node.getId());
			}
		}
		return networks;
	}

	public boolean isMatched(Node nodeA, Node nodeB) {
		for(NodesMatching nodesMatching:nodesMatchings)
			if(nodeA.getId().equals(nodesMatching.getSubNodeNetworkA().getId()) && nodeB.getId().equals(nodesMatching.getSubNodeNetworkB().getId()))
				return true;
		return false;
	}


}
