package org.matsim.contrib.analysis.vsp.traveltimedistance;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.Tuple;

/**
 * Validator that performs the routing on a time variant network.
 */
public class NetworkRouteValidator implements TravelTimeDistanceValidator {

	private final LeastCostPathCalculator router;
	private final SearchableNetwork network;

	public NetworkRouteValidator(Network network) {

		SpeedyALTFactory factory = new SpeedyALTFactory();

		FreeSpeedTravelTime tt = new FreeSpeedTravelTime();
		this.router = factory.createPathCalculator(network, new OnlyTimeDependentTravelDisutility(tt), tt);
		this.network = (SearchableNetwork) network;

	}


	@Override
	public Tuple<Double, Double> getTravelTime(Coord fromCoord, Coord toCoord, double departureTime, String tripId) {

		LeastCostPathCalculator.Path path = router.calcLeastCostPath(network.getNearestNode(fromCoord), network.getNearestNode(toCoord), departureTime, null, null);
		double length = path.links.stream().mapToDouble(Link::getLength).sum();
		return Tuple.of(path.travelTime, length);
	}

}
