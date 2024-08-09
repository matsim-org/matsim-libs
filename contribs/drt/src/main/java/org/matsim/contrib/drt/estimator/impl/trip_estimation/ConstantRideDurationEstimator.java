package org.matsim.contrib.drt.estimator.impl.trip_estimation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.OptionalTime;

public class ConstantRideDurationEstimator implements RideDurationEstimator {
	private final double alpha;
	private final double beta;

	public ConstantRideDurationEstimator(double alpha, double beta) {
		this.alpha = alpha;
		this.beta = beta;
	}

	@Override
	public double getEstimatedRideDuration(Id<Link> fromLinkId, Id<Link> toLinkId, OptionalTime departureTime, double directRideDuration) {
		return alpha * directRideDuration + beta;
	}
}
