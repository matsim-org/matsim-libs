package playground.christoph.router.util;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.router.DijkstraWrapper;
import playground.christoph.router.MyDijkstra;

/*
 * Basically we could also extend LeastCostPathCalculatorFactory -
 * currently we don't use methods from DijkstraFactory but maybe
 * somewhere is checked if our Class is instanceof DijkstraFactory...
 */
public class MyDijkstraFactory extends DijkstraFactory {

	@Override
	public LeastCostPathCalculator createPathCalculator(final NetworkLayer network, final TravelCost travelCosts, final TravelTime travelTimes) {
		
		Dijkstra dijkstra = new Dijkstra(network, travelCosts, travelTimes);
//		Dijkstra dijkstra = new MyDijkstra(network, travelCosts, travelTimes);
		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCosts, travelTimes, network);
		
		/*
		 *  Return only a clone (if possible)
		 *  Otherwise we could get problems when doing the
		 *  Replanning multithreaded.
		 */
		return dijkstraWrapper.clone();
	}

}
