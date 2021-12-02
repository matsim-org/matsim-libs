package org.matsim.contrib.drt.extension.alonso_mora.travel_time;

import org.matsim.api.core.v01.network.Link;

/**
 * This interface provides a generic way of calculating travel times for the
 * dispatching algorithm.
 * 
 * @author sebhoerl
 */
public interface TravelTimeEstimator {
	/**
	 * Calculates the travel time given an origin and destination link as well as a
	 * departure time. As fourth argument, an upper bound can be given. For
	 * instance, if a the travel time to a dropoff point is calculated here, it is
	 * possible to pass the latest possible arrival time to still fufill the
	 * constraint. The underlying estimator implementation might use this
	 * information to already check against the constraint using a simple heuristic
	 * before performing a detailed calculation.
	 */
	double estimateTravelTime(Link fromLink, Link toLink, double departureTime, double arrivalTimeThreshold);
}
