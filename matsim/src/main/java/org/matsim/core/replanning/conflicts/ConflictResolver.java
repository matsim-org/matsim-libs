package org.matsim.core.replanning.conflicts;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

/**
 * This interface is called after standard replanning. Its purpose is to check
 * the population and detect any conflicts between agents. The interface must
 * then return a list of agents that should be reset to a non-conflicting plan
 * in order to resolve all conflicts. Plans that are not conflicting are
 * identified as such using the isPotentiallyConflicting method.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ConflictResolver {
	IdSet<Person> resolve(Population population, int iteration);

	boolean isPotentiallyConflicting(Plan plan);

	String getName();
}
