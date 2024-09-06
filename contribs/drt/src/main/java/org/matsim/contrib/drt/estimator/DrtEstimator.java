package org.matsim.contrib.drt.estimator;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.Attributable;

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
	 * @param rideDistance  travel distance in meter
	 * @param rideTime      ride time in seconds
	 * @param waitingTime   waiting time in seconds
//	 * @param fare          money, which is negative if the customer needs to pay it
	 * @param rejectionRate probability of a trip being rejected
	 */
	record Estimate(double rideDistance, double rideTime, double waitingTime, double rejectionRate) {

	}

	/**
	 * Write estimate information into the leg attributes.
	 */
	static void setEstimateAttributes(Leg leg, Estimate estimate) {
		leg.getAttributes().putAttribute("est_ride_time", estimate.rideTime());
		leg.getAttributes().putAttribute("est_ride_distance", estimate.rideDistance());
		leg.getAttributes().putAttribute("est_wait_time", estimate.waitingTime());
	}

}
