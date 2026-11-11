package org.matsim.contrib.drt.passenger;

import java.util.Objects;

import org.matsim.api.core.v01.network.Link;

import com.google.common.base.Preconditions;

/**
 * A stop at which a passenger can be picked up (access) or dropped off (egress), together with the walking duration
 * between the passenger's origin/destination and this stop.
 */
public record DrtStopCandidate(Link link, double walkDuration) {
	public DrtStopCandidate {
		Objects.requireNonNull(link, "link");
		Preconditions.checkArgument(Double.isFinite(walkDuration) && walkDuration >= 0,
				"walkDuration must be finite and non-negative: %s", walkDuration);
	}
}
