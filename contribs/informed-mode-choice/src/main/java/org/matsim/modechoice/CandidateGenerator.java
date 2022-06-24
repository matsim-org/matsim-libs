package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Plan;

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
	Collection<PlanCandidate> generate(Plan plan);


}
