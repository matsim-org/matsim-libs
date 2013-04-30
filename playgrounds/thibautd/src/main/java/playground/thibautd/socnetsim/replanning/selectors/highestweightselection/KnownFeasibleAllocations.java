/* *********************************************************************** *
 * project: org.matsim.*
 * KnownFeasibleAllocations.java
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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
final class KnownFeasibleAllocations {
	private final Queue<GroupPlans> cache = new ArrayDeque<GroupPlans>();
	// limits both memory and computation time. No need to remember too much,
	// as similar plans allow similar combinations.
	private final int capacity;

	public KnownFeasibleAllocations() {
		this( Integer.MAX_VALUE );
	}

	public KnownFeasibleAllocations(final int capacity) {
		if ( capacity < 0 ) throw new IllegalArgumentException( ""+capacity );
		this.capacity = capacity;
	}

	public void addFeasibleAllocation(final GroupPlans plans) {
		cache.add( plans );
		if ( cache.size() > capacity ) cache.poll();
		assert cache.size() <= capacity;
	}

	public boolean blocksAllKnownAllocations(final GroupPlans plan) {
		for ( GroupPlans cached : cache ) {
			if ( Collections.disjoint(
						cached.getAllIndividualPlans(),
						plan.getAllIndividualPlans() ) ) {
				return false;
			}
		}
		return true;
	}
}

