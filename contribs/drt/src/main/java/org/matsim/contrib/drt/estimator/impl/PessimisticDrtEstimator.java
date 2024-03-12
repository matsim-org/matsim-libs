package org.matsim.contrib.drt.estimator.impl;

import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Uses the upper bounds from config for the initial estimate.
 */
public class PessimisticDrtEstimator implements DrtEstimator {
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

		// for distance, also use the max travel time alpha
		return new Estimate(route.getDistance() * drtConfig.maxTravelTimeAlpha, travelTime, drtConfig.maxWaitTime, 0);
	}

}
