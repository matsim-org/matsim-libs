package org.matsim.modechoice.search;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.PlanCandidate;

import java.util.*;

/**
 * Generates the best known choices.
 */
public class BestChoiceGenerator extends TopKChoicesGenerator {

	// This class is completely based on the top k choices generator with a small k
	// However, only computing the very best option is an easier problem to solve
	// and a dedicated implementation might be more efficient

	@Inject
	BestChoiceGenerator(InformedModeChoiceConfigGroup config, Map<String, ModeOptions<?>> options) {
		super(config, options);
	}

	@Override
	public Collection<PlanCandidate> generate(Plan plan) {

		List<PlanCandidate> candidates = new ArrayList<>(generate(plan, null, 10, 0));

		if (candidates.isEmpty())
			return Set.of();

		return List.of(candidates.stream().max(Comparator.comparingDouble(PlanCandidate::getUtility)).orElseThrow());
	}

}
