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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public final class KnownStates {
	// note: using lists assumes that records are always ordered the same way
	private final Map<List<PersonRecord>, PlanAllocation> cache = new HashMap<List<PersonRecord>, PlanAllocation>();
	private final Map<List<PersonRecord>, Double> unfeasibleProblems = new HashMap<List<PersonRecord>, Double>();

	public PlanAllocation getCachedSolutionForRemainingAgents(
			final List<PersonRecord> remainingAgents) {
		final PlanAllocation alloc = cache.get( remainingAgents );
		if ( alloc == null ) return null;

		final PlanAllocation copy = new PlanAllocation();
		copy.addAll( alloc.getPlans() );
		return copy;
	}

	public boolean isUnfeasible(
			final List<PersonRecord> remainingAgents,
			final double minimalWeightToAttain) {
		final Double level = unfeasibleProblems.get( remainingAgents );
		return level != null && minimalWeightToAttain > level;
	}

	public void cacheSolution(
			final List<PersonRecord> remainingAgents,
			final PlanAllocation solution,
			final double minimalWeightToAttain) {
		// XXX list should be copied to be on the safe side
		if ( solution != null ) {
			final PlanAllocation copy = new PlanAllocation();
			copy.addAll( solution.getPlans() );
			cache.put( 
					remainingAgents,
					copy );
		}
		else {
			final Double level = unfeasibleProblems.get( remainingAgents );
			if ( level == null || level > minimalWeightToAttain ) {
				unfeasibleProblems.put( remainingAgents , minimalWeightToAttain );
			}
		}
	}
}

