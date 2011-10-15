package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;



public class NodesMatching {

	//Constants
	
	private static final String SEPARATOR_NODES = ",-,";
	
	private static final String SEPARATOR_AB = "@@@";
	

	//Attributes

	protected final ComposedNode composedNodeA;
	
	protected final ComposedNode composedNodeB;


	//Methods

	public NodesMatching(Set<Node> nodesA, Set<Node> nodesB) {
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
	
	public String toString() {
		String line = "";
		for(Node node:composedNodeA.getNodes())
			line+=node.getId().toString()+SEPARATOR_NODES;
		line+=SEPARATOR_AB;
		for(Node node:composedNodeB.getNodes())
			line+=node.getId().toString()+SEPARATOR_NODES;
		return line;
	}
	
	public static NodesMatching parseNodesMatching(String line, Network networkA, Network networkB) {
		Set<Node> nodesA = new HashSet<Node>();
		Set<Node> nodesB = new HashSet<Node>();
		String[] ab = line.split(SEPARATOR_AB);
		for(String nodeId:ab[0].split(SEPARATOR_NODES))
			if(!nodeId.equals(""))
				nodesA.add(networkA.getNodes().get(new IdImpl(nodeId)));
		for(String nodeId:ab[1].split(SEPARATOR_NODES))
			if(!nodeId.equals(""))
				nodesB.add(networkB.getNodes().get(new IdImpl(nodeId)));
		return new NodesMatching(nodesA, nodesB);
	}

}
