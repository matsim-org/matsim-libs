package playground.christoph.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.network.SubLink;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.SubNode;

public class MyDijkstra extends Dijkstra{
	
	public MyDijkstra(Network network, TravelCost costFunction, TravelTime timeFunction)
	{
		super(network, costFunction, timeFunction);
	}

	public void setNetwork(Network network)
	{
		this.network = network;
	}
	
	/*
	 * The handed over fromNode and toNode are Nodes from the
	 * underlying, full Network. We have to replace them with
	 * their children in the Person´s SubNetwork.
	 */
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime)
	{
		Node newFromNode = this.network.getNodes().get(fromNode.getId());
		Node newToNode = this.network.getNodes().get(toNode.getId());

//		if (this.network.getNodes().size() == 0) System.out.println("No Nodes in SubNetwork!");
//		
//		if (fromNode == null) System.out.println("FromNode Null!");
//		if (toNode == null) System.out.println("ToNode Null!");
//		
//		if (newFromNode == null) System.out.println("NewFromNode Null!");
//		else if (newFromNode.getId() == null) System.out.println("NewFromNodeId Null!");
//		
//		if (newToNode == null) System.out.println("NewToNode Null!");
//		else if (newToNode.getId() == null) System.out.println("NewToNodeId Null!");
		
		Path path = super.calcLeastCostPath(newFromNode, newToNode, startTime);
		
		/*
		 *  The path contains SubNodes and SubLinks - we have to replace them with
		 *  their Parents.
		 */
		List<Node> newNodes = new ArrayList<Node>();
		for(Node node : path.nodes)
		{
			newNodes.add(((SubNode)node).getParentNode());
		}
		path.nodes.clear();
		path.nodes.addAll(newNodes);
		
		List<Link> newLinks = new ArrayList<Link>();
		for(Link link : path.links)
		{
			newLinks.add(((SubLink)link).getParentLink());
		}
		path.links.clear();
		path.links.addAll(newLinks);
		
		return path;
	}
	
}
