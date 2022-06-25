package org.matsim.modechoice.search;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.PlanCandidate;

import java.util.*;

/**
 * Generates the best known choices. It should be noted that there could be multiple when min max estimates are in use.
 */
public class BestChoicesGenerator extends TopKChoicesGenerator {

	// This class is completely based on the top k choices generator with k=1
	// However, only computing the very best option is an easier problem to solve
	// and a dedicated implementation might be more efficient

	@Inject
	BestChoicesGenerator(InformedModeChoiceConfigGroup config, Map<String, ModeOptions<?>> options) {
		super(config, options);
	}

	@Override
	public Collection<PlanCandidate> generate(Plan plan) {

		List<PlanCandidate> candidates = new ArrayList<>(generate(plan, 1, 0));

		if (candidates.isEmpty())
			return Set.of();

		// sort by minimum guaranteed utility
		candidates.sort(Comparator.comparingDouble(PlanCandidate::getMinUtility).reversed());

		double min = candidates.get(0).getUtility();

		// Candidates with worse best case utility than the guaranteed best can be removed
		candidates.removeIf(c -> c.getUtility() < min);

		return candidates;
	}

}
