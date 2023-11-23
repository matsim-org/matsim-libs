package org.matsim.modechoice.pruning;

import org.matsim.modechoice.PlanModel;

/**
 * Calculate the allowed threshold based on the distance of trips plus a constant factor.
 */
public class DistanceBasedPruner implements CandidatePruner {

	private final double c;
	private final double dist;

	/**
	 * Constructor.
	 * @param c constant
	 * @param dist distance factor
	 */
	public DistanceBasedPruner(double c, double dist) {
		this.c = c;
		this.dist = dist;
	}

	@Override
	public double planThreshold(PlanModel planModel) {
		return c + planModel.distance() * dist / 1000;
	}

	@Override
	public double tripThreshold(PlanModel planModel, int idx) {
		return c  + planModel.distance(idx) * dist / 1000;
	}
}
