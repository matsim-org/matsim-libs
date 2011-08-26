package playground.sergioo.NetworksMatcher.kernel;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.NetworksMatcher.kernel.ComposedNode.Types;

public class CrossingReductionAlgorithm implements MatchingAlgorithm {

	
	//Attributes
	
	private double radius;

	
	//Methods
	
	public CrossingReductionAlgorithm(double radius) {
		this.radius = radius;
	}
	
	public Set<NodesMatching> execute(Network networkA, Network networkB, double radius) {
		this.radius = radius;
		return execute(networkA, networkB);
	}

	@Override
	public Set<NodesMatching> execute(Network networkA, Network networkB) {
		Set<NodesMatching> matchings = new HashSet<NodesMatching>();
		Set<Node> alreadyReducedA = new HashSet<Node>();
		Set<Node> alreadyReducedB = new HashSet<Node>();
		for(Node node:networkA.getNodes().values()) {
			SEARCH_MATCH:
			if(((ComposedNode)node).getType().equals(Types.CROSSING) && !alreadyReducedA.contains(node)) {
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
		// TODO Auto-generated method stub
		return true;
	}
	
	private boolean matches(Set<Node> nodeSubsetA, Set<Node> nodeSubsetB) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Set<NodesMatching> execute(Network networkA, Network networkB, Region region) {
		return null;
	}
	

}
