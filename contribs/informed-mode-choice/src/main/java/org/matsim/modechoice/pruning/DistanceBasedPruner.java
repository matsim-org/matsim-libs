package org.matsim.modechoice.pruning;

import org.matsim.modechoice.PlanModel;

/**
 * Calculate the allowed threshold based on the distance of trips plus a constant factor and a trip factor.
 */
public class DistanceBasedPruner implements CandidatePruner {

	private final double c;
	private final double trip;
	private final double dist;

	/**
	 * Constructor.
	 * @param c constant
	 * @param trip trip factor
	 * @param dist distance factor
	 */
	public DistanceBasedPruner(double c, double trip, double dist) {
		this.c = c;
		this.trip = trip;
		this.dist = dist;
	}

	@Override
	public double planThreshold(PlanModel planModel) {
		return c + planModel.trips() * trip + planModel.distance() * dist / 1000;
	}

	@Override
	public double tripThreshold(PlanModel planModel, int idx) {
		return c + trip + planModel.distance(idx) * dist / 1000;
	}
}
