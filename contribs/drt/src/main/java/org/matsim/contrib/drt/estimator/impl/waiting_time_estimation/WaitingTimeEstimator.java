package org.matsim.contrib.drt.estimator.impl.waiting_time_estimation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.OptionalTime;

public interface WaitingTimeEstimator {
	double estimateWaitTime(Id<Link> fromLinkId, Id<Link> toLinkId, OptionalTime departureTime);
}
