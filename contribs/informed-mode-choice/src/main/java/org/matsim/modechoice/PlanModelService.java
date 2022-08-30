package org.matsim.modechoice;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.ActivityEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.TripEstimator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A service for working with {@link PlanModel} and creating estimates.
 */
@SuppressWarnings("unchecked")
public final class PlanModelService {

	@Inject
	private Map<String, LegEstimator<?>> legEstimators;

	@Inject
	private Map<String, TripEstimator<?>> tripEstimator;

	@Inject
	private Set<TripConstraint<?>> constraints;

	@Inject
	private ActivityEstimator actEstimator;

	@Inject
	private TimeInterpretation timeInterpretation;

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
	 * Allowed modes of a plan.
	 */
	public List<String> allowedModes(PlanModel planModel) {

		List<String> modes = new ArrayList<>();

		for (String mode : config.getModes()) {

			ModeOptions<Enum<?>> t = (ModeOptions<Enum<?>>) this.options.get(mode);
			List<Enum<?>> modeOptions = t.get(planModel.getPerson());

			for (Enum<?> modeOption : modeOptions) {
				boolean usable = t.allowUsage(modeOption);

				if (usable) {
					modes.add(mode);
					break;
				}
			}
		}
		return modes;
	}

	/**
	 * Calculate the estimates for all options. Note that plan model has to be routed before computing estimates.
	 */
	public void calculateEstimates(EstimatorContext context, PlanModel planModel) {

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

					double tt = 0;

					for (Leg leg : legs) {
						String legMode = leg.getMode();

						tt += timeInterpretation.decideOnLegTravelTime(leg).orElse(0);

						// Already scored with the trip estimator
						if (tripEst != null && legMode.equals(c.getMode()))
							continue;

						LegEstimator<Enum<?>> legEst = (LegEstimator<Enum<?>>) legEstimators.get(legMode);

						if (legEst == null)
							throw new IllegalStateException("No leg estimator defined for mode: " + legMode);

						estimate += legEst.estimate(context, legMode, leg, c.getOption());
					}

					TripStructureUtils.Trip trip = planModel.getTrip(i);

					// early or late arrival can also have an effect on the activity scores which is potentially considered here
					estimate += actEstimator.estimate(context, planModel.getStartTimes()[i] + tt, trip.getDestinationActivity());

					values[i] = estimate;
				}
			}
		}
	}

	/**
	 * Check whether all registered constraints are met.
	 */
	public boolean isValidOption(PlanModel model, String[] modes) {

		if (constraints.isEmpty())
			return true;

		// Scoring is null here as it should not be needed
		EstimatorContext context = new EstimatorContext(model.getPerson(), null);

		for (TripConstraint<?> c : this.constraints) {
			ConstraintHolder<?> h = new ConstraintHolder<>(
					(TripConstraint<Object>) c,
					c.getContext(context, model)
			);

			if (!h.test(modes))
				return false;

		}

		return true;
	}


	public record ConstraintHolder<T>(TripConstraint<T> constraint, T context) implements Predicate<String[]> {

		@Override
		public boolean test(String[] modes) {
			return constraint.isValid(context, modes);
		}

		public boolean testMode(String[] currentModes, ModeEstimate mode, boolean[] mask) {
			return constraint.isValidMode(context, currentModes, mode, mask);
		}

	}

}
