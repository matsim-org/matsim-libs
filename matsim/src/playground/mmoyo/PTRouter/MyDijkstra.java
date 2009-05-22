package playground.mmoyo.PTRouter;

import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.api.network.Link;

import playground.mmoyo.Validators.PathValidator;

public class MyDijkstra extends Dijkstra{
	private PathValidator pathVal = new PathValidator(); 
	
	public MyDijkstra(Network network, TravelCost costFunction, TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	
	}

	
	/*
	 * Make public the inner class Dikjstra.DijkstraNodeData?   or create a public method only to return the prev Link 
	
	protected boolean canPassLink(final Link link) {
		DijkstraNodeData fromNodeData= getData(link.getFromNode());
		Link lastLink = fromNodeData.getPrevLink();
		return pathVal.canPassLink(lastLink, link);	
	}
	*/
	
	
}
