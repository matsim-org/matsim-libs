package org.matsim.contrib.drt.passenger;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;

/**
 * Stop candidates of a {@link DrtRequest}: the walking durations to/from the request's own stops (its from/to link)
 * and alternative access (pickup) and egress (dropoff) stops. The request's own stops are not repeated in the lists.
 * <p>
 * The insertion search turns the combinations into request variants, see
 * {@code org.matsim.contrib.drt.optimizer.insertion.RequestVariants}.
 */
public record DrtStopCandidates(double accessWalkDuration, double egressWalkDuration,
								List<DrtStopCandidate> alternativeAccess, List<DrtStopCandidate> alternativeEgress) {
	public DrtStopCandidates {
		Preconditions.checkArgument(Double.isFinite(accessWalkDuration) && accessWalkDuration >= 0,
				"accessWalkDuration must be finite and non-negative: %s", accessWalkDuration);
		Preconditions.checkArgument(Double.isFinite(egressWalkDuration) && egressWalkDuration >= 0,
				"egressWalkDuration must be finite and non-negative: %s", egressWalkDuration);
		alternativeAccess = List.copyOf(Objects.requireNonNull(alternativeAccess, "alternativeAccess"));
		alternativeEgress = List.copyOf(Objects.requireNonNull(alternativeEgress, "alternativeEgress"));
	}
}
