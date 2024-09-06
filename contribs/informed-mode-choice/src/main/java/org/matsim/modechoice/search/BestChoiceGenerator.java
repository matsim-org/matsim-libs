package org.matsim.modechoice.search;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Generates the best known choices.
 */
public class BestChoiceGenerator extends TopKChoicesGenerator {

	// This class is completely based on the top k choices generator with a small k
	// However, only computing the very best option is an easier problem to solve
	// and a dedicated implementation might be more efficient

	@Inject
	BestChoiceGenerator(InformedModeChoiceConfigGroup config) {
		super(config);
	}


	@Override
	public List<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> consideredModes, @Nullable boolean[] mask) {

		List<PlanCandidate> candidates = new ArrayList<>(generate(planModel, consideredModes, mask, 10, 0, Double.NaN));

		if (candidates.isEmpty())
			return candidates;

		return candidates.subList(0, 1);
	}

}
