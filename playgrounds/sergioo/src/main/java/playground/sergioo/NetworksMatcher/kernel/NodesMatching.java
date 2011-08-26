package playground.sergioo.NetworksMatcher.kernel;

import java.util.Set;

import org.matsim.api.core.v01.network.Node;


public class NodesMatching {


	//Attributes

	private final ComposedNode composedNodeA;
	
	private final ComposedNode composedNodeB;


	//Methods

	protected NodesMatching(Set<Node> nodesA, Set<Node> nodesB) {
		super();
		this.composedNodeA = new ComposedNode(nodesA);
		this.composedNodeB = new ComposedNode(nodesB);
	}

	public ComposedNode getComposedNodeA() {
		return composedNodeA;
	}

	public ComposedNode getComposedNodeB() {
		return composedNodeB;
	}


}
