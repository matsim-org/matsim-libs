package playground.mmoyo.PTRouter;

import org.matsim.core.api.network.Network;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.api.network.Link;

import playground.mmoyo.Validators.PathValidator;

/**
 * Matsim implementation of Dijkstra algorithm adapted to the PT network Model 
 */

public class MyDijkstra extends Dijkstra{
	private PathValidator pathVal = new PathValidator(); 
	
	public MyDijkstra(Network network, TravelCost costFunction, TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}

	protected boolean canPassLink(final Link link) {
		DijkstraNodeData fromNodeData= getData(link.getFromNode());
		Link lastLink = fromNodeData.getPrevLink();
		return pathVal.canPassLink(lastLink, link);	
	}
	
}