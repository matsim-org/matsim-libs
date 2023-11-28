package org.matsim.contrib.drt.extension.insertion.distances;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

public class RoutingDistanceCalculator implements DistanceCalculator {
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;

	public RoutingDistanceCalculator(LeastCostPathCalculator router, TravelTime travelTime) {
		this.router = router;
		this.travelTime = travelTime;
	}

	// Currently, simply protected through synchonized. Later on would be better to
	// set this up in a more intelligent way, I'm not sure, but I guess that the
	// constraints are accesses in parallel by DRT. /sh nov'23
	@Override
	public synchronized double estimateDistance(double departureTime, Link fromLink, Link toLink) {
		VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTime);
		return VrpPaths.calcDistance(path);
	}
}
