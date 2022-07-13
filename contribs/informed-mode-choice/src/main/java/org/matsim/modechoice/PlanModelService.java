package org.matsim.modechoice;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.TripEstimator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A service for working with {@link PlanModel} and creating estimates.
 */
public final class PlanModelService {

	@Inject
	private Map<String, LegEstimator<?>> legEstimators;

	@Inject
	private Map<String, TripEstimator<?>> tripEstimator;

	private final InformedModeChoiceConfigGroup config;
	private final Map<String, ModeOptions<?>> options;

	@Inject
	private PlanModelService(InformedModeChoiceConfigGroup config, Map<String, ModeOptions<?>> options) {
		this.config = config;
		this.options = options;

		for (String mode : config.getModes()) {

			if (!options.containsKey(mode))
				throw new IllegalArgumentException(String.format("No estimators configured for mode %s", mode));
		}
	}

	/**
	 * Initialized {@link ModeEstimate} for all available options in the {@link PlanModel}. No routing or estimation is performed yet.
	 */
	public void initEstimates(EstimatorContext context, PlanModel planModel) {

		for (String mode : config.getModes()) {

			ModeOptions<Enum<?>> t = (ModeOptions<Enum<?>>) this.options.get(mode);

			List<Enum<?>> modeOptions = t.get(planModel.getPerson());

			List<ModeEstimate> c = new ArrayList<>();

			for (Enum<?> modeOption : modeOptions) {
				TripEstimator<Enum<?>> te = (TripEstimator<Enum<?>>) tripEstimator.get(mode);

				boolean usable = t.allowUsage(modeOption);

				// Check if min estimate is needed
				if (te != null && te.providesMinEstimate(context, mode, modeOption)) {

					c.add(new ModeEstimate(mode, modeOption, planModel.trips(), usable, true, false));
					c.add(new ModeEstimate(mode, modeOption, planModel.trips(), usable, true, true));

				} else {

					c.add(new ModeEstimate(mode, modeOption, planModel.trips(), usable, false, false));

				}
			}

			planModel.putEstimate(mode, c);
		}
	}

	/**
	 * Calculate the estimates for all options. Note that plan model has to be routed before computing estimates.
	 */
	public void calculateEstimates(EstimatorContext context, PlanModel planModel) {

		// estimates only consider the leg score by using a certain mode
		// early or late arrival can also have an effect on the activity scores which is not considered here

		for (Map.Entry<String, List<ModeEstimate>> e : planModel.getEstimates().entrySet()) {

			for (ModeEstimate c : e.getValue()) {

				if (!c.isUsable())
					continue;

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

					// some options may produce equivalent results, but are re-estimated
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
