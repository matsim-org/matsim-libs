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

import org.apache.log4j.Logger;
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
	private static final Logger log = Logger.getLogger( RandomGroupLevelSelector.class );
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
		final Queue<AnswerNode> plansStack = getInitialNodes( persons.get(0).plans );

		if ( log.isTraceEnabled() ) {
			log.trace( "search for random combination for persons "+persons );
		}

		while ( !plansStack.isEmpty() ) {
			final AnswerNode currentPlan = plansStack.remove();

			if ( log.isTraceEnabled() ) {
				log.trace( "examine "+currentPlan );
			}

			if ( currentPlan.type == AnswerNodeType.feasibility ) {
				currentPlan.resetFeasibilities();
				continue;
			}

			if ( currentPlan.record.isStillFeasible ) {

				final List<PersonRecord> actuallyRemainingPersons = remainingPersons(persons, currentPlan);

				if (!actuallyRemainingPersons.isEmpty()) {
					final FeasibilityChanger feasibilityChanger = new FeasibilityChanger();
					SelectorUtils.tagIncompatiblePlansAsInfeasible(
							currentPlan.record,
							incompatibleRecords,
							feasibilityChanger);
					markPlansOfCotravelersAsInfeasible( currentPlan , feasibilityChanger );
					markJointPlansAsInfeasible( currentPlan.record , feasibilityChanger );

					final PersonRecord nextPerson = actuallyRemainingPersons.get(0);
					plansStack.add( new AnswerNode( feasibilityChanger ) );
					for (PlanRecord r : shuffle(nextPerson.plans)) {
						// TODO: only add one plan per planComposition-incompatibility "branch"
						if (!r.isStillFeasible) continue;

						final AnswerNode newNode = new AnswerNode( r, currentPlan );
						if ( log.isTraceEnabled() ) {
							log.trace( "add to stack: "+newNode );
						}
						assert !containsSamePersonTwice( newNode ) : "plans="+ plansStack +"\n       answer="+ newNode;
						plansStack.add( newNode );

					}
				}
				else {
					final GroupPlans constructedPlan = new GroupPlans();
					for ( AnswerNode p : currentPlan ) {
						SelectorUtils.add( constructedPlan, p.record );
					}
					return constructedPlan;
				}
			}
		}

		return null;
	}

	private void markJointPlansAsInfeasible(
			final PlanRecord currentRecord,
			final FeasibilityChanger feasibilityChanger ) {
		for ( PlanRecord otherRecord : currentRecord.person.plans ) {
			if ( currentRecord == otherRecord ) continue;
			for ( PlanRecord linked : otherRecord.linkedPlans ) {
				feasibilityChanger.changeIfNecessary( linked );
			}
		}
	}

	private void markPlansOfCotravelersAsInfeasible(
			final AnswerNode currentPlan,
			final FeasibilityChanger feasibilityChanger ) {
		final Collection<PlanRecord> linkedPlans = currentPlan.record.linkedPlans;
		for ( PlanRecord linkedPlan : linkedPlans ) {
			for ( PlanRecord planOfCotraveler : linkedPlan.person.plans ) {
				if ( linkedPlan == planOfCotraveler ) continue;
				feasibilityChanger.changeIfNecessary( planOfCotraveler );
				markJointPlansAsInfeasible( planOfCotraveler , feasibilityChanger );
			}
		}
	}

	private Queue<AnswerNode> getInitialNodes(List<PlanRecord> plans ) {
		final Queue<AnswerNode> queue = Collections.asLifoQueue( new ArrayDeque<AnswerNode>() );
		for ( PlanRecord p : plans ) queue.add( new AnswerNode( p , null ) );
		return queue;
	}

	private boolean containsSamePersonTwice(AnswerNode answerStack) {
		final Set<Id<Person>> ps = new HashSet<>();

		for ( AnswerNode p : answerStack ) {
			if ( !ps.add( p.record.person.person.getId() ) ) return true;
			for ( PlanRecord l : p.record.linkedPlans ) {
				if ( !ps.add( l.person.person.getId() ) ) return true;
			}
		}

		return false;
	}

	private List<PersonRecord> remainingPersons(List<PersonRecord> persons, AnswerNode plansStack) {
		final List<PersonRecord> rem = new ArrayList<>( persons );
		for ( AnswerNode p : plansStack ) {
			rem.remove( p.record.person );
			for ( PlanRecord l : p.record.linkedPlans ) rem.remove( l.person );
		}
		return rem;
	}

	private <T> Collection<T> shuffle(List<T> plans) {
		Collections.shuffle( plans , random );
		return plans;
	}

	private enum AnswerNodeType {record, feasibility}

	private static class AnswerNode implements Iterable<AnswerNode> {
		private final PlanRecord record;
		private final AnswerNode parent;
		private final FeasibilityChanger feasibilityChanger;
		private final AnswerNodeType type;

		private AnswerNode(PlanRecord record, AnswerNode parent) {
			this.record = record;
			this.parent = parent;
			this.feasibilityChanger = null;
			this.type = AnswerNodeType.record;
		}

		private AnswerNode(FeasibilityChanger feasibilityChanger) {
			this.record = null;
			this.parent = null;
			this.feasibilityChanger = feasibilityChanger;
			this.type = AnswerNodeType.feasibility;
		}

		@Override
		public Iterator<AnswerNode> iterator() {
			return new Iterator<AnswerNode>() {
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				AnswerNode current = AnswerNode.this;

				@Override
				public boolean hasNext() {
					return current != null;
				}

				@Override
				public AnswerNode next() {
					AnswerNode n = current;
					current = current.parent;
					return n;
				}
			};
		}

		public void resetFeasibilities() {
			if ( feasibilityChanger == null ) return;
			feasibilityChanger.resetFeasibilities();
		}

		@Override
		public String toString() {
			final StringBuilder s = new StringBuilder( "AnswerNode{ " );
			for ( AnswerNode n : this ) s.append( n.record.toString() );
			if ( feasibilityChanger != null ) s.append( "; feasibilityChanging" );
			s.append( " } " );
			return s.toString();
		}
	}
}
