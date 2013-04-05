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
import java.util.HashSet;

/**
 * @author thibautd
 */
class IncompatiblePlanRecords {
	public Collection<PlanRecord> getIncompatiblePlans( final PlanRecord record ) {
		final Collection<PlanRecord> incompatible = new HashSet<PlanRecord>();

		addLinkedPlansOfOtherPlansOfPerson( incompatible , record );
		addLinkedPlansOfPartners( incompatible , record );

		return incompatible;
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

