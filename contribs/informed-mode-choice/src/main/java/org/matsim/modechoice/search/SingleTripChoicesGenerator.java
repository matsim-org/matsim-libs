package org.matsim.modechoice.search;

import org.apache.commons.lang3.ArrayUtils;
import org.matsim.modechoice.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
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
	public Collection<PlanCandidate> generate(PlanModel planModel, @Nullable boolean[] mask) {

		if (mask == null)
			throw new IllegalArgumentException("Mask must be provided");

		int idx = ArrayUtils.indexOf(mask, true);

		if (idx == -1 || ArrayUtils.lastIndexOf(mask, true) != idx) {
			throw new IllegalArgumentException("Mask must contain exactly one true value:" + Arrays.toString(mask));
		}

		if (mask.length != planModel.trips())
			throw new IllegalArgumentException("Mask needs length " + planModel.trips() + ": " + Arrays.toString(mask));

		EstimatorContext context = new EstimatorContext(planModel.getPerson(), params.getScoringParameters(planModel.getPerson()));

		if (!planModel.hasEstimates()) {
			service.initEstimates(context, planModel);
		}

		router.routeSingleTrip(planModel, planModel.filterModes(ModeEstimate::isUsable), idx);

		service.calculateEstimates(context, planModel);

		List<PlanCandidate> candidates = new ArrayList<>();

		// construct candidates
		for (List<ModeEstimate> value : planModel.getEstimates().values()) {

			Optional<ModeEstimate> opt = value.stream()
					.filter(ModeEstimate::isUsable)
					.filter(Predicate.not(ModeEstimate::isMin))
					.findFirst();

			if (opt.isEmpty())
				continue;

			ModeEstimate est = opt.get();

			String[] modes = new String[planModel.trips()];
			modes[idx] = est.getMode();

			double estimate = est.getEstimates()[idx];

			if (estimate == Double.NEGATIVE_INFINITY)
				continue;

			candidates.add(new PlanCandidate(modes, estimate));
		}

		Collections.sort(candidates);

		return candidates;
	}

}
