package playground.christoph.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.network.SubNetwork;

public class MyDijkstra extends Dijkstra{
	
	public MyDijkstra(Network network, TravelCost costFunction, TravelTime timeFunction)
	{
		super(network, costFunction, timeFunction);
	}

	public void setNetwork(Network network)
	{
		this.network = network;
	}
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {
		if (!(this.network instanceof SubNetwork)) System.out.println("No SubNetwork Found!");
		return super.calcLeastCostPath(fromNode, toNode, startTime);
	}
	
}
