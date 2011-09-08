package playground.sergioo.NetworksMatcher.kernel;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.NetworksMatcher.kernel.core.MatchingComposedNetwork;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode;
import playground.sergioo.NetworksMatcher.kernel.core.MatchingStep;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.NetworksMatcher.kernel.core.Region;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode.Types;

public class CrossingMatchingStep extends MatchingStep {


	//Attributes

	private double radius;
	
	//Methods

	public CrossingMatchingStep(Region region, double radius, double minAngle) {
		super("Crossing matching step", region);
		nodesMatchings = new HashSet<NodesMatching>();
		this.radius = radius;
		ComposedNode.radius = radius;
		IncidentLinksNodesMatching.minAngle = minAngle;
		networkSteps.add(new CreateDirectGraphStep(region));
		internalStepPosition = 1;
	}

	public void setRadius(double radius) {
		this.radius = radius;
		ComposedNode.radius = radius;
	}

	public void setMinAngle(double minAngle) {
		IncidentLinksNodesMatching.minAngle = minAngle;
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

	@Override
	protected MatchingComposedNetwork[] execute() {
		MatchingComposedNetwork[] networks = new MatchingComposedNetwork[] {networkA.clone(), networkB.clone()};
		Set<Node> alreadyReducedA = new HashSet<Node>();
		Set<Node> alreadyReducedB = new HashSet<Node>();
		for(Node node:networks[0].getNodes().values())
			SEARCH_MATCH:
			if(region.isInside(node) && ((ComposedNode)node).getType().equals(Types.CROSSING) && !alreadyReducedA.contains(node)) {
				Set<Node> nearestNodesToA = new HashSet<Node>();
				for(Node nodeA:networks[0].getNodes().values())
					if(((ComposedNode)nodeA).getType().equals(Types.CROSSING) && ((CoordImpl)node.getCoord()).calcDistance(nodeA.getCoord())<radius && !alreadyReducedA.contains(nodeA))
						nearestNodesToA.add(nodeA);
				Set<Node> nearestNodesToB = new HashSet<Node>();
				for(Node nodeB:networks[1].getNodes().values())
					if(((ComposedNode)nodeB).getType().equals(Types.CROSSING) && ((CoordImpl)node.getCoord()).calcDistance(nodeB.getCoord())<radius && !alreadyReducedB.contains(nodeB))
						nearestNodesToB.add(nodeB);
				for(int n=nearestNodesToA.size(); n>=1; n--) {
					Set<Set<Node>> nodesSubsetsA = getSubsetsOfSize(nearestNodesToA, n);
					for(Set<Node> nodeSubsetA:nodesSubsetsA)
						//if(new ComposedNode(nodeSubsetA).isConnected())
							for(int m=nearestNodesToB.size(); m>=1; m--) {
								Set<Set<Node>> nodesSubsetsB = getSubsetsOfSize(nearestNodesToB, m);
								for(Set<Node> nodeSubsetB:nodesSubsetsB)
									if(true/*new ComposedNode(nodeSubsetB).isConnected()*/) {
										IncidentLinksNodesMatching nodesMatching = new IncidentLinksNodesMatching(nodeSubsetA, nodeSubsetB);
										if(nodesMatching.linksAnglesMatches()) {
											nodesMatchings.add(nodesMatching);
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
		networkSteps.add(new CrossingReductionStep(region, nodesMatchings));
		networkSteps.add(new EdgeDeletionStep(region));
		return networks;
	}


}
