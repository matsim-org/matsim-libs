package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;

/**
 * Default estimator using MATSim scoring config.
 */
public class DefaultLegScoreEstimator implements LegEstimator<ModeAvailability> {


	@Override
	public double estimate(EstimatorContext context, String mode, Leg leg, ModeAvailability option) {

		// TODO

		return 0;
	}
}
