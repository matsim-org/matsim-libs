package org.matsim.modechoice.search;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.*;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.TripEstimator;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generate top n choices for each possible mode option.
 */
@SuppressWarnings("unchecked")
public class TopKChoicesGenerator implements CandidateGenerator {


	/**
	 * Maximum number of iterations. Memory usage will increase the more iterations are done.
	 */
	private static final int MAX_ITER = 10_000_000;

	private static final Logger log = LogManager.getLogger(TopKChoicesGenerator.class);

	private final InformedModeChoiceConfigGroup config;
	private final Map<String, ModeOptions<?>> options;

	@Inject
	private Map<String, LegEstimator<?>> legEstimators;

	@Inject
	private Map<String, TripEstimator<?>> tripEstimator;

	@Inject
	private Map<String, FixedCostsEstimator<?>> fixedCosts;

	@Inject
	private ScoringParametersForPerson params;

	@Inject
	private EstimateRouter router;

	@Inject
	private Set<TripConstraint<?>> constraints;

	@Inject
	TopKChoicesGenerator(InformedModeChoiceConfigGroup config, Map<String, ModeOptions<?>> options) {

		this.config = config;
		this.options = options;

		for (String mode : config.getModes()) {

			if (!options.containsKey(mode))
				throw new IllegalArgumentException(String.format("No estimators configured for mode %s", mode));
		}
	}

	public Collection<PlanCandidate> generate(Plan plan) {
		return generate(plan, config.getTopK(), 0);
	}

	/**
	 * Generate k top choices.
	 *
	 * @param plan      plan
	 * @param topK      use at most top k choices (for each combination)
	 * @param threshold discard solutions that are worse than best solution
	 */
	public Collection<PlanCandidate> generate(Plan plan, int topK, double threshold) {

		EstimatorContext context = new EstimatorContext(plan.getPerson(), params.getScoringParameters(plan.getPerson()));

		PlanModel planModel = new PlanModel(plan);

		Map<String, List<Combination>> options = new IdentityHashMap<>();

		Set<String> usableModes = new HashSet<>();

		// modes that need to be re-estimated with the full plan
		Set<String> consolidateModes = new HashSet<>();

		for (String mode : config.getModes()) {

			ModeOptions<Enum<?>> t = (ModeOptions<Enum<?>>) this.options.get(mode);

			List<Enum<?>> modeOptions = t.get(plan.getPerson());
			if (modeOptions.stream().anyMatch(t::allowUsage))
				usableModes.add(mode);

			List<Combination> c = new ArrayList<>();

			for (Enum<?> modeOption : modeOptions) {
				TripEstimator<Enum<?>> te = (TripEstimator<Enum<?>>) tripEstimator.get(mode);

				// Check if min estimate is needed
				if (te != null && te.providesMinEstimate(context, mode, modeOption)) {

					c.add(new Combination(mode, modeOption, planModel.trips(), true, false));
					c.add(new Combination(mode, modeOption, planModel.trips(), true, true));

					consolidateModes.add(mode);
				} else {

					c.add(new Combination(mode, modeOption, planModel.trips(), false, false));

				}
			}

			options.put(mode, c);
		}

		router.routeModes(plan, planModel, usableModes);

		calculateEstimates(context, planModel, options);

		List<ConstraintHolder<?>> constraints = buildConstraints(context, planModel, plan);

		return generateCandidate(context, planModel, topK, threshold, consolidateModes, constraints, Combination.combinations(options));
	}


	private List<ConstraintHolder<?>> buildConstraints(EstimatorContext context, PlanModel planModel, Plan plan) {

		List<ConstraintHolder<?>> constraints = new ArrayList<>();
		for (TripConstraint<?> c : this.constraints) {
			constraints.add(new ConstraintHolder<>(
					(TripConstraint<Object>) c,
					c.getContext(context, planModel, plan)
			));
		}

		return constraints;
	}

	/**
	 * Calculate the estimates for all options.
	 */
	private void calculateEstimates(EstimatorContext context, PlanModel planModel, Map<String, List<Combination>> options) {

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

	@SuppressWarnings("StringEquality")
	private Collection<PlanCandidate> generateCandidate(EstimatorContext context, PlanModel planModel, int topK, double threshold,
	                                                    Set<String> consolidateModes, List<ConstraintHolder<?>> constraints,
	                                                    List<List<Combination>> combinations) {

		ModeChoiceSearch search = new ModeChoiceSearch(planModel.trips(), planModel.modes());

		Object2ObjectMap<PlanCandidate, PlanCandidate> candidates = new Object2ObjectOpenHashMap<>();

		for (List<Combination> options : combinations) {

			search.clear();

			for (Combination mode : options) {

				ModeOptions<Enum<?>> t = (ModeOptions<Enum<?>>) this.options.get(mode.getMode());

				// check if a mode can be use at all
				if (!t.allowUsage(mode.getOption()))
					continue;

				search.addEstimates(mode.getMode(), mode.getEstimates());
			}

			// results will be updated here
			String[] result = new String[planModel.trips()];

			// store which modes have been used for one solution
			ReferenceSet<String> usedModes = new ReferenceOpenHashSet<>();

			int k = 0;
			int n = 0;
			DoubleIterator it = search.iter(result);
			double best = Double.NEGATIVE_INFINITY;

			outer:
			while (it.hasNext() && k < topK) {
				double estimate = it.nextDouble();

				if (n++ > MAX_ITER) {
					log.warn("Maximum number of iterations reached for {}", context.person.getId());
					break;
				}

				for (ConstraintHolder<?> c : constraints) {
					if (!c.test(result))
						continue outer;
				}

				Collections.addAll(usedModes, result);

				// Add the fixed costs estimate if a mode has been used
				for (Combination mode : options) {

					FixedCostsEstimator<Enum<?>> f = (FixedCostsEstimator<Enum<?>>) fixedCosts.get(mode.getMode());

					// Fixed costs are not required for each mode
					if (f == null)
						continue;

					if (usedModes.contains(mode.getMode())) {
						estimate += f.usageUtility(context, mode.getMode(), mode.getOption());
					}

					estimate += f.fixedUtility(context, mode.getMode(), mode.getOption());
				}

				for (String consolidateMode : consolidateModes) {
					// search all options for the mode to consolidate
					for (Combination mode : options) {
						if (mode.getMode() == consolidateMode && usedModes.contains(consolidateMode)) {

							TripEstimator<Enum<?>> f = (TripEstimator<Enum<?>>) tripEstimator.get(mode.getMode());

							// subtract all the trip estimates that have been made before
							for (int i = 0; i < result.length; i++) {
								if (result[i] == mode.getMode())
									estimate -= mode.getTripEstimates()[i];
							}

							// Add the estimate for the whole plan
							estimate += f.estimate(context, mode.getMode(), result, planModel, mode.getOption());

						}
					}
				}

				usedModes.clear();

				if (estimate > best)
					best = estimate;

				if (threshold > 0 && best - estimate > threshold)
					break;

					// relative threshold, only works if estimate is negative
				else if (threshold < 0 && best < 0 && estimate < (1 - threshold) * best)
					break;

				PlanCandidate c = new PlanCandidate(Arrays.copyOf(result, planModel.trips()), estimate);

				if (candidates.containsKey(c))
					if (Math.abs(candidates.get(c).getUtility() - c.getUtility()) > 1e-5)
						throw new IllegalStateException("Candidates estimates differ: " + candidates.get(c) + " | " + c);

				candidates.put(c, c);

				k++;
			}
		}

		List<PlanCandidate> result = candidates.keySet().stream().sorted().limit(topK).collect(Collectors.toList());

		// threshold need to be rechecked again on the global best
		if (threshold != 0) {

			double best = result.get(0).getUtility();

			if (threshold < 0)
				// relative threshold
				result.removeIf(c -> c.getUtility() < (1 - threshold) * best);
			else
				// absolute threshold
				result.removeIf(c -> c.getUtility() < best - threshold);
		}

		return result;
	}

	private static final class ConstraintHolder<T> implements Predicate<String[]> {

		public final TripConstraint<T> constraint;
		public final T context;

		private ConstraintHolder(TripConstraint<T> constraint, T context) {
			this.constraint = constraint;
			this.context = context;
		}


		@Override
		public boolean test(String[] modes) {
			return constraint.isValid(context, modes);
		}
	}

}
