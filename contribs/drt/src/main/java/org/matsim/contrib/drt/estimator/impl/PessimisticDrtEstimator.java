package org.matsim.contrib.drt.estimator.impl;

import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
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
		DrtOptimizationConstraintsSet constraints = drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
		if(constraints instanceof DrtOptimizationConstraintsSetImpl defaultConstraints) {
			double travelTime = Math.min(route.getDirectRideTime() + defaultConstraints.getMaxAbsoluteDetour(),
					route.getDirectRideTime() * defaultConstraints.getMaxTravelTimeAlpha());

			// for distance, we multiply the direct distance to the ratio between estimated travel time and direct travel time
			return new Estimate(route.getDistance() * (travelTime / route.getDirectRideTime()), travelTime, constraints.getMaxWaitTime(), 0);
		} else {
			throw new RuntimeException("Not implemented for custom constraints sets");
		}
	}
}
