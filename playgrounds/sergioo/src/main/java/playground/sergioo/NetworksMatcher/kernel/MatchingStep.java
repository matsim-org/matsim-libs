package playground.sergioo.NetworksMatcher.kernel;

import java.util.Set;

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

	private Set<NodesMatching> nodesMatchings;
	
	
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

	public Set<NodesMatching> getNodesMatchings() {
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
		for(Node node:networkA.getNodes().values()) {
			ComposedNode newNode = new ComposedNode(((ComposedNode)node).getNodes());
			networks[0].addNode(newNode);
		}
		for(Node node:networkB.getNodes().values()) {
			ComposedNode newNode = new ComposedNode(((ComposedNode)node).getNodes());
			networks[1].addNode(newNode);
		}
		for(Link link:networkA.getLinks().values()) {
			ComposedLink composedLink = new ComposedLink(link, networks[0]);
			composedLink.setFromNode(networks[0].getNodes().get(composedLink.getFromNode().getId()));
			composedLink.setToNode(networks[0].getNodes().get(composedLink.getToNode().getId()));
			composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
			networks[0].addLink(composedLink);
		}
		for(Link link:networkB.getLinks().values()) {
			ComposedLink composedLink = new ComposedLink(link, networks[1]);
			composedLink.setFromNode(networks[1].getNodes().get(composedLink.getFromNode().getId()));
			composedLink.setToNode(networks[1].getNodes().get(composedLink.getToNode().getId()));
			composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
			networks[1].addLink(composedLink);
		}
		for(NodesMatching matching:nodesMatchings) {
			ComposedNode composedNodeA = matching.getComposedNodeA();
			if(composedNodeA.getNodes().size()>1) {
				networks[0].addNode(composedNodeA);
				for(Node node:composedNodeA.getNodes()) {
					networks[0].removeNode(node.getId());
					for(Link link:node.getInLinks().values()) {
						boolean insideLink = false;
						for(Node node2:composedNodeA.getNodes())
							if(link.getFromNode().getId().equals(node2.getId()))
								insideLink = true;
						networks[0].removeLink(link.getId());
						if(!insideLink) {
							ComposedLink composedLink = new ComposedLink(link, networks[0]);
							Node fromNode = networks[0].getNodes().get(composedLink.getFromNode().getId());
							if(fromNode==null)
								fromNode = networks[0].getNodes().get(((ComposedNode)composedLink.getFromNode()).getContainerNode().getId());
							composedLink.setFromNode(fromNode);
							composedLink.setToNode(composedNodeA);
							composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
							networks[0].addLink(composedLink);
						}
					}
					for(Link link:node.getOutLinks().values()) {
						boolean insideLink = false;
						for(Node node2:composedNodeA.getNodes())
							if(link.getToNode().getId().equals(node2.getId()))
								insideLink = true;
						networks[0].removeLink(link.getId());
						if(!insideLink) {
							ComposedLink composedLink = new ComposedLink(link, networks[0]);
							composedLink.setFromNode(composedNodeA);
							Node toNode = networks[0].getNodes().get(composedLink.getToNode().getId());
							if(toNode==null)
								toNode = networks[0].getNodes().get(((ComposedNode)composedLink.getToNode()).getContainerNode().getId());
							composedLink.setToNode(toNode);
							composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
							networks[0].addLink(composedLink);
						}
					}			
				}
			}
			ComposedNode composedNodeB = matching.getComposedNodeB();
			if(composedNodeB.getNodes().size()>1) {
				networks[1].addNode(composedNodeB);
				for(Node node:composedNodeB.getNodes()) {
					networks[1].removeNode(node.getId());
					for(Link link:node.getInLinks().values()) {
						boolean insideLink = false;
						for(Node node2:composedNodeB.getNodes())
							if(link.getFromNode().getId().equals(node2.getId()))
								insideLink = true;
						networks[1].removeLink(link.getId());
						if(!insideLink) {
							ComposedLink composedLink = new ComposedLink(link, networks[1]);
							Node fromNode = networks[1].getNodes().get(composedLink.getFromNode().getId());
							if(fromNode==null)
								fromNode = networks[1].getNodes().get(((ComposedNode)composedLink.getFromNode()).getContainerNode().getId());
							composedLink.setFromNode(fromNode);
							composedLink.setToNode(composedNodeB);
							composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
							networks[1].addLink(composedLink);
						}
					}
					for(Link link:node.getOutLinks().values()) {
						boolean insideLink = false;
						for(Node node2:composedNodeB.getNodes())
							if(link.getToNode().getId().equals(node2.getId()))
								insideLink = true;
						networks[1].removeLink(link.getId());
						if(!insideLink) {
							ComposedLink composedLink = new ComposedLink(link, networks[1]);
							composedLink.setFromNode(composedNodeB);
							Node toNode = networks[1].getNodes().get(composedLink.getToNode().getId());
							if(toNode==null)
								toNode = networks[1].getNodes().get(((ComposedNode)composedLink.getToNode()).getContainerNode().getId());
							composedLink.setToNode(toNode);
							composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
							networks[1].addLink(composedLink);
						}
					}			
				}
			}
		}
		return networks;
	}

	public boolean isMatched(Node nodeA, Node nodeB) {
		for(NodesMatching nodesMatching:nodesMatchings)
			if(nodeA.getId().equals(nodesMatching.getComposedNodeA().getId()) && nodeB.getId().equals(nodesMatching.getComposedNodeB().getId()))
				return true;
		return false;
	}


}
