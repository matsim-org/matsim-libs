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
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.thibautd.utils.MapUtils;

/**
 * @author thibautd
 */
final class KnownStates {
	private final Map<List<PersonRecord>, Map<Set<Id>, PlanAllocation>> cache =
			new HashMap<List<PersonRecord>, Map<Set<Id>, PlanAllocation>>();

	public void cache(
			final List<PersonRecord> personsToAllocate,
			final Set<Id> allowedIncompatibilityGroups,
			final PlanAllocation allocation) {
		if ( allocation == null ) return;

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
}

