/* *********************************************************************** *
 * project: org.matsim.*
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifier;
import playground.thibautd.utils.MapUtils;

final class PersonRecord {
	final Person person;
	final List<PlanRecord> plans;
	final List<PlanRecord> prunedPlans;

	public PersonRecord(
			final Person person,
			final List<PlanRecord> plans,
			final IncompatiblePlansIdentifier incompatiblePlans) {
		this.person = person;
		this.plans = plans;
		Collections.sort(
				this.plans,
				new Comparator<PlanRecord>() {
					@Override
					public int compare(
							final PlanRecord o1,
							final PlanRecord o2) {
						// sort in DECREASING order
						return -Double.compare( o1.avgJointPlanWeight , o2.avgJointPlanWeight );
					}
				});

		if ( incompatiblePlans == null ) {
			prunedPlans = null;
			return;
		}

		// TODO: pass this in the selector, as it is very linked to the way it works
		prunedPlans = new ArrayList<PlanRecord>();
		final Map<Set<Id>, Set<Set<Id>>> knownBranches = new HashMap<Set<Id>, Set<Set<Id>>>();
		double lastRecordWeight = Double.POSITIVE_INFINITY;
		for (PlanRecord r : this.plans) {
			assert r.avgJointPlanWeight <= lastRecordWeight : this.plans;
			lastRecordWeight = r.avgJointPlanWeight;

			final Set<Id> cotravs = r.jointPlan == null ?
				Collections.<Id>emptySet() :
				r.jointPlan.getIndividualPlans().keySet();
			// only consider the best plan of each structure for each set of
			// incompatible plans.
			if ( MapUtils.getSet( cotravs , knownBranches ).add( incompatiblePlans.identifyIncompatibilityGroups( r.plan ) ) ) {
				prunedPlans.add( r );
			}
		}
	}

	public PlanRecord getRecord( final Plan plan ) {
		for (PlanRecord r : plans) {
			if (r.plan == plan) return r;
		}
		throw new IllegalArgumentException();
	}

	@Override
	public String toString() {
		return "{PersonRecord: person="+person+"; plans="+plans+"}";
	}
}
