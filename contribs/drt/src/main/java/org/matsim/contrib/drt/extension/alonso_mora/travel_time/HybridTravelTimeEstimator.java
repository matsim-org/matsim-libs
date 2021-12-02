package org.matsim.contrib.drt.extension.alonso_mora.travel_time;

import org.matsim.api.core.v01.network.Link;

/**
 * This implementation of the travel time estimator is a combination of the
 * Euclidean distance-based estimator and the exact routing estimator.
 * Specifically, first, a routing is performed using the Euclidean estimator. If
 * this routing already exceeds the arrivalTimeThreshold of the route, the
 * respective time is returned. Hence, the EUuclidean-distance based travel time
 * should represent an optimistic estimate, which helps to early decide not to
 * perform a detailed routing. This detailed routing is performed otherwise.
 * 
 * Note that configuration parameters for both Euclidean and Routing estimator
 * will be taken into account.
 * 
 * @author sebhoerl
 */
public class HybridTravelTimeEstimator implements TravelTimeEstimator {
	static public final String TYPE = "Hybrid";
	
	private final RoutingTravelTimeEstimator routingEstimator;
	private final EuclideanTravelTimeEstimator euclideanEstimator;

	public HybridTravelTimeEstimator(RoutingTravelTimeEstimator routingEstimator,
			EuclideanTravelTimeEstimator euclideanEstimator) {
		this.routingEstimator = routingEstimator;
		this.euclideanEstimator = euclideanEstimator;
	}

	@Override
	public double estimateTravelTime(Link fromLink, Link toLink, double departureTime, double arrivalTimeThreshold) {
		double euclideanTravelTime = euclideanEstimator.estimateTravelTime(fromLink, toLink, departureTime,
				arrivalTimeThreshold);

		if (departureTime + euclideanTravelTime > arrivalTimeThreshold) {
			return euclideanTravelTime;
		}

		return routingEstimator.estimateTravelTime(fromLink, toLink, departureTime, arrivalTimeThreshold);
	}
}
