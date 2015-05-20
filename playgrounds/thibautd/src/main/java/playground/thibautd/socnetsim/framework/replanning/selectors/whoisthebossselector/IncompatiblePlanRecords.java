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
package playground.thibautd.socnetsim.framework.replanning.selectors.whoisthebossselector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import org.matsim.core.utils.collections.MapUtils;
import playground.thibautd.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifier;

/**
 * @author thibautd
 */
final class IncompatiblePlanRecords {
	private final Set<Id> allIncompatibilityGroupIds;
	private final IncompatiblePlansIdentifier identifier;
	private final Map<PlanRecord, Collection<PlanRecord>> cachedIncompatiblePlans =
		new HashMap<PlanRecord, Collection<PlanRecord>>();

	public IncompatiblePlanRecords(
			final IncompatiblePlansIdentifier identifier,
			final Map<Id, PersonRecord> personRecords) {
		this.identifier = identifier;
		final Map<Id, Collection<PlanRecord>> plansPerGroup = new HashMap<Id, Collection<PlanRecord>>();

		final HashSet<Id> ids = new HashSet<Id>();
		this.allIncompatibilityGroupIds = Collections.unmodifiableSet( ids );
		for ( PersonRecord person : personRecords.values() ) {
			for ( PlanRecord plan : person.plans ) {
				for ( Id group : identifyGroups( identifier , plan ) ) {
					MapUtils.getCollection( group , plansPerGroup ).add( plan );
					ids.add( group );
				}
			}
		}

		for ( PersonRecord person : personRecords.values() ) {
			for ( PlanRecord plan : person.plans ) {
				cachedIncompatiblePlans.put(
						plan,
						calcIncompatiblePlans(
							identifier,
							plansPerGroup,
							plan));
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
		return cachedIncompatiblePlans.get( record );
	}

	private static Collection<PlanRecord> calcIncompatiblePlans(
			final IncompatiblePlansIdentifier identifier,
			final Map<Id, Collection<PlanRecord>> plansPerGroup,
			final PlanRecord record) {
		final Collection<PlanRecord> incompatible = new HashSet<PlanRecord>();

		addLinkedPlansOfOtherPlansOfPerson( incompatible , record );
		addLinkedPlansOfPartners( incompatible , record );
		addIncompatiblePlans( incompatible , plansPerGroup , record , identifier );

		return incompatible;
	}

	private static void addIncompatiblePlans(
			final Collection<PlanRecord> incompatible,
			final Map<Id, Collection<PlanRecord>> plansPerGroup,
			final PlanRecord record,
			final IncompatiblePlansIdentifier identifier) {
		for ( Id group : identifyGroups( identifier , record ) ) {
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
		return identifyGroups( identifier , pr );
	}
}

