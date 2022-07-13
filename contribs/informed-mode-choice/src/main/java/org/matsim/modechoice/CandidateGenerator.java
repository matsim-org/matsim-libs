package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Plan;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;

/**
 * Interface to generate plan candidates.
 */
@FunctionalInterface
public interface CandidateGenerator {

	/**
	 * Generate plan candidates, ordered by their natural comparator.
	 */
	default Collection<PlanCandidate> generate(PlanModel planModel) {
		return generate(planModel, null);
	}


	/**
	 * Generate plan candidates, ordered by their natural comparator.
	 * @param mask if not null, only include these trips with a true entry at their respective index.
	 */
	Collection<PlanCandidate> generate(PlanModel planModel, @Nullable boolean[] mask);
}
