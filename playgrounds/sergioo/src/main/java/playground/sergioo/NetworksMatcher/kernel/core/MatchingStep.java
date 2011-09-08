package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Node;



public abstract class MatchingStep extends NetworksStep {


	//Attributes
	
	protected Set<NodesMatching> nodesMatchings;
	
	
	//Methods

	public MatchingStep(String name, Region region) {
		super(name, region);
		nodesMatchings = new HashSet<NodesMatching>();
	}

	public Set<NodesMatching> getNodesMatchings() {
		return nodesMatchings;
	}

	public boolean isMatched(Node nodeA, Node nodeB) {
		for(NodesMatching nodesMatching:nodesMatchings)
			if(nodeA.getId().equals(nodesMatching.getComposedNodeA().getId()) && nodeB.getId().equals(nodesMatching.getComposedNodeB().getId()))
				return true;
		return false;
	}


}
