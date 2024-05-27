package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;

/**
 * Default estimator using MATSim scoring config.
 */
public class DefaultLegScoreEstimator implements LegEstimator {


	@Override
	public double estimate(EstimatorContext context, String mode, Leg leg, ModeAvailability option) {

		ModeUtilityParameters params = context.scoring.modeParams.get(mode);

		double dist = leg.getRoute().getDistance();
		double tt = leg.getTravelTime().orElse(0);

		return params.constant +
				params.marginalUtilityOfDistance_m * dist +
				params.marginalUtilityOfTraveling_s * tt +
				context.scoring.marginalUtilityOfMoney * params.monetaryDistanceCostRate * dist;
	}
}
