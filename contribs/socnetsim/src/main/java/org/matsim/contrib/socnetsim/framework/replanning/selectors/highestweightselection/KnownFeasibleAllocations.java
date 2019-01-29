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
package org.matsim.contrib.socnetsim.framework.replanning.selectors.highestweightselection;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
final class KnownFeasibleAllocations {
	private final Queue<GroupPlans> cache = new ArrayDeque<GroupPlans>();
	// limits both memory and computation time. No need to remember too much,
	// as similar plans allow similar combinations.
	private final int capacity;

	// represents the last known "actually blocking" combination (the plans
	// which were actually used to determine that a combination is blocking).
	// This allows to discard bloking brnches at no cost when a leave is found
	// blocking.
	private GroupPlans knownForbiddingPlans = null;

	// /////////////////////////////////////////////////////////////////////////
	// init
	// /////////////////////////////////////////////////////////////////////////
	public KnownFeasibleAllocations() {
		this( Integer.MAX_VALUE );
	}

	public KnownFeasibleAllocations(final int capacity) {
		if ( capacity < 0 ) throw new IllegalArgumentException( ""+capacity );
		this.capacity = capacity;
	}

	// /////////////////////////////////////////////////////////////////////////
	// feasible
	// /////////////////////////////////////////////////////////////////////////
	public void addFeasibleAllocation(final GroupPlans plans) {
		cache.add( plans );
		if ( cache.size() > capacity ) cache.remove();
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

	// /////////////////////////////////////////////////////////////////////////
	// known infeasible
	// /////////////////////////////////////////////////////////////////////////
	public boolean isKownAsBlocking(final GroupPlans plan) {
		// known as blocking if contains the known blocking
		return knownForbiddingPlans != null &&
			plan.getJointPlans().containsAll( knownForbiddingPlans.getJointPlans() ) &&
			plan.getIndividualPlans().containsAll( knownForbiddingPlans.getIndividualPlans() );
	}

	public void setKownBlockingCombination(final GroupPlans plan) {
		this.knownForbiddingPlans = plan;
	}
}

