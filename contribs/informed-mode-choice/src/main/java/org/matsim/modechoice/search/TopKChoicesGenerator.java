package org.matsim.modechoice.search;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.modechoice.*;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.TripEstimator;
import org.matsim.modechoice.pruning.CandidatePruner;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.modechoice.PlanModelService.ConstraintHolder;

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

	public Collection<PlanCandidate> generate(PlanModel planModel, Set<String> consideredModes, boolean[] mask) {

		CandidatePruner p = pruner.get();
		double threshold = -1;
		if (p != null) {
			threshold = p.planThreshold(planModel);
		}

		return generate(planModel, consideredModes, mask, config.getTopK(), threshold, Double.NaN);
	}

	/**
	 * Generate k top choices.
	 *
	 * @param planModel     plan
	 * @param topK          use at most top k choices (for each combination)
	 * @param diffThreshold allowed difference to the best solution (if positive)
	 * @param absThreshold  minimal required estimate score (use NaN or negative infinity if not needed)
	 */
	public Collection<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> consideredModes, @Nullable boolean[] mask,
	                                          int topK, double diffThreshold, double absThreshold) {

		EstimatorContext context = new EstimatorContext(planModel.getPerson(), params.getScoringParameters(planModel.getPerson()));

		if (mask != null && mask.length != planModel.trips())
			throw new IllegalArgumentException("Mask must be same length as trips.");

		prepareModel(planModel, context);

		if (planModel.trips() == 0)
			return new ArrayList<>();

		// modes that need to be re-estimated with the full plan
		Set<String> consolidateModes = planModel.filterModes(ModeEstimate::isMin);

		List<ConstraintHolder<?>> constraints = buildConstraints(context, planModel);

		return generateCandidate(context, planModel, mask, topK, diffThreshold, absThreshold, consideredModes, consolidateModes, constraints);
	}

	protected final void prepareModel(PlanModel planModel, EstimatorContext context) {

		if (!planModel.hasEstimates()) {
			service.initEstimates(context, planModel);
		}

		if (!planModel.isFullyRouted()) {
			router.routeModes(planModel, planModel.filterModes(ModeEstimate::isUsable));
			service.calculateEstimates(context, planModel);
		}
	}


	private Collection<PlanCandidate> generateCandidate(EstimatorContext context, PlanModel planModel, boolean[] mask, int topK, double diffThreshold, double absThreshold,
	                                                    Set<String> consideredModes, Set<String> consolidateModes, List<ConstraintHolder<?>> constraints) {

		ModeChoiceSearch search = new ModeChoiceSearch(planModel.trips(), planModel.modes());

		Object2ObjectMap<PlanCandidate, PlanCandidate> candidates = new Object2ObjectOpenHashMap<>();

		int skipped = 0;
		List<List<ModeEstimate>> combinations = planModel.combinations();

		comb:
		for (List<ModeEstimate> options : combinations) {

			search.clear();

			m:
			for (ModeEstimate mode : options) {

				// check if a mode can be use at all
				if (!mode.isUsable() || (consideredModes != null && !consideredModes.contains(mode.getMode())))
					continue m;

				for (ConstraintHolder<?> c : constraints) {

					if (!c.testMode(planModel.getCurrentModesMutable(), mode, mask))
						continue m;
				}

				search.addEstimates(mode.getMode(), mode.getEstimates(), mask);
			}

			if (search.isEmpty())
				continue comb;

			// results will be updated here
			String[] result = planModel.getCurrentModes();

			// There could be unknown modes which need to be removed first
			for (int i = 0; i < result.length; i++) {
				if (!config.getModes().contains(result[i]))
					result[i] = null;

				if (mask != null && !mask[i] && result[i] != null) {

					// If the predefined mode contain any non-usable mode, this combination is skipped
					// There should be at least one combination where the mode is usable, if not the input violates the configuration
					for (ModeEstimate option : options) {
						if (option.getMode().equals(result[i]) && !option.isUsable()) {
							skipped++;
							continue comb;
						}

					}
				}
			}

			// Estimation of already existing modes that were predetermined
			// the search will not add them, so they need to be calculated here
			double preDeterminedEstimate = 0;
			if (mask != null) {

				for (int i = 0; i < mask.length; i++) {

					// only select the unmasked modes
					if (mask[i] || result[i] == null)
						continue;

					for (ModeEstimate option : options) {
						if (option.getMode().equals(result[i]))
							preDeterminedEstimate += option.getEstimates()[i];
					}
				}
			}

			// store which modes have been used for one solution
			ReferenceSet<String> usedModes = new ReferenceOpenHashSet<>();

			int k = 0;
			int n = 0;
			DoubleIterator it = search.iter(result);
			double best = Double.NEGATIVE_INFINITY;

			outer:
			while (it.hasNext() && k < topK) {
				double estimate = preDeterminedEstimate + it.nextDouble();

				if (n++ > MAX_ITER) {
					log.warn("Maximum number of iterations reached for person {}", context.person.getId());
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

				if (!Double.isNaN(diffThreshold) && diffThreshold >= 0 && best - estimate > diffThreshold)
					break;

				// absolute threshold
				if (!Double.isNaN(absThreshold) && estimate < absThreshold)
					break;

				PlanCandidate c = new PlanCandidate(Arrays.copyOf(result, planModel.trips()), estimate);

				if (candidates.containsKey(c)) {
					if (Math.abs(candidates.get(c).getUtility() - c.getUtility()) > 1e-5) {

						// TODO: probably needs to be investigated
						// Only occurs with certain configurations, usually including the pt mode
						// Not big issue probably, might remove the warning

//						throw new IllegalStateException("Candidates estimates differ: " + candidates.get(c) + " | " + c);
						log.warn("Estimates differ for person {}: {} vs. {}", planModel.getPerson().getId(), candidates.get(c), c.getUtility());
					}

					// Put the candidate with the higher utility in the map
					if (c.getUtility() > candidates.get(c).getUtility()) {
						candidates.put(c, c);
					}

				} else
					candidates.put(c, c);

				k++;
			}
		}

		if (skipped == combinations.size())
			throw new IllegalStateException("Received an input with predetermined modes, which are not usable for this agent: " + planModel.getPerson());

		List<PlanCandidate> result = candidates.keySet().stream().sorted().limit(topK).collect(Collectors.toList());

		// threshold need to be rechecked again on the global best
		if (!Double.isNaN(diffThreshold) && diffThreshold >= 0) {
			double best = result.get(0).getUtility();

			// absolute threshold
			result.removeIf(c -> c.getUtility() < best - diffThreshold);
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
	 * @return one candidate for each requested mode combination, A candidate violating a constraint will receive a utility of {@link Double#NEGATIVE_INFINITY}.
	 *
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

		List<ConstraintHolder<?>> constraints = buildConstraints(context, planModel);

		Set<String> consolidateModes = planModel.filterModes(ModeEstimate::isMin);
		Set<String> usableModes = planModel.filterModes(ModeEstimate::isUsable);

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
				if (opt == null) {

					// Violates constraints by using a non-allowed mode
					if (allModes.contains(mode) && !usableModes.contains(mode)) {
						estimate = Double.NEGATIVE_INFINITY;
						break;
					} else
						continue;
				}

				estimate += opt.getEstimates()[i];

			}

			ReferenceSet<String> usedModes = new ReferenceOpenHashSet<>(result);

			estimate += computePlanEstimate(context, planModel, result, usedModes, consolidateModes, singleOptions.values());

			for (ConstraintHolder<?> c : constraints) {
				if (!c.test(result))
					estimate = Double.NEGATIVE_INFINITY;
			}

			PlanCandidate c = new PlanCandidate(Arrays.copyOf(result, planModel.trips()), estimate);

			candidates.add(c);
		}

		return candidates;
	}

}
