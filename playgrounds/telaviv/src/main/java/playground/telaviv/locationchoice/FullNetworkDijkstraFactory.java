package playground.telaviv.locationchoice;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;


public class FullNetworkDijkstraFactory implements LeastCostPathCalculatorFactory {

	@Override
	public LeastCostPathCalculator createPathCalculator(Network network, TravelCost travelCosts, TravelTime travelTimes) {
		return new FullNetworkDijkstra(network, travelCosts, travelTimes);
	}
}