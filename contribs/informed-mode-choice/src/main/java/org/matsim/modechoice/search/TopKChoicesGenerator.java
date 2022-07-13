package org.matsim.modechoice.search;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.*;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.TripEstimator;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generate top n choices for each possible mode option.
 */
@SuppressWarnings("unchecked")
public class TopKChoicesGenerator extends AbstractCandidateGenerator {

	/**
	 * Maximum number of iterations. Memory usage will increase the more iterations are done.
	 */
	private static final int MAX_ITER = 10_000_000;

	private static final Logger log = LogManager.getLogger(TopKChoicesGenerator.class);


	@Inject
	TopKChoicesGenerator(InformedModeChoiceConfigGroup config) {
		super(config);
	}

	public Collection<PlanCandidate> generate(PlanModel planModel, boolean[] mask) {
		return generate(planModel, mask, config.getTopK(), 0);
	}

	/**
	 * Generate k top choices.
	 *
	 * @param planModel plan
	 * @param topK      use at most top k choices (for each combination)
	 * @param threshold discard solutions that are worse than best solution
	 */
	public Collection<PlanCandidate> generate(PlanModel planModel, boolean[] mask, int topK, double threshold) {

		EstimatorContext context = new EstimatorContext(planModel.getPerson(), params.getScoringParameters(planModel.getPerson()));

		if (mask != null)
			throw new UnsupportedOperationException("Mask not supported yet");

		prepareModel(planModel, context);

		// modes that need to be re-estimated with the full plan
		Set<String> consolidateModes = planModel.filterModes(ModeEstimate::isMin);

		List<ConstraintHolder<?>> constraints = buildConstraints(context, planModel);

		return generateCandidate(context, planModel, topK, threshold, consolidateModes, constraints);
	}

	private List<ConstraintHolder<?>> buildConstraints(EstimatorContext context, PlanModel planModel) {

		List<ConstraintHolder<?>> constraints = new ArrayList<>();
		for (TripConstraint<?> c : this.constraints) {
			constraints.add(new ConstraintHolder<>(
					(TripConstraint<Object>) c,
					c.getContext(context, planModel)
			));
		}

		return constraints;
	}

	protected final void prepareModel(PlanModel planModel, EstimatorContext context) {
		if (!planModel.hasEstimates()) {

			service.initEstimates(context, planModel);
			router.routeModes(planModel, planModel.filterModes(ModeEstimate::isUsable));
			service.calculateEstimates(context, planModel);
		}
	}


	private Collection<PlanCandidate> generateCandidate(EstimatorContext context, PlanModel planModel, int topK, double threshold,
	                                                    Set<String> consolidateModes, List<ConstraintHolder<?>> constraints) {

		ModeChoiceSearch search = new ModeChoiceSearch(planModel.trips(), planModel.modes());

		Object2ObjectMap<PlanCandidate, PlanCandidate> candidates = new Object2ObjectOpenHashMap<>();

		for (List<ModeEstimate> options : planModel.combinations()) {

			search.clear();

			for (ModeEstimate mode : options) {

				// check if a mode can be use at all
				if (!mode.isUsable())
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

				estimate += computePlanEstimate(context, planModel, result, usedModes, consolidateModes, options);

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

	/**
	 * Handles fixed costs and plan estimators.
	 */
	@SuppressWarnings("StringEquality")
	private double computePlanEstimate(EstimatorContext context, PlanModel planModel, String[] result,
	                                   ReferenceSet<String> usedModes,
	                                   Set<String> consolidateModes, Collection<ModeEstimate> options) {

		double estimate = 0;

		// Add the fixed costs estimate if a mode has been used
		for (ModeEstimate mode : options) {

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
			for (ModeEstimate mode : options) {
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

		return estimate;
	}

	/**
	 * Compute candidates from predefined given modes.
	 *
	 * @param modes list of mode combinations to estimate
	 * @return one candidate for each requested mode combination
	 */
	public List<PlanCandidate> generatePredefined(PlanModel planModel, List<String[]> modes) {

		List<PlanCandidate> candidates = new ArrayList<>();

		EstimatorContext context = new EstimatorContext(planModel.getPerson(), params.getScoringParameters(planModel.getPerson()));
		prepareModel(planModel, context);

		Map<String, ModeEstimate> singleOptions = new HashMap<>();

		// reduce to single options that are usable
		for (Map.Entry<String, List<ModeEstimate>> e : planModel.getEstimates().entrySet()) {
			for (ModeEstimate o : e.getValue()) {
				// check if a mode can be use at all
				if (!o.isUsable() || o.isMin())
					continue;

				singleOptions.put(e.getKey(), o);
				break;
			}
		}


		// Same Logic as the top k estimator
		for (String[] result : modes) {

			if (result.length != planModel.trips())
				throw new IllegalArgumentException(String.format("Mode arrays must be same length as trips: %d != %d", result.length, planModel.trips()));

			double estimate = 0;

			// Collect estimates for all entries
			for (int i = 0; i < result.length; i++) {

				if (result[i] == null)
					continue;

				result[i] = result[i].intern();

				String mode = result[i];

				ModeEstimate opt = singleOptions.get(mode);
				if (opt == null)
					continue;

				estimate += opt.getEstimates()[i];

			}

			ReferenceSet<String> usedModes = new ReferenceOpenHashSet<>(result);

			Set<String> consolidateModes = planModel.filterModes(ModeEstimate::isMin);

			estimate += computePlanEstimate(context, planModel, result, usedModes, consolidateModes, singleOptions.values());

			PlanCandidate c = new PlanCandidate(Arrays.copyOf(result, planModel.trips()), estimate);

			candidates.add(c);
		}

		return candidates;
	}

	private record ConstraintHolder<T>(TripConstraint<T> constraint, T context) implements Predicate<String[]> {

		@Override
		public boolean test(String[] modes) {
			return constraint.isValid(context, modes);
		}
	}

}
