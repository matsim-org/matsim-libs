package org.matsim.modechoice.search;

import org.apache.commons.lang3.ArrayUtils;
import org.matsim.modechoice.*;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import java.util.*;
import java.util.function.Predicate;

/**
 * Generates candidates for choices at one particular trip.
 */
public class SingleTripChoicesGenerator extends AbstractCandidateGenerator {

	@Inject
	public SingleTripChoicesGenerator(InformedModeChoiceConfigGroup config) {
		super(config);
	}

	@Override
	public List<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> consideredModes, @Nullable boolean[] mask) {

		if (mask == null)
			throw new IllegalArgumentException("Mask must be provided");

		int idx = ArrayUtils.indexOf(mask, true);

		if (idx == -1 || ArrayUtils.lastIndexOf(mask, true) != idx) {
			throw new IllegalArgumentException("Mask must contain exactly one true value:" + Arrays.toString(mask));
		}

		if (mask.length != planModel.trips())
			throw new IllegalArgumentException("Mask needs length " + planModel.trips() + ": " + Arrays.toString(mask));

		EstimatorContext context = new EstimatorContext(planModel.getPerson(), params.getScoringParameters(planModel.getPerson()));
		List<PlanModelService.ConstraintHolder<?>> constraints = calculator.buildConstraints(context, planModel);

		Predicate<ModeEstimate> considerMode = m -> consideredModes == null || consideredModes.contains(m.getMode());
		calculator.prepareEstimates(planModel, context, considerMode.and(ModeEstimate::isUsable), idx);

		List<PlanCandidate> candidates = new ArrayList<>();

		// construct candidates
		gen: for (List<ModeEstimate> value : planModel.getEstimates().values()) {

			Optional<ModeEstimate> opt = value.stream()
					.filter(ModeEstimate::isUsable)
					.filter(Predicate.not(ModeEstimate::isMin))
					.filter(considerMode)
					.findFirst();

			if (opt.isEmpty())
				continue;

			ModeEstimate est = opt.get();

			// Not actual used modes are not generated here
			if (est.getNoRealUsage()[idx])
				continue;

			String[] modes = planModel.getCurrentModes();
			modes[idx] = est.getMode();

			double estimate = calculator.calculatePlanEstimate(context, planModel, modes);

			if (estimate == Double.NEGATIVE_INFINITY)
				continue;

			for (PlanModelService.ConstraintHolder<?> c : constraints) {
				if (!c.test(modes)) {
					continue gen;
				}
			}

			candidates.add(new PlanCandidate(modes, estimate));
		}

		Collections.sort(candidates);

		return candidates;
	}

}
