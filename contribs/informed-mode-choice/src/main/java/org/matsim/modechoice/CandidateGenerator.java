package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Plan;

import java.util.Collection;
import java.util.Iterator;

/**
 * Interface to generate candidates
 */
@FunctionalInterface
public interface CandidateGenerator {

	Collection<PlanCandidate> generate(Plan plan);


}
