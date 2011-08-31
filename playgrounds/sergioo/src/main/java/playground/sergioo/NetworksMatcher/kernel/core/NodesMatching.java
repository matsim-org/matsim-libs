package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.Set;

import org.matsim.api.core.v01.network.Node;



public class NodesMatching {


	//Attributes

	protected final ComposedNode composedNodeA;
	
	protected final ComposedNode composedNodeB;


	//Methods

	protected NodesMatching(Set<Node> nodesA, Set<Node> nodesB) {
		super();
		composedNodeA = new ComposedNode(nodesA);
		composedNodeB = new ComposedNode(nodesB);
	}	

	public ComposedNode getComposedNodeA() {
		return composedNodeA;
	}

	public ComposedNode getComposedNodeB() {
		return composedNodeB;
	}


}
