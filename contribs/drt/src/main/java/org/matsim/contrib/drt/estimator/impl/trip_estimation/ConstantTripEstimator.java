package org.matsim.contrib.drt.estimator.impl.trip_estimation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.OptionalTime;

public class ConstantTripEstimator implements TripEstimator {
	private final double alpha;
	private final double beta;

	public ConstantTripEstimator(double alpha, double beta) {
		this.alpha = alpha;
		this.beta = beta;
	}

	@Override
	public Tuple<Double, Double> getAlphaBetaValues(Id<Link> fromLinkId, Id<Link> toLinkId, OptionalTime departureTime) {
		return new Tuple<>(alpha, beta);
	}
}
