package org.matsim.contrib.drt.estimator.impl.waiting_time_estimation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.OptionalTime;

public class ConstantWaitingTimeEstimator implements WaitingTimeEstimator {
	private final double typicalWaitingTime;

	public ConstantWaitingTimeEstimator(double typicalWaitingTime) {
		this.typicalWaitingTime = typicalWaitingTime;
	}

	@Override
	public double estimateWaitTime(Id<Link> fromLinkId, Id<Link> toLinkId, OptionalTime departureTime) {
		return typicalWaitingTime;
	}
}
