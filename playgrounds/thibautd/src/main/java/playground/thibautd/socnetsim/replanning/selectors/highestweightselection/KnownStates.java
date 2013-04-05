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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public final class KnownStates {
	// note: using lists assumes that records are always ordered the same way
	private final Collection<List<PersonRecord>> cachedRemainingPersons = new HashSet<List<PersonRecord>>();
	private final Map<Collection<PlanRecord>, PlanAllocation> cache = new HashMap<Collection<PlanRecord>, PlanAllocation>();
	private final Collection<List<PersonRecord>> cachedUnfeasibleProblems = new HashSet<List<PersonRecord>>();
	private final Map<Collection<PlanRecord>, Double> unfeasibleProblems = new HashMap<Collection<PlanRecord>, Double>();

	public PlanAllocation getCachedSolutionForRemainingAgents(
			final List<PersonRecord> remainingAgents) {
		if ( !cachedRemainingPersons.contains( remainingAgents ) ) return null;
		final PlanAllocation alloc = cache.get( getFeasibleRecords( remainingAgents ) );
		if ( alloc == null ) return null;

		final PlanAllocation copy = new PlanAllocation();
		copy.addAll( alloc.getPlans() );
		return copy;
	}

	public boolean isUnfeasible(
			final List<PersonRecord> remainingAgents,
			final double minimalWeightToAttain) {
		if ( !cachedUnfeasibleProblems.contains( remainingAgents ) ) return false;
		final Double level = unfeasibleProblems.get( getFeasibleRecords( remainingAgents ) );
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
			cachedRemainingPersons.add( remainingAgents );
			cache.put( 
					getFeasibleRecords( remainingAgents ),
					copy );
		}
		else {
			final Double level = unfeasibleProblems.get( remainingAgents );
			if ( level == null || level > minimalWeightToAttain ) {
				cachedUnfeasibleProblems.add( remainingAgents );
				unfeasibleProblems.put( getFeasibleRecords( remainingAgents ) , minimalWeightToAttain );
			}
		}
	}

	private static Collection<PlanRecord> getFeasibleRecords(
			final List<PersonRecord> persons ) {
		final List<PlanRecord> plans = new ArrayList<PlanRecord>();

		for ( PersonRecord person : persons ) {
			for ( PlanRecord pr : person.bestPlansPerJointStructure ) {
				if ( pr.isStillFeasible ) plans.add( pr );
			}
		}
		return plans;
	}
}

