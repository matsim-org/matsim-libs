package playground.sergioo.NetworksMatcher.kernel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.NetworksMatcher.kernel.ComposedNode.Types;

public class CrossingReductionAlgorithm implements MatchingAlgorithm {

	
	//Attributes
	
	private double radius;

	
	//Methods
	
	public CrossingReductionAlgorithm(double radius) {
		this.radius = radius;
	}
	
	public void setRadius(double radius) {
		this.radius = radius;
	}

	@Override
	public Set<NodesMatching> execute(Network networkA, Network networkB) {
		return execute(networkA, networkB, new InfiniteRegion());
	}

	private Set<Set<Node>> getSubsetsOfSize(Set<Node> nodesSet, int n) {
		Set<Set<Node>> nodesSubsets = new HashSet<Set<Node>>();
		getSubsetsOfSize(nodesSet, n, new HashSet<Node>(), nodesSubsets);
		return nodesSubsets;
	}

	private void getSubsetsOfSize(Set<Node> nodesSet, int n, Set<Node> nodesSubset, Set<Set<Node>> nodesSubsets) {
		if(n==0)
			nodesSubsets.add(nodesSubset);
		else {
			Set<Node> newNodeSet = new HashSet<Node>(nodesSet);
			for(Node node:nodesSet) {
				nodesSubset.add(node);
				newNodeSet.remove(node);
				getSubsetsOfSize(newNodeSet, n-1, nodesSubset, nodesSubsets);
			}
		}
	}

	private boolean isConnected(Set<Node> nodeSubset) {
		Set<Node> nodes = new HashSet<Node>();
		fillNodes(nodeSubset.iterator().next(), nodes, nodeSubset);
		return nodeSubset.size()==nodes.size();
	}
	
	private void fillNodes(Node node, Set<Node> nodes, Set<Node> allNodes) {
		nodes.add(node);
		for(Link link:node.getInLinks().values())
			if(allNodes.contains(link.getToNode()) && !nodes.contains(link.getToNode()))
				fillNodes(link.getFromNode(), nodes, allNodes);
		for(Link link:node.getOutLinks().values())
			if(allNodes.contains(link.getToNode()) && !nodes.contains(link.getToNode()))
				fillNodes(link.getToNode(), nodes, allNodes);
	}

	private boolean matches(Set<Node> nodeSubsetA, Set<Node> nodeSubsetB) {
		// TODO Auto-generated method stub
		return true;
	}
	
	private List<Link> getIncidentLinks(Set<Node> nodeSubset, Network network) {
		return null;
		/*List<Link> incidentLinks = new ArrayList<Link>();
		ComposedNode composedNodeA = new ComposedNode(nodeSubset);
		if(composedNodeA.getNodes().size()>1)
			for(Node node:composedNodeA.getNodes()) {
				for(Link link:node.getInLinks().values()) {
					boolean insideLink = false;
					for(Node node2:composedNodeA.getNodes())
						if(link.getFromNode().getId().equals(node2.getId()))
							insideLink = true;
					if(!insideLink) {
						link.setToNode(composedNodeA);
						incidentLinks.add(new NetworkFactoryImpl(network));
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
		}*/
	}

	@Override
	public Set<NodesMatching> execute(Network networkA, Network networkB, Region region) {
		Set<NodesMatching> matchings = new HashSet<NodesMatching>();
		Set<Node> alreadyReducedA = new HashSet<Node>();
		Set<Node> alreadyReducedB = new HashSet<Node>();
		for(Node node:networkA.getNodes().values()) {
			SEARCH_MATCH:
			if(region.isInside(node) && ((ComposedNode)node).getType().equals(Types.CROSSING) && !alreadyReducedA.contains(node)) {
				Set<Node> nearestNodesToA = new HashSet<Node>();
				for(Node nodeA:networkA.getNodes().values())
					if(((ComposedNode)nodeA).getType().equals(Types.CROSSING) && ((CoordImpl)node.getCoord()).calcDistance(nodeA.getCoord())<radius && !alreadyReducedA.contains(nodeA))
						nearestNodesToA.add(nodeA);
				Set<Node> nearestNodesToB = new HashSet<Node>();
				for(Node nodeB:networkB.getNodes().values())
					if(((ComposedNode)nodeB).getType().equals(Types.CROSSING) && ((CoordImpl)node.getCoord()).calcDistance(nodeB.getCoord())<radius && !alreadyReducedB.contains(nodeB))
						nearestNodesToB.add(nodeB);
				for(int n=1; n<=nearestNodesToA.size(); n++) {
					Set<Set<Node>> nodesSubsetsA = getSubsetsOfSize(nearestNodesToA, n);
					for(Set<Node> nodeSubsetA:nodesSubsetsA)
						if(isConnected(nodeSubsetA))
							for(int m=1; m<=nearestNodesToB.size(); m++) {
								Set<Set<Node>> nodesSubsetsB = getSubsetsOfSize(nearestNodesToB, m);
								for(Set<Node> nodeSubsetB:nodesSubsetsB)
									if(isConnected(nodeSubsetB))
										if(matches(nodeSubsetA, nodeSubsetB)) {
											matchings.add(new NodesMatching(nodeSubsetA, nodeSubsetB));
											for(Node nodeMatched:nodeSubsetA)
												alreadyReducedA.add(nodeMatched);
											for(Node nodeMatched:nodeSubsetB)
												alreadyReducedB.add(nodeMatched);
											break SEARCH_MATCH;
										}
							}
				}
			}
		}
		return matchings;
	}
	

}
