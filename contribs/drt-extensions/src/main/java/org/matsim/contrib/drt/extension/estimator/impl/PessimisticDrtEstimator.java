package org.matsim.contrib.drt.extension.estimator.impl;

import org.matsim.contrib.drt.extension.estimator.DrtInitialEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Uses the upper bounds from config for the initial estimate.
 */
public class PessimisticDrtEstimator implements DrtInitialEstimator {
	private final DrtConfigGroup drtConfig;

	public PessimisticDrtEstimator(DrtConfigGroup drtConfig) {
		this.drtConfig = drtConfig;
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		// If not estimates are present, use travel time alpha as detour
		// beta is not used, because estimates are supposed to be minimums and not worst cases
		double travelTime = Math.min(route.getDirectRideTime() + drtConfig.maxAbsoluteDetour,
			route.getDirectRideTime() * drtConfig.maxTravelTimeAlpha);

		double fare = 0;
		if (drtConfig.getDrtFareParams().isPresent()) {
			DrtFareParams fareParams = drtConfig.getDrtFareParams().get();
			fare = fareParams.distanceFare_m * route.getDistance()
				+ fareParams.timeFare_h * route.getDirectRideTime() / 3600.0
				+ fareParams.baseFare;

			fare = Math.max(fare, fareParams.minFarePerTrip);
		}

		// for distance, also use the max travel time alpha
		return new Estimate(route.getDistance() * drtConfig.maxTravelTimeAlpha, travelTime, drtConfig.maxWaitTime, fare, 0);
	}

}
