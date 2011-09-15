package playground.telaviv.locationchoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;
import org.matsim.core.utils.misc.Time;

public class FullNetworkDijkstra extends Dijkstra {
	
	protected Map<Id, Double> distances;
	
	public FullNetworkDijkstra(Network network, TravelCost costFunction, TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
		
		distances = new HashMap<Id, Double>();
	}

	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {

		double arrivalTime = getData(toNode).getTime();

		// now construct the path
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();

		nodes.add(0, toNode);
		Link tmpLink = getData(toNode).getPrevLink();
		if (tmpLink != null) {
			while (tmpLink.getFromNode() != fromNode) {
				links.add(0, tmpLink);
				nodes.add(0, tmpLink.getFromNode());
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
			links.add(0, tmpLink);
			nodes.add(0, tmpLink.getFromNode());
		}

		DijkstraNodeData toNodeData = getData(toNode);
		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());

		return path;
	}
	
	public void calcLeastCostTrees() {
		for (Node fromNode : this.network.getNodes().values()) {
			calcLeastCostTree(fromNode, Time.UNDEFINED_TIME);
		}
	}
	
	public void calcLeastCostTree(Node fromNode, double startTime) {

		augmentIterationId();

		PseudoRemovePriorityQueue<Node> pendingNodes = new PseudoRemovePriorityQueue<Node>(500);
		initFromNode(fromNode, null, startTime, pendingNodes);

		while (true) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) return;

			relaxNode(outNode, null, pendingNodes);
		}
	}
	
	/*package*/ void initFromNode(final Node fromNode, final Node toNode, final double startTime,
			final PseudoRemovePriorityQueue<Node> pendingNodes) {
		DijkstraNodeData data = getData(fromNode);
		visitNode(fromNode, data, pendingNodes, startTime, 0, null);
	}
	
}
