package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Plan;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Interface to generate plan candidates.
 */
@FunctionalInterface
public interface CandidateGenerator {

	/**
	 * Generate plan candidates, ordered by their natural comparator.
	 */
	default List<PlanCandidate> generate(PlanModel planModel) {
		return generate(planModel, null, null);
	}

	/**
	 * Generate plan candidates, ordered by their natural comparator.
	 * @param consideredModes if not null, will restrict usable modes to these present in the set
	 * @param mask if not null, only include these trips with a true entry at their respective index.
	 * @return list of candidates, this list must be mutable
	 */
	List<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> consideredModes, @Nullable boolean[] mask);
}
