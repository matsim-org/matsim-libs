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
package playground.thibautd.socnetsim.replanning.selectors.highestweightselection;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifier;

/**
 * @author thibautd
 */
class IncompatiblePlanRecords {
	private final Map<PlanRecord, Collection<PlanRecord>> cachedIncompatiblePlans =
		new HashMap<PlanRecord, Collection<PlanRecord>>();

	public IncompatiblePlanRecords(
			final IncompatiblePlansIdentifier identifier,
			final Map<Id, PersonRecord> personRecords) {
		for ( PersonRecord person : personRecords.values() ) {
			for ( PlanRecord plan : person.plans ) {
				cachedIncompatiblePlans.put(
						plan,
						calcIncompatiblePlans(
							identifier,
							personRecords,
							plan));
			}
		}
	}

	public Collection<PlanRecord> getIncompatiblePlans( final PlanRecord record ) {
		return cachedIncompatiblePlans.get( record );
	}

	private static Collection<PlanRecord> calcIncompatiblePlans(
			final IncompatiblePlansIdentifier identifier,
			final Map<Id, PersonRecord> personRecords,
			final PlanRecord record) {
		final Collection<PlanRecord> incompatible = new HashSet<PlanRecord>();

		addLinkedPlansOfOtherPlansOfPerson( incompatible , record );
		addLinkedPlansOfPartners( incompatible , record );
		addIncompatiblePlans( incompatible , record , personRecords , identifier );

		return incompatible;
	}

	private static void addIncompatiblePlans(
			final Collection<PlanRecord> incompatible,
			final PlanRecord record,
			final Map<Id, PersonRecord> personRecords,
			final IncompatiblePlansIdentifier identifier) {
		final Collection<Plan> plans = identifier.identifyIncompatiblePlans( record.plan );

		for ( Plan p : plans ) {
			final PersonRecord person = personRecords.get( p.getPerson().getId() );
			final PlanRecord plan = person.getRecord( p );
			if ( plan == null ) throw new NullPointerException();
			incompatible.add( plan );
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
}

