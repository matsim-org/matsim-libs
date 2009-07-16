package playground.mmoyo.PTRouter;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.mmoyo.Validators.PathValidator;

/**
 * Matsim implementation of Dijkstra algorithm adapted to the PT network Model 
 */

public class MyDijkstra extends Dijkstra{
	private PathValidator pathVal = new PathValidator(); 
	
	public MyDijkstra(final NetworkLayer network, final TravelCost costFunction, final TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}

	protected boolean canPassLink(final LinkImpl link) {
		DijkstraNodeData fromNodeData= getData(link.getFromNode());
		LinkImpl lastLink = (LinkImpl) fromNodeData.getPrevLink();
		return pathVal.canPassLink(lastLink, link);	
	}
	
}