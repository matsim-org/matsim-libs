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

/**
 * Generate top n choices for each possible mode option.
 */
public final class TopKChoicesGenerator implements CandidateGenerator {


	/**
	 * Maximum number of iterations. Memory usage will increase with more and more.
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
	public TopKChoicesGenerator(InformedModeChoiceConfigGroup config, Map<String, ModeOptions<?>> options) {

		this.config = config;
		this.options = options;

		for (String mode : config.getModes()) {

			if (!options.containsKey(mode))
				throw new IllegalArgumentException(String.format("No estimators configured for mode %s", mode));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<PlanCandidate> generate(Plan plan) {

		EstimatorContext context = new EstimatorContext(plan.getPerson(), params.getScoringParameters(plan.getPerson()));

		Set<String> usableModes = new HashSet<>();

		Map<String, List<PlanModel.Combination>> options = new IdentityHashMap<>();

		for (String mode : config.getModes()) {

			ModeOptions<Enum<?>> t = (ModeOptions<Enum<?>>) this.options.get(mode);

			List<Enum<?>> modeOptions = t.get(plan.getPerson());
			if (modeOptions.stream().anyMatch(t::allowUsage))
				usableModes.add(mode);

			List<PlanModel.Combination> c = new ArrayList<>();

			for (Enum<?> modeOption : modeOptions) {
				c.add(new PlanModel.Combination(mode, modeOption));
				TripEstimator<Enum<?>> te = (TripEstimator<Enum<?>>) tripEstimator.get(mode);

				// Check if max estimate is needed
				if (te != null && te.providesMaxEstimate(context, mode, modeOption))
					c.add(new PlanModel.Combination(mode, modeOption, true));
			}

			options.put(mode, c);
		}

		PlanModel planModel = router.routeModes(plan, usableModes);

		Map<PlanModel.Combination, double[]> estimates = calculateEstimates(context, planModel, options);

		List<ConstraintHolder<?>> constraints = new ArrayList<>();

		for (TripConstraint<?> c : this.constraints) {
			constraints.add(new ConstraintHolder<>(
					(TripConstraint<Object>) c,
					c.getContext(context, planModel, plan)
			));
		}

		return generateCandidate(context, planModel, estimates, constraints, PlanModel.combinations(options));

	}

	/**
	 * Calculate the estimates for all options.
	 */
	@SuppressWarnings("unchecked")
	private Map<PlanModel.Combination, double[]> calculateEstimates(EstimatorContext context, PlanModel planModel, Map<String, List<PlanModel.Combination>> options) {

		Map<PlanModel.Combination, double[]> estimates = new IdentityHashMap<>();

		for (Map.Entry<String, List<PlanModel.Combination>> e : options.entrySet()) {

			for (PlanModel.Combination c : e.getValue()) {

				double[] values = new double[planModel.trips()];

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
						estimate += c.isMax() ? minMax.getMax() : minMax.getMin();
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

				estimates.put(c, values);
			}
		}

		return estimates;
	}

	@SuppressWarnings("unchecked")
	private Collection<PlanCandidate> generateCandidate(EstimatorContext context, PlanModel planModel, Map<PlanModel.Combination, double[]> estimates,
	                                                    List<ConstraintHolder<?>> constraints, List<List<PlanModel.Combination>> combinations) {

		ModeChoiceSearch search = new ModeChoiceSearch(planModel.trips(), planModel.modes());

		Object2ObjectMap<PlanCandidate, PlanCandidate> candidates = new Object2ObjectAVLTreeMap<>();

		for (List<PlanModel.Combination> options : combinations) {

			search.clear();

			for (PlanModel.Combination mode : options) {

				ModeOptions<Enum<?>> t = (ModeOptions<Enum<?>>) this.options.get(mode.getMode());

				// check if a mode can be use at all
				if (!t.allowUsage(mode.getOption()))
					continue;

				search.addEstimates(mode.getMode(), estimates.get(mode));
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
			while (it.hasNext() && k < config.getTopK()) {
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
				for (PlanModel.Combination mode : options) {

					FixedCostsEstimator<Enum<?>> f = (FixedCostsEstimator<Enum<?>>) fixedCosts.get(mode.getMode());

					// Fixed costs are not required for each mode
					if (f == null)
						continue;

					if (usedModes.contains(mode.getMode())) {
						estimate += f.usageUtility(context, mode.getMode(), mode.getOption());
					}

					estimate += f.fixedUtility(context, mode.getMode(), mode.getOption());
				}

				usedModes.clear();

				// the cutoff won't work until estimates are below 0
				// normally an agent should not have a negative costs for travelling
				if (estimate < 0 && estimate > best)
					best = estimate;

				// estimate and best are negative at this point
				if (estimate < 0 && config.getCutoff() > 0 && estimate < best * (1 + config.getCutoff()))
					break;

				PlanCandidate c = new PlanCandidate(Arrays.copyOf(result, planModel.trips()), estimate);
				PlanCandidate existing;

				// update min max utility for existing candidates
				if ((existing = candidates.get(c)) != null) {
					existing.updateUtility(estimate);
				} else
					candidates.put(c, c);

				k++;
			}

		}


		return candidates.keySet();
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
