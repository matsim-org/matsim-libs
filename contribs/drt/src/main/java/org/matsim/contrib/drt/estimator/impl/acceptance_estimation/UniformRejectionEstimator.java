package org.matsim.contrib.drt.estimator.impl.acceptance_estimation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.OptionalTime;

public class UniformRejectionEstimator implements RejectionRateEstimator {
	private final double probabilityOfRejection;

	public UniformRejectionEstimator(double probabilityOfRejection) {
		this.probabilityOfRejection = probabilityOfRejection;
	}

	@Override
	public double getEstimatedProbabilityOfRejection(Id<Link> fromLinkId, Id<Link> toLinkId, OptionalTime departureTime) {
		return probabilityOfRejection;
	}
}
