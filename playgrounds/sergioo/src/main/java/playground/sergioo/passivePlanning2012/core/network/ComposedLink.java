package playground.sergioo.passivePlanning2012.core.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;

public class ComposedLink extends LinkImpl {

	//Attributes
	private final Node startNode;
	private final Node endNode;

	//Methods
	protected ComposedLink(Id<Link> id, Node from, Node to, Network network, double length, double freespeed, double capacity, double lanes, Node startNode, Node endNode) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
		this.startNode = startNode;
		this.endNode = endNode;
	}
	public Node getStartNode() {
		return startNode;
	}
	public Node getEndNode() {
		return endNode;
	}

}

	
