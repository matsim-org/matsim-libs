package org.matsim.contrib.drt.extension.estimator.impl;

import org.matsim.contrib.drt.extension.estimator.DrtInitialEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Estimates using a constant detour factor and waiting time.
 */
public class ConstantDrtEstimator implements DrtInitialEstimator {

	private final DrtConfigGroup drtConfig;

	/**
	 * Detour factor for the estimate. 1.0 means no detour, 2.0 means twice the distance.
	 */
	private final double detourFactor;

	/**
	 * Constant waiting time estimate in seconds.
	 */
	private final double waitingTime;

	public ConstantDrtEstimator(DrtConfigGroup drtConfig, double detourFactor, double waitingTime) {
		this.drtConfig = drtConfig;
		this.detourFactor = detourFactor;
		this.waitingTime = waitingTime;
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {

		double distance = route.getDistance() * detourFactor;
		double travelTime = route.getDirectRideTime() * detourFactor;

		double fare = 0;
		if (drtConfig.getDrtFareParams().isPresent()) {
			DrtFareParams fareParams = drtConfig.getDrtFareParams().get();
			fare = fareParams.distanceFare_m * distance
				+ fareParams.timeFare_h * travelTime / 3600.0
				+ fareParams.baseFare;

			fare = Math.max(fare, fareParams.minFarePerTrip);
		}

		return new Estimate(distance, travelTime, waitingTime, fare, 0);
	}
}
