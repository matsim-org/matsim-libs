package playground.christoph.router;

import org.matsim.core.api.experimental.network.Network;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class MyDijkstra extends Dijkstra{
	
	public MyDijkstra(Network network, TravelCost costFunction, TravelTime timeFunction)
	{
		super(network, costFunction, timeFunction);
	}

	public void setNetwork(Network network)
	{
		this.network = network;
	}
}
