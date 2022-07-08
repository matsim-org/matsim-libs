package org.matsim.modechoice.search;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.*;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.TripEstimator;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds fields required for injection.
 */
abstract class AbstractCandidateGenerator implements CandidateGenerator {

	@Inject
	protected Map<String, LegEstimator<?>> legEstimators;

	@Inject
	protected Map<String, TripEstimator<?>> tripEstimator;

	@Inject
	protected Map<String, FixedCostsEstimator<?>> fixedCosts;

	@Inject
	protected ScoringParametersForPerson params;

	@Inject
	protected EstimateRouter router;

	@Inject
	protected Set<TripConstraint<?>> constraints;

	protected final InformedModeChoiceConfigGroup config;
	protected final Map<String, ModeOptions<?>> options;

	protected AbstractCandidateGenerator(InformedModeChoiceConfigGroup config, Map<String, ModeOptions<?>> options) {
		this.config = config;
		this.options = options;

		for (String mode : config.getModes()) {

			if (!options.containsKey(mode))
				throw new IllegalArgumentException(String.format("No estimators configured for mode %s", mode));
		}
	}


	/**
	 * Calculate the estimates for all options.
	 */
	protected final void calculateEstimates(EstimatorContext context, PlanModel planModel, Map<String, List<Combination>> options) {

		// estimates only consider the leg score by using a certain mode
		// early or late arrival can also have an effect on the activity scores which is not considered here

		for (Map.Entry<String, List<Combination>> e : options.entrySet()) {

			for (Combination c : e.getValue()) {

				double[] values = c.getEstimates();
				double[] tValues = c.getTripEstimates();

				// Collect all estimates
				for (int i = 0; i < planModel.trips(); i++) {

					List<Leg> legs = planModel.getLegs(c.getMode(), i);

					// This mode could not be applied
					if (legs == null) {
						values[i] = Double.NEGATIVE_INFINITY;
						continue;
					}

					TripEstimator<Enum<?>> tripEst = (TripEstimator<Enum<?>>) tripEstimator.get(c.getMode());

					// some options may produce equivalent options, but are re-estimated
					// however, the more expensive computation is routing and only done once

					double estimate = 0;
					if (tripEst != null) {
						MinMaxEstimate minMax = tripEst.estimate(context, c.getMode(), planModel, legs, c.getOption());
						double tripEstimate = c.isMin() ? minMax.getMin() : minMax.getMax();

						// Only store if required
						if (tValues != null)
							tValues[i] = tripEstimate;

						estimate += tripEstimate;
					}

					for (Leg leg : legs) {
						String legMode = leg.getMode();

						// Already scored with the trip estimator
						if (tripEst != null && legMode.equals(c.getMode()))
							continue;

						LegEstimator<Enum<?>> legEst = (LegEstimator<Enum<?>>) legEstimators.get(legMode);

						if (legEst == null)
							throw new IllegalStateException("No leg estimator defined for mode: " + legMode);

						estimate += legEst.estimate(context, legMode, leg, c.getOption());
					}

					values[i] = estimate;
				}
			}
		}
	}
}
