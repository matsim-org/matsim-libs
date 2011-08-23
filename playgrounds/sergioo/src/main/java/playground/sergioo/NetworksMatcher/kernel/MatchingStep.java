package playground.sergioo.NetworksMatcher.kernel;

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

	private final MatchingAlgorithm matchingAlgorithm;

	private Region region;

	private Collection<NodesMatching> nodesMatchings;
	
	
	//Methods

	public MatchingStep(MatchingAlgorithm matchingAlgorithm) {
		this.matchingAlgorithm = matchingAlgorithm;
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
			nodesMatchings = matchingAlgorithm.execute(networkA, networkB);
		else
			nodesMatchings = matchingAlgorithm.execute(networkA, networkB, region);
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
			NetworkNode networkNodeA = matching.getSubNetworkNodeA();
			networks[0].addNode(networkNodeA);
			for(Id linkId:matching.getSubNetworkNodeA().getSubNetwork().getLinks().keySet())
				networks[0].removeLink(linkId);
			for(Node node:matching.getSubNetworkNodeA().getSubNetwork().getNodes().values()) {
				for(Link link:node.getOutLinks().values())
					link.setFromNode(networkNodeA);
				for(Link link:node.getInLinks().values())
					link.setToNode(networkNodeA);
				networks[0].removeNode(node.getId());
			}
			NetworkNode networkNodeB = matching.getSubNetworkNodeB();
			networks[1].addNode(networkNodeB);
			for(Id linkId:matching.getSubNetworkNodeB().getSubNetwork().getLinks().keySet())
				networks[1].removeLink(linkId);
			for(Node node:matching.getSubNetworkNodeB().getSubNetwork().getNodes().values()) {
				for(Link link:node.getOutLinks().values())
					link.setFromNode(networkNodeB);
				for(Link link:node.getInLinks().values())
					link.setToNode(networkNodeB);
				networks[1].removeNode(node.getId());
			}
		}
		return networks;
	}

	public boolean isMatched(Node nodeA, Node nodeB) {
		for(NodesMatching nodesMatching:nodesMatchings)
			if(nodeA.getId().equals(nodesMatching.getSubNetworkNodeA().getId()) && nodeB.getId().equals(nodesMatching.getSubNetworkNodeB().getId()))
				return true;
		return false;
	}


}
