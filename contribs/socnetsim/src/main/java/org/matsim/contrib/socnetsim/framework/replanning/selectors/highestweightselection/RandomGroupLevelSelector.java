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

import java.util.*;

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

		final GroupPlans plans = searchForRandomCombination(
				incompatibleRecords,
				new ArrayList<>(
					personRecords.values() ) );

		if ( plans == null ) {
			throw new RuntimeException( "could not find combination for group "+group );
		}

		assert plans.getAllIndividualPlans().size() == group.getPersons().size() : plans.getAllIndividualPlans()+" != "+group.getPersons();

		return plans;
	}

	private static Map<Id, PersonRecord> getPersonRecords(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Map<Id, PersonRecord> map = new LinkedHashMap<Id, PersonRecord>();

		final Map<JointPlan, Collection<PlanRecord>> recordsPerJp = new HashMap<>();
		for (Person person : group.getPersons()) {
			final List<PlanRecord> plans = new ArrayList<>();
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
						rs = new ArrayList<>();
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

	private GroupPlans searchForRandomCombination(
			final IncompatiblePlanRecords incompatibleRecords,
			final List<PersonRecord> persons) {
		final Queue<PlanRecord> plansStack =
				Collections.asLifoQueue(
						new ArrayDeque<>(
								shuffle(
										persons.get(0).plans)));

		final Queue<FeasibilityChanger> feasibilityStack =
				Collections.asLifoQueue(
						new ArrayDeque<FeasibilityChanger>());

		final FeasibilityChanger inactive = new FeasibilityChanger();
		feasibilityStack.add( inactive );

		final Queue<PlanRecord> answerStack =
				Collections.asLifoQueue(
						new ArrayDeque<PlanRecord>());

		while ( !plansStack.isEmpty() ) {
			final PlanRecord currentPlan = plansStack.remove();
			if ( currentPlan.isStillFeasible ) {
				if ( !answerStack.isEmpty() && answerStack.element().person == currentPlan.person ) {
					answerStack.remove();
				}
				answerStack.add( currentPlan );

				assert !containsSamePersonTwice( answerStack ) : "plans="+ plansStack +"\n       answer="+ answerStack+"\n      current="+currentPlan;

				final List<PersonRecord> actuallyRemainingPersons = remainingPersons(persons, answerStack);

				if (!actuallyRemainingPersons.isEmpty()) {
					final FeasibilityChanger feasibilityChanger = new FeasibilityChanger();
					SelectorUtils.tagIncompatiblePlansAsInfeasible(
							currentPlan,
							incompatibleRecords,
							feasibilityChanger);

					final PersonRecord nextPerson = actuallyRemainingPersons.get(0);
					boolean added = false;
					for (PlanRecord r : shuffle(nextPerson.plans)) {
						// TODO: only add one plan per planComposition-incompatibility "branch"
						if (!r.isStillFeasible) continue;
						plansStack.add(r);
						// add active changer at the beginning of the sequence,
						// to reset feasibilities when we "go out" of the person
						feasibilityStack.add(added ? inactive : feasibilityChanger);
						added = true;
					}
				}
				else {
					final GroupPlans constructedPlan = new GroupPlans();
					for (PlanRecord p : answerStack) {
						SelectorUtils.add(constructedPlan, p);
					}
					return constructedPlan;
				}
			}

			feasibilityStack.remove().resetFeasibilities();
		}

		return null;
	}

	private boolean containsSamePersonTwice(Queue<PlanRecord> answerStack) {
		final Set<Id<Person>> ps = new HashSet<>();

		for ( PlanRecord p : answerStack ) {
			if ( !ps.add( p.person.person.getId() ) ) return true;
			for ( PlanRecord l : p.linkedPlans ) {
				if ( !ps.add( l.person.person.getId() ) ) return true;
			}
		}

		return false;
	}

	private List<PersonRecord> remainingPersons(List<PersonRecord> persons, Queue<PlanRecord> plansStack) {
		final List<PersonRecord> rem = new ArrayList<>( persons );
		for ( PlanRecord p : plansStack ) {
			rem.remove( p.person );
			for ( PlanRecord l : p.linkedPlans ) rem.remove( l.person );
		}
		return rem;
	}

	private <T> Collection<T> shuffle(List<T> plans) {
		Collections.shuffle( plans , random );
		return plans;
	}

}
