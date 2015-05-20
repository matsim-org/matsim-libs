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
package playground.thibautd.socnetsim.framework.replanning.selectors.highestweightselection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import org.matsim.core.utils.collections.MapUtils;

/**
 * @author thibautd
 */
final class KnownStates {
	private final Map<List<PersonRecord>, Map<Set<Id>, PlanAllocation>> cache =
			new HashMap<List<PersonRecord>, Map<Set<Id>, PlanAllocation>>();
	private final Map<List<PersonRecord>, Map<Set<Id>, DecreasingDouble>> unfeasible =
			new HashMap<List<PersonRecord>, Map<Set<Id>, DecreasingDouble>>();

	public void cache(
			final List<PersonRecord> personsToAllocate,
			final Set<Id> allowedIncompatibilityGroups,
			final PlanAllocation allocation,
			final double minimalWeight) {
		if ( allocation == null ) {
			cacheUnfeasible(
					personsToAllocate,
					allowedIncompatibilityGroups,
					minimalWeight);
		}
		else {
			cacheFeasible(
					personsToAllocate,
					allowedIncompatibilityGroups,
					allocation);
		}
	}

	private void cacheUnfeasible(
			final List<PersonRecord> personsToAllocate,
			final Set<Id> allowedIncompatibilityGroups,
			final double minimalWeight) {
		final Map<Set<Id>, DecreasingDouble> map =
			MapUtils.getMap( personsToAllocate , unfeasible );
		DecreasingDouble cachedWeight = map.get(
					allowedIncompatibilityGroups );

		if ( cachedWeight == null ) {
			cachedWeight = new DecreasingDouble();
			map.put( allowedIncompatibilityGroups , cachedWeight );
		}

		cachedWeight.set( minimalWeight );
	}

	private void cacheFeasible(
			final List<PersonRecord> personsToAllocate,
			final Set<Id> allowedIncompatibilityGroups,
			final PlanAllocation allocation) {
		assert allocation != null;
		assert personsToAllocate.size() == allocation.getPlans().size() :
			personsToAllocate.size()+" != "+allocation.getPlans().size();

		MapUtils.getMap( personsToAllocate , cache ).put(
				allowedIncompatibilityGroups,
				SelectorUtils.copy( allocation ) );
	}

	public PlanAllocation getCached(
			final List<PersonRecord> personsToAllocate,
			final Set<Id> allowedIncompatibilityGroups) {
		final PlanAllocation cached = 
			MapUtils.getMap( personsToAllocate , cache ).get(
				allowedIncompatibilityGroups );

		if ( cached == null ) return null;
		assert personsToAllocate.size() == cached.getPlans().size() :
			personsToAllocate.size()+" != "+cached.getPlans().size();

		return SelectorUtils.copy( cached );
	}

	public boolean isUnfeasible(
			final List<PersonRecord> personsToAllocate,
			final Set<Id> allowedIncompatibilityGroups,
			final double minWeightToObtain) {
		final Map<Set<Id>, DecreasingDouble> map =
			unfeasible.get( personsToAllocate );

		if ( map == null ) return false;

		final DecreasingDouble cachedWeight = map.get(
					allowedIncompatibilityGroups );

		if ( cachedWeight == null ) return false;
		return minWeightToObtain >= cachedWeight.get();
	}
}

final class DecreasingDouble {
	private double v = Double.POSITIVE_INFINITY;

	public void set(final double newV) {
		if ( newV < v ) v = newV;
	}

	public double get() {
		return v;
	}
}
