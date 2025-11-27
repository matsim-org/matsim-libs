package org.matsim.contrib.drt.estimator.impl.acceptance_estimation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.OptionalTime;

public interface RejectionRateEstimator {
	double getEstimatedProbabilityOfRejection(Id<Link> fromLinkId, Id<Link> toLinkId, OptionalTime departureTime);
}
