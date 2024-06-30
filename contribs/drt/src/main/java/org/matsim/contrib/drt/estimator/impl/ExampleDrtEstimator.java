package org.matsim.contrib.drt.estimator.impl;

import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Estimates using a constant detour factor and waiting time.
 */
@Deprecated
public class ExampleDrtEstimator implements DrtEstimator {

    /**
	 * Detour factor for the estimate. 1.0 means no detour, 2.0 means twice the distance.
	 */
	private final double detourFactor;

	/**
	 * Constant waiting time estimate in seconds.
	 */
	private final double waitingTime;

	public ExampleDrtEstimator(double detourFactor, double waitingTime) {
        this.detourFactor = detourFactor;
		this.waitingTime = waitingTime;
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		double distance = route.getDistance() * detourFactor;
		double travelTime = route.getDirectRideTime() * detourFactor;
		return new Estimate(distance, travelTime, waitingTime, 0);
	}
}
