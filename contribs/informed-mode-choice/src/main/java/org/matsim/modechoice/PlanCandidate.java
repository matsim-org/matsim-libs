package org.matsim.modechoice;

/**
 * Candidate for trip modes.
 */
public final class PlanCandidate {

	private final String[] modes;
	private double utility;

	public PlanCandidate(int trips) {
		modes = new String[trips];
		utility = 0;
	}

	// TODO: subclass of plan model ?
	// TODO: apply function ?

}
