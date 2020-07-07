package org.matsim.contribs.discrete_mode_choice.model.nested;

import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultTripCandidate;

public class DefaultNestedTripCandidate extends DefaultTripCandidate implements NestedUtilityCandidate {
	private final Nest nest;

	public DefaultNestedTripCandidate(double utility, String mode, double duration, Nest nest) {
		super(utility, mode, duration);
		this.nest = nest;
	}

	@Override
	public Nest getNest() {
		return nest;
	}
}