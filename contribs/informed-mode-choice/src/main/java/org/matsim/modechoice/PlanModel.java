package org.matsim.modechoice;

import org.matsim.modechoice.estimators.PlanBasedLegEstimator;

/**
 * A rough model of the daily plan containing trip departure times and distances.
 * This information can be useful for rough estimate using {@link PlanBasedLegEstimator}.
 */
public final class PlanModel {

	private final double[] departues;

	private final double[] distances;

	private final String[] modes;

	// TODO: estimated distances of these trips

	public boolean hasMode(String mode, int idx) {
	}

	// can also contain the mode selection
	// list of all legs


	// TODO: custom iterator that allows accessing all modes

}
