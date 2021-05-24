package org.matsim.contribs.discrete_mode_choice.model.nested;

import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;

public class WrappingNestedUtilityCandidate implements NestedUtilityCandidate {
	private final UtilityCandidate delegate;
	private final Nest rootNest;

	public WrappingNestedUtilityCandidate(UtilityCandidate delegate, Nest rootNest) {
		this.delegate = delegate;
		this.rootNest = rootNest;
	}

	@Override
	public double getUtility() {
		return delegate.getUtility();
	}

	@Override
	public Nest getNest() {
		return rootNest;
	}
}