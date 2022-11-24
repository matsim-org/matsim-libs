package org.matsim.contrib.drt.estimator;

import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Interface to estimate a DRT service's detour, waiting time and costs.
 */
public interface DrtEstimator extends ControlerListener {

	/**
	 * Provide an estimate for a drt route with specific pickup and dropoff point.
	 *
	 * @param route         drt route
	 * @param departureTime estimated departure time
	 * @return An {@link Estimate} instance
	 */
	Estimate estimate(DrtRoute route, OptionalTime departureTime);


	/**
	 * Estimate for various attributes for a drt trip.
	 *
	 * @param distance    travel distance in meter
	 * @param travelTime  travel time in seconds
	 * @param waitingTIme waiting time in seconds
	 * @param fare        money, which is negative if the customer needs to pay it
	 */
	record Estimate(double distance, double travelTime, double waitingTIme, double fare) {

	}

}
