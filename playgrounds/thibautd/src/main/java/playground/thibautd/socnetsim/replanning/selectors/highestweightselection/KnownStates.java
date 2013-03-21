/* *********************************************************************** *
 * project: org.matsim.*
 * KnownStates.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.socnetsim.replanning.selectors.highestweightselection;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
public final class KnownStates {
	private final Map<Set<PersonRecord>, PlanAllocation> cache = new HashMap<Set<PersonRecord>, PlanAllocation>();
	private final Map<Set<PersonRecord>, Double> unfeasibleProblems = new HashMap<Set<PersonRecord>, Double>();

	public PlanAllocation getCachedSolutionForRemainingAgents(
			final Collection<PersonRecord> remainingAgents) {
		final Set<PersonRecord> set = new HashSet<PersonRecord>( remainingAgents );
		assert set.size() == remainingAgents.size() : set.size() +"!="+ remainingAgents.size();

		final PlanAllocation alloc = cache.get( set );
		if ( alloc == null ) return null;

		final PlanAllocation copy = new PlanAllocation();
		copy.addAll( alloc.getPlans() );
		return copy;
	}

	public boolean isUnfeasible(
			final Collection<PersonRecord> remainingAgents,
			final double minimalWeightToAttain) {
		final Set<PersonRecord> set = new HashSet<PersonRecord>( remainingAgents );
		assert set.size() == remainingAgents.size() : set.size() +"!="+ remainingAgents.size();

		final Double level = unfeasibleProblems.get( set );
		return level != null && minimalWeightToAttain > level;
	}

	public void cacheSolution(
			final Collection<PersonRecord> remainingAgents,
			final PlanAllocation solution,
			final double minimalWeightToAttain) {
		final Set<PersonRecord> set = new HashSet<PersonRecord>( remainingAgents );
		assert set.size() == remainingAgents.size() : set.size() +"!="+ remainingAgents.size();
		if ( solution != null ) {
			final PlanAllocation copy = new PlanAllocation();
			copy.addAll( solution.getPlans() );
			cache.put( 
					set,
					copy );
		}
		else {
			final Double level = unfeasibleProblems.get( set );
			if ( level == null || level > minimalWeightToAttain ) {
				unfeasibleProblems.put( set , minimalWeightToAttain );
			}
		}
	}
}

