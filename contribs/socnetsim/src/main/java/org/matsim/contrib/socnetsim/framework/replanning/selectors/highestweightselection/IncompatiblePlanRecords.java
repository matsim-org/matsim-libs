/* *********************************************************************** *
 * project: org.matsim.*
 * IncompatiblePlanRecords.java
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import org.matsim.core.utils.collections.MapUtils;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifier;

/**
 * @author thibautd
 */
final class IncompatiblePlanRecords {
	private final Set<Id> allIncompatibilityGroupIds;

	private final Map<Id, Collection<PlanRecord>> plansPerGroup = new HashMap<Id, Collection<PlanRecord>>();

	public IncompatiblePlanRecords(
			final IncompatiblePlansIdentifier identifier,
			final Map<Id, PersonRecord> personRecords) {
		final HashSet<Id> ids = new HashSet<Id>();
		this.allIncompatibilityGroupIds = Collections.unmodifiableSet( ids );
		for ( PersonRecord person : personRecords.values() ) {
			for ( PlanRecord plan : person.plans ) {
				for ( Id group : identifyGroups( identifier , plan ) ) {
					MapUtils.getCollection( group , plansPerGroup ).add( plan );
					ids.add( group );
				}
				plan.setIncompatibilityGroups( identifyGroups( identifier , plan ) );
			}
		}
	}

	private static Set<Id> identifyGroups(
			final IncompatiblePlansIdentifier identifier,
			final PlanRecord plan) {
		return plan.jointPlan == null ?
			identifier.identifyIncompatibilityGroups( plan.plan ) :
			identifier.identifyIncompatibilityGroups( plan.jointPlan );
	}

	public Set<Id> getAllIncompatibilityGroupIds() {
		return allIncompatibilityGroupIds;
	}

	public Collection<PlanRecord> getIncompatiblePlans( final PlanRecord record ) {
		if ( record.getIncompatiblePlans() == null ) {
			record.setIncompatiblePlans(
					calcIncompatiblePlans(
						plansPerGroup,
						record));
		}

		return record.getIncompatiblePlans();
	}

	private static Collection<PlanRecord> calcIncompatiblePlans(
			final Map<Id, Collection<PlanRecord>> plansPerGroup,
			final PlanRecord record) {
		final Collection<PlanRecord> incompatible = new HashSet<PlanRecord>();

		addLinkedPlansOfOtherPlansOfPerson( incompatible , record );
		addLinkedPlansOfPartners( incompatible , record );
		addIncompatiblePlans( incompatible , plansPerGroup , record );

		return incompatible;
	}

	private static void addIncompatiblePlans(
			final Collection<PlanRecord> incompatible,
			final Map<Id, Collection<PlanRecord>> plansPerGroup,
			final PlanRecord record) {
		for ( Id group : record.getIncompatibilityGroups()  ) {
			incompatible.addAll( plansPerGroup.get( group ) );
		}
	}

	private static void addLinkedPlansOfOtherPlansOfPerson(
			final Collection<PlanRecord> incompatible,
			final PlanRecord record ) {
		for ( PlanRecord otherRecord : record.person.plans ) {
			if ( record.equals( otherRecord ) ) continue;
			incompatible.addAll( otherRecord.linkedPlans );
		}
	}

	private static void addLinkedPlansOfPartners(
			final Collection<PlanRecord> incompatible,
			final PlanRecord record ) {
		for ( PlanRecord linkedPlan : record.linkedPlans ) {
			final PersonRecord cotrav = linkedPlan.person;
			addLinkedPlansOfPerson( incompatible , cotrav );
		}
	}

	private static void addLinkedPlansOfPerson(
			final Collection<PlanRecord> incompatible,
			final PersonRecord person ) {
		for ( PlanRecord otherRecord : person.plans ) {
			incompatible.addAll( otherRecord.linkedPlans );
		}
	}

	public Set<Id> getIncompatibilityGroups(final PlanRecord pr) {
		return pr.getIncompatibilityGroups();
	}
}

