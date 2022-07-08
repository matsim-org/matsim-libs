package org.matsim.modechoice.search;

import org.apache.commons.lang3.ArrayUtils;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * Generates candidates for choices at one particular trip.
 */
public class SingleTripChoicesGenerator extends AbstractCandidateGenerator {

	@Inject
	public SingleTripChoicesGenerator(InformedModeChoiceConfigGroup config, Map<String, ModeOptions<?>> options) {
		super(config, options);
	}

	@Override
	public Collection<PlanCandidate> generate(Plan plan, @Nullable boolean[] mask) {

		if (mask == null)
			throw new IllegalArgumentException("Mask must be provided");

		int idx = ArrayUtils.indexOf(mask, true);

		if (idx == -1 || ArrayUtils.lastIndexOf(mask, true) != idx) {
			throw new IllegalArgumentException("Mask must contain exactly one true value:" + Arrays.toString(mask));
		}

		PlanModel planModel = new PlanModel(plan);

		if (mask.length != planModel.trips())
			throw new IllegalArgumentException("Mask needs length " + planModel.trips() + ": " + Arrays.toString(mask));

		Set<String> usableModes = new HashSet<>();
		Map<String, List<Combination>> options = new IdentityHashMap<>();

		for (String mode : config.getModes()) {

			ModeOptions<Enum<?>> t = (ModeOptions<Enum<?>>) this.options.get(mode);

			List<Enum<?>> modeOptions = t.get(plan.getPerson());

			for (Enum<?> modeOption : modeOptions) {

				if (!t.allowUsage(modeOption))
					continue;

				// Only the first option that allows using the mode is considered
				options.put(mode, List.of(new Combination(mode, modeOption, planModel.trips(), false, false)));
				usableModes.add(mode);
				break;
			}
		}

		router.routeSingleTrip(plan, planModel, usableModes, idx);

		EstimatorContext context = new EstimatorContext(plan.getPerson(), params.getScoringParameters(plan.getPerson()));

		calculateEstimates(context, planModel, options);

		ArrayList<PlanCandidate> candidates = new ArrayList<>();

		// construct the
		for (List<Combination> value : options.values()) {

			Combination est = value.get(0);

			String[] modes = new String[planModel.trips()];
			modes[idx] = est.getMode();

			candidates.add(new PlanCandidate(modes, est.getEstimates()[idx]));
		}

		Collections.sort(candidates);

		return candidates;
	}

}
