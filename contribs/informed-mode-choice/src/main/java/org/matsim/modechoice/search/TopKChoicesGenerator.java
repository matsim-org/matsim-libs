package org.matsim.modechoice.search;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.modechoice.*;
import org.matsim.modechoice.pruning.CandidatePruner;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.matsim.modechoice.PlanModelService.ConstraintHolder;

/**
 * Generate top n choices for each possible mode option.
 */
@SuppressWarnings("unchecked")
public class TopKChoicesGenerator extends AbstractCandidateGenerator {

	private static final Logger log = LogManager.getLogger(TopKChoicesGenerator.class);

	@Inject
	TopKChoicesGenerator(InformedModeChoiceConfigGroup config) {
		super(config);
	}

	public List<PlanCandidate> generate(PlanModel planModel, Set<String> consideredModes, boolean[] mask) {

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
	public List<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> consideredModes, @Nullable boolean[] mask,
	                                          int topK, double diffThreshold, double absThreshold) {

		EstimatorContext context = new EstimatorContext(planModel.getPerson(), params.getScoringParameters(planModel.getPerson()));

		if (mask != null && mask.length != planModel.trips())
			throw new IllegalArgumentException("Mask must be same length as trips.");

		calculator.prepareEstimates(planModel, context, consideredModes, mask);

		if (planModel.trips() == 0)
			return new ArrayList<>();

		List<ConstraintHolder<?>> constraints = calculator.buildConstraints(context, planModel);

		return generateCandidate(context, planModel, mask, topK, diffThreshold, absThreshold, consideredModes, constraints);
	}


	private List<PlanCandidate> generateCandidate(EstimatorContext context, PlanModel planModel, boolean[] mask, int topK, double diffThreshold, double absThreshold,
	                                                    Set<String> consideredModes, List<ConstraintHolder<?>> constraints) {

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

				double[] est = new double[mode.getLegEstimates().length];
				Arrays.setAll(est, i -> mode.getLegEstimates()[i] + mode.getActEst()[i]);

				if (mode.getTripEstimates() != null)
					Arrays.setAll(est, i -> est[i] + mode.getTripEstimates()[i]);

				// Only add estimates for desired modes and those that have actual usage
				search.addEstimates(mode.getMode(), est, mask, mode.getNoRealUsage());
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

			int k = 0;
			int n = 0;
			ModeIterator it = search.iter(result);
			double best = Double.NEGATIVE_INFINITY;

			outer:
			while (it.hasNext() && k < topK) {
				double estimate = it.nextDouble();

				if (n++ > it.maxIters()) {
					log.warn("Maximum number of iterations reached for person {}", context.person.getId());
					break;
				}

				for (ConstraintHolder<?> c : constraints) {
					if (!c.test(result))
						continue outer;
				}

				if (estimate > best)
					best = estimate;

				if (!Double.isNaN(diffThreshold) && diffThreshold >= 0 && best - estimate > diffThreshold)
					break;

				// Store the whole plan estimate, which may differ from the partial estimate generated during search
				PlanCandidate c = new PlanCandidate(Arrays.copyOf(result, planModel.trips()), calculator.calculatePlanEstimate(context, planModel, result));
				candidates.put(c, c);

				k++;
			}
		}

		if (skipped == combinations.size())
			throw new IllegalStateException("Received an input with predetermined modes, which are not usable for this agent: " + planModel.getPerson());

		List<PlanCandidate> result = candidates.keySet().stream().sorted().limit(topK).collect(Collectors.toList());

		// threshold need to be rechecked again on the global best
		if (!Double.isNaN(diffThreshold) && diffThreshold >= 0 && !result.isEmpty()) {
			double best = result.getFirst().getUtility();

			// absolute threshold
			result.removeIf(c -> c.getUtility() < best - diffThreshold);
		}

		// absolute threshold
		if (!Double.isNaN(absThreshold)) {
			result.removeIf(c -> c.getUtility() < absThreshold);
		}

		return result;
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

		calculator.prepareEstimates(planModel, context, modes);

		List<ConstraintHolder<?>> constraints = calculator.buildConstraints(context, planModel);
		Set<String> usableModes = planModel.filterModes(ModeEstimate::isUsable);

		// Same Logic as the top k estimator
		for (String[] result : modes) {

			if (result.length != planModel.trips())
				throw new IllegalArgumentException(String.format("Mode arrays must be same length as trips: %d != %d", result.length, planModel.trips()));

			double estimate = calculator.calculatePlanEstimate(context, planModel, result);

			for (String mode : result) {
				// Violates constraints by using a non-allowed mode
				if (allModes.contains(mode) && !usableModes.contains(mode)) {
					estimate = Double.NEGATIVE_INFINITY;
					break;
				}
			}

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
