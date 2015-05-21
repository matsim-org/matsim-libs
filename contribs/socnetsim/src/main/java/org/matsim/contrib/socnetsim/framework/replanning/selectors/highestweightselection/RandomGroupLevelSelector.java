/* *********************************************************************** *
 * project: org.matsim.*
 * RandomGroupLevelSelector.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifierFactory;

/**
 * @author thibautd
 */
public class RandomGroupLevelSelector implements GroupLevelPlanSelector {
	private final Random random;
	private final IncompatiblePlansIdentifierFactory incompFactory;

	public RandomGroupLevelSelector(
			final Random random,
			final IncompatiblePlansIdentifierFactory incompFactory) {
		this.random = random;
		this.incompFactory = incompFactory;
	}
	
	// /////////////////////////////////////////////////////////////////////////
	// interface and abstract method
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public final GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final IncompatiblePlansIdentifier incompatiblePlansIdentifier =
			incompFactory.createIdentifier( jointPlans , group );
		final Map<Id, PersonRecord> personRecords =
			getPersonRecords(
					jointPlans,
					group );
		final IncompatiblePlanRecords incompatibleRecords =
			new IncompatiblePlanRecords(
					incompatiblePlansIdentifier,
					personRecords );

		final GroupPlans plans = new GroupPlans();
		final boolean found = searchForRandomCombination(
				incompatibleRecords,
				plans,
				new ArrayList<PersonRecord>(
					personRecords.values() ) );

		if ( !found ) {
			throw new RuntimeException( "could not find combination for group "+group );
		}

		assert plans.getAllIndividualPlans().size() == group.getPersons().size();

		return plans;
	}

	private static Map<Id, PersonRecord> getPersonRecords(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Map<Id, PersonRecord> map = new LinkedHashMap<Id, PersonRecord>();

		final Map<JointPlan, Collection<PlanRecord>> recordsPerJp = new HashMap<JointPlan, Collection<PlanRecord>>();
		for (Person person : group.getPersons()) {
			final List<PlanRecord> plans = new ArrayList<PlanRecord>();
			for (Plan plan : person.getPlans()) {
				final JointPlan jp = jointPlans.getJointPlan( plan );

				final PlanRecord r = new PlanRecord(
							plan,
							jp,
							0 );
				plans.add( r );
				if ( jp != null ) {
					Collection<PlanRecord> rs = recordsPerJp.get( jp );
					if ( rs == null ) {
						rs = new ArrayList<PlanRecord>();
						recordsPerJp.put( jp , rs );
					}
					rs.add( r );
				}
			}
			final PersonRecord pr = new PersonRecord( person , plans );
			map.put(
					person.getId(),
					pr );
			for ( PlanRecord p : plans ) {
				p.person = pr;
			}
		}

		for (PersonRecord personRecord : map.values()) {
			for ( PlanRecord pr : personRecord.plans ) {
				if ( pr.jointPlan == null ) continue;
				pr.linkedPlans.addAll( recordsPerJp.get( pr.jointPlan ) );
				pr.linkedPlans.remove( pr );
			}
		}

		return map;
	}

	private boolean searchForRandomCombination(
			final IncompatiblePlanRecords incompatibleRecords,
			final GroupPlans constructedPlan,
			final List<PersonRecord> personsStillToAllocate) {
		//if ( !remainsFeasible( personsStillToAllocate ) ) return false;
		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			new ArrayList<PersonRecord>( personsStillToAllocate );
		final PersonRecord currentPerson = remainingPersons.remove(0);

		for ( PersonRecord pr : remainingPersons ) {
			if ( !SelectorUtils.remainsPossible( pr.plans ) ) {
				return false;
			}
		}

		final Set<Branch> exploredBranches = new HashSet<Branch>();
		Collections.shuffle( currentPerson.plans , random );
		for ( PlanRecord r : currentPerson.plans ) {
			// skip forbidden plans
			if ( !r.isStillFeasible ) {
				continue;
			}

			final Set<Id<Person>> cotravs = r.jointPlan == null ?
				Collections.<Id<Person>>emptySet() :
				r.jointPlan.getIndividualPlans().keySet();

			if ( !exploredBranches.add(
						new Branch(
							cotravs,
							incompatibleRecords.getIncompatibilityGroups( r ) ) ) ) {
				continue;
			}

			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			if (r.jointPlan != null) {
				assert SelectorUtils.containsAllIds( personsStillToAllocate , r.jointPlan.getIndividualPlans().keySet() ); 
				actuallyRemainingPersons = SelectorUtils.filter( remainingPersons , r.jointPlan );
			}

			boolean found = true;
			if ( !actuallyRemainingPersons.isEmpty() ) {
				final FeasibilityChanger feasibilityChanger = new FeasibilityChanger();
				SelectorUtils.tagIncompatiblePlansAsInfeasible(
						r,
						incompatibleRecords,
						feasibilityChanger);

				found = searchForRandomCombination(
						incompatibleRecords,
						constructedPlan,
						actuallyRemainingPersons);

				feasibilityChanger.resetFeasibilities();
			}

			if (found) {
				if ( constructedPlan != null ) SelectorUtils.add( constructedPlan , r );
				return true;
			}
		}

		return false;
	}
}
