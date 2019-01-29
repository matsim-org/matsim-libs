/* *********************************************************************** *
 * project: org.matsim.*
 * FullExplorationSelector.java
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Selects the plan combination with the highest (implementation specific)
 * weight.
 * <br>
 * To do so, it iteratively constructs the joint plan using a branch-and-bound
 * approach, which avoids exploring the full set of combinations.
 * @author thibautd
 */
public final class FullExplorationSelector implements GroupLevelPlanSelector {
	private static final double EPSILON = 1E-7;
	private final boolean forbidBlockingCombinations;
	private final WeightCalculator weightCalculator;
	private final IncompatiblePlansIdentifierFactory factory;

	public FullExplorationSelector(
			final IncompatiblePlansIdentifierFactory factory,
			final WeightCalculator weightCalculator) {
		this( false , factory , weightCalculator );
	}

	public FullExplorationSelector(
			final boolean isForRemoval,
			final IncompatiblePlansIdentifierFactory factory,
			final WeightCalculator weightCalculator) {
		this.forbidBlockingCombinations = isForRemoval;
		this.weightCalculator = weightCalculator;
		this.factory = factory;
	}
	
	public static interface WeightCalculator {
		/**
		 * Defines the weight of a plan, used for selection.
		 * The method is called once for each plan: it is not required that
		 * the method returns the same result if called twice with the same
		 * arguments (ie it can return a random number).
		 *
		 * @param indivPlan the plan to weight
		 * @param replanningGroup the group for which plans are being selected.
		 * Selectors using "niching" measures may need this. No modifications should
		 * be done to the group.
		 */
		public double getWeight(
				final Plan indivPlan,
				final ReplanningGroup replanningGroup);
	}

	// /////////////////////////////////////////////////////////////////////////
	// "translation" to and from the internal data structures
	// /////////////////////////////////////////////////////////////////////////
	private static GroupPlans toGroupPlans(final PlanString allocation) {
		Set<JointPlan> jointPlans = new HashSet<JointPlan>();
		List<Plan> individualPlans = new ArrayList<Plan>();
		for (PlanString curr = allocation;
				curr != null;
				curr = curr.tail) {
			if (curr.planRecord.jointPlan != null) {
				jointPlans.add( curr.planRecord.jointPlan );
			}
			else {
				individualPlans.add( curr.planRecord.plan );
			}
		}

		return new GroupPlans( jointPlans , individualPlans );
	}

	private Map<Id, PersonRecord> getPersonRecords(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Map<Id, PersonRecord> map = new LinkedHashMap<Id, PersonRecord>();
		final Map<Plan, Double> weights = new HashMap<Plan, Double>();

		for (Person person : group.getPersons()) {
			for (Plan plan : person.getPlans()) {
				final double w = weightCalculator.getWeight( plan , group );
				if ( Double.isNaN( w ) ) throw new IllegalArgumentException( "NaN weights are not allowed" );
				weights.put( plan , w );
			}
		}

		final Map<JointPlan, Collection<PlanRecord>> recordsPerJp = new HashMap<JointPlan, Collection<PlanRecord>>();
		for (Person person : group.getPersons()) {
			final List<PlanRecord> plans = new ArrayList<PlanRecord>();
			for (Plan plan : person.getPlans()) {
				double w = weights.get( plan );
				final JointPlan jp = jointPlans.getJointPlan( plan );

				if (jp != null) {
					for (Plan p : jp.getIndividualPlans().values()) {
						if (p == plan) continue;
						w += weights.get( p );
					}
					w /= jp.getIndividualPlans().size();
				}
				
				final PlanRecord r = new PlanRecord(
							plan,
							jp,
							w);
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

			Collections.sort(
					personRecord.plans,
					new Comparator<PlanRecord>() {
						@Override
						public int compare(
								final PlanRecord o1,
								final PlanRecord o2) {
							// sort in DECREASING order
							return Double.compare( o2.avgJointPlanWeight , o1.avgJointPlanWeight );
						}
					});
		}

		return map;
	}

	// /////////////////////////////////////////////////////////////////////////
	// "outer loop": search and forbid if blocking (if forbid blocking is true)
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public final GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Map<Id, PersonRecord> personRecords = getPersonRecords( jointPlans , group );
		final ForbidenPlanStrings forbiden = new ForbidenPlanStrings();
		final IncompatiblePlansIdentifier incompatiblePlans =
			factory.createIdentifier( jointPlans , group );

		GroupPlans plans = null;

		int count = 0;
		do {
			count++;
			final PlanString allocation = buildPlanString(
					incompatiblePlans,
					forbiden,
					new ArrayList<PersonRecord>( personRecords.values() ),
					Collections.<Id>emptySet(),
					null,
					Double.NEGATIVE_INFINITY);

			plans = allocation == null ? null : toGroupPlans( allocation );
		} while (
				plans != null &&
				continueIterations( incompatiblePlans , forbiden , personRecords , plans ) );

		assert forbidBlockingCombinations || count == 1 : count;
		assert plans == null || !forbiden.isForbidden( plans );

		return plans;
	}

	private boolean continueIterations(
			final IncompatiblePlansIdentifier incompatiblePlans,
			final ForbidenPlanStrings forbiden,
			final Map<Id, PersonRecord> personRecords,
			final GroupPlans allocation) {
		if ( !forbidBlockingCombinations ) return false;

		assert !forbiden.isForbidden( allocation ) : "forbidden combination was re-examined";

		if (isBlocking( incompatiblePlans , personRecords, allocation )) {
			forbiden.forbid( allocation );
			return true;
		}

		return false;
	}

	private boolean isBlocking(
			final IncompatiblePlansIdentifier incompatiblePlans,
			final Map<Id, PersonRecord> personRecords,
			final GroupPlans groupPlan) {
		return !searchForCombinationsWithoutForbiddenPlans(
				incompatiblePlans,
				groupPlan,
				personRecords,
				new ArrayList<PersonRecord>( personRecords.values() ),
				Collections.<Id> emptySet(),
				Collections.<Id<Person>> emptySet());
	}

	private boolean searchForCombinationsWithoutForbiddenPlans(
			final IncompatiblePlansIdentifier incompatiblePlans,
			final GroupPlans forbidenPlans,
			final Map<Id, PersonRecord> allPersonsRecord,
			final List<PersonRecord> personsStillToAllocate,
			final Set<Id> allocatedIncompatibilityGroups,
			final Set<Id<Person>> alreadyAllocatedPersons) {
		final PersonRecord currentPerson = personsStillToAllocate.get(0);

		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			personsStillToAllocate.size() > 1 ?
			personsStillToAllocate.subList( 1, personsStillToAllocate.size() ) :
			Collections.<PersonRecord> emptyList();

		final List<PlanRecord> records = new ArrayList<PlanRecord>( currentPerson.plans );

		final KnownBranches knownBranches = new KnownBranches( true );
		for (PlanRecord r : records) {
			// skip forbidden plans
			if ( r.jointPlan == null &&
					forbidenPlans.getIndividualPlans().contains( r.plan ) ) {
				continue;
			}
			if ( r.jointPlan != null &&
					forbidenPlans.getJointPlans().contains( r.jointPlan ) ) {
				continue;
			}

			final Set<Id> incomp = incompatiblePlans.identifyIncompatibilityGroups( r.plan );
			if ( !Collections.disjoint(
						allocatedIncompatibilityGroups,
						incomp) ) {
				continue;
			}

			final Set<Id<Person>> cotravelers = r.jointPlan == null ?
				Collections.<Id<Person>>emptySet() :
				r.jointPlan.getIndividualPlans().keySet();
			if ( knownBranches.isExplored( cotravelers , incomp ) ) continue;
			// if we do not find anything here, it is impossible to find allowed
			// plans with the remaining agents. No need to re-explore.
			knownBranches.tagAsExplored( cotravelers , incomp );

			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			Set<Id<Person>> actuallyAllocatedPersons = new HashSet< >(alreadyAllocatedPersons);
			actuallyAllocatedPersons.add( currentPerson.person.getId() );
			if (r.jointPlan != null) {
				if ( intersect( r.jointPlan.getIndividualPlans().keySet(), alreadyAllocatedPersons ) ) continue;
				actuallyRemainingPersons = filter( remainingPersons , r.jointPlan );
				actuallyAllocatedPersons.addAll( r.jointPlan.getIndividualPlans().keySet() );
			}

			if ( !actuallyRemainingPersons.isEmpty() ) {
				final Set<Id> newIncomp = new HashSet<Id>( allocatedIncompatibilityGroups );
				newIncomp.addAll( incomp );
				final boolean found =
					searchForCombinationsWithoutForbiddenPlans(
						incompatiblePlans,
						forbidenPlans,
						allPersonsRecord,
						actuallyRemainingPersons,
						newIncomp,
						actuallyAllocatedPersons);
				if (found) return true;
			}
			else {
				return true;
			}
		}

		return false;
	}

	// /////////////////////////////////////////////////////////////////////////
	// actual branching and bounding methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Recursively decends in the tree of possible joint plans.
	 *
	 * @param allPersonRecord helper map, just links persons to ids
	 * @param personsStillToAllocate in the name
	 * @param alreadyAllocatedPersons set of the ids of the already allocated persons,
	 * used to determine which joint plans are stil possible
	 * @param str the PlanString of the plan constructed until now
	 */
	private PlanString buildPlanString(
			final IncompatiblePlansIdentifier incompatiblePlans,
			final ForbidenPlanStrings forbidenCombinations,
			final List<PersonRecord> personsStillToAllocate,
			final Set<Id> allocatedIncompatibilityGroups,
			final PlanString str,
			final double minimalWeightToObtain) {
		final FeasibilityChanger feasibilityChanger = new FeasibilityChanger();

		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			new ArrayList<PersonRecord>( personsStillToAllocate );
		final PersonRecord currentPerson = remainingPersons.remove(0);
		// the joint plans implying this person will be considered "selectable"
		// only when considering this joint plan as being selected for currentPerson
		tagLinkedPlansOfPersonAsInfeasible(
				currentPerson,
				feasibilityChanger);

		assert str == null || !str.containsPerson( currentPerson.person.getId() );

		// get a list of plans in decreasing order of maximum possible weight.
		// The weight is always computed on the full joint plan, and thus consists
		// of the weight until now plus the upper bound
		final List<PlanRecord> records = new ArrayList<PlanRecord>( currentPerson.plans );

		// get the actual allocation, and stop when the allocation
		// is better than the maximum possible in remaining plans
		// or worst than the worst possible at a higher level
		PlanString constructedString = null;
		// the plans got after this step only depend on the agents still to
		// allocate. We can stop at the first found solution.
		// this is only useful when exploreAll is enabled (otherwise, only
		// the best plan per joint structure is kept)
		final KnownBranches knownBranches = new KnownBranches( true );

		final FeasibilityChanger localFeasibilityChanger = new FeasibilityChanger();
		double lastWeight = Double.POSITIVE_INFINITY;
		for (PlanRecord r : records) {
			assert lastWeight >= r.avgJointPlanWeight;
			lastWeight = r.avgJointPlanWeight;
			if (!r.isStillFeasible) {
				assert intersect( r.jointPlan.getIndividualPlans().keySet() , str );
				continue;
			}

			final Set<Id> incomp = incompatiblePlans.identifyIncompatibilityGroups( r.plan );
			if ( !Collections.disjoint(
						allocatedIncompatibilityGroups,
						incomp) ) {
				continue;
			}

			final Set<Id<Person>> cotravelers = r.jointPlan == null ?
				Collections.<Id<Person>>emptySet() :
				r.jointPlan.getIndividualPlans().keySet();
			if ( knownBranches.isExplored( cotravelers , incomp ) ) continue;
			// if we find something, it is the best given the joint structure
			// (plans are sorted by avg joint plan weight): do not search more
			// for this particular structure.
			// If we do not find anything, trying again with the same structure
			// will not change anything.
			knownBranches.tagAsExplored( cotravelers , incomp );

			PlanString tail = str;
			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			if (r.jointPlan != null) {
				// normally, it is impossible that it is always the case if there
				// is a valid plan: a branch were this would be the case would
				// have a infinitely negative weight and not explored.
				//if ( intersect( r.jointPlan.getIndividualPlans().keySet() , alreadyAllocatedPersons ) ) continue;
				// impossible if "isStillPossible" correctly implemented
				assert !intersect( r.jointPlan.getIndividualPlans().keySet() , str );
				tail = getOtherPlansAsString( r , tail);
				actuallyRemainingPersons = filter( remainingPersons , r.jointPlan );

				tagLinkedPlansOfPartnersAsInfeasible(
						r,
						localFeasibilityChanger);
			}


			PlanString newString;
			if ( !actuallyRemainingPersons.isEmpty() ) {
				final Set<Id> newIncomp = new HashSet<Id>( allocatedIncompatibilityGroups );
				newIncomp.addAll( incomp );
				newString = buildPlanString(
						incompatiblePlans,
						forbidenCombinations,
						actuallyRemainingPersons,
						newIncomp,
						new PlanString( r , tail ),
						Math.max(
							minimalWeightToObtain,
							constructedString != null ?
								constructedString.getWeight() - EPSILON :
								Double.NEGATIVE_INFINITY));
			}
			else {
				newString = new PlanString( r , tail );

				if ( forbidBlockingCombinations && forbidenCombinations.isForbidden( newString ) ) {
					// we are on a leaf (ie a full plan).
					// If some combinations are forbidden, check if this one is.
					newString = null;
				}
			}
			localFeasibilityChanger.resetFeasibilities();

			if (newString == null) continue;

			if (constructedString == null ||
					newString.getWeight() > constructedString.getWeight()) {
				constructedString = newString;
			}
		}

		feasibilityChanger.resetFeasibilities();
		return constructedString;
	}

	private static void tagLinkedPlansOfPersonAsInfeasible(
			final PersonRecord person,
			final FeasibilityChanger changer) {
		for ( PlanRecord pr : person.plans ) {
			if ( pr.jointPlan == null ) {
				assert pr.linkedPlans.isEmpty();
				continue;
			}
			for ( PlanRecord p : pr.linkedPlans ) {
				changer.changeIfNecessary( p );
			}
		}
	}

	private static void tagLinkedPlansOfPartnersAsInfeasible(
			final PlanRecord r,
			final FeasibilityChanger changer) {
		assert !r.linkedPlans.contains( r );
		for ( PlanRecord linkedPlan : r.linkedPlans ) {
			final PersonRecord cotrav = linkedPlan.person;
			tagLinkedPlansOfPersonAsInfeasible( cotrav , changer );
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// various small helper methods
	// /////////////////////////////////////////////////////////////////////////
	private static List<PersonRecord> filter(
			final List<PersonRecord> toFilter,
			final JointPlan jointPlan) {
		List<PersonRecord> newList = new ArrayList<PersonRecord>();

		for (PersonRecord r : toFilter) {
			if (!jointPlan.getIndividualPlans().containsKey( r.person.getId() )) {
				newList.add( r );
			}
		}

		return newList;
	}

	private static PlanString getOtherPlansAsString(
			final PlanRecord r,
			final PlanString additionalTail) {
		PlanString str = additionalTail;

		for (PlanRecord p : r.linkedPlans) {
			str = new PlanString( p , str );
		}

		return str;
	}

	private static <T> boolean intersect(
			final Collection<T> ids1,
			final Collection<T> ids2) {
		final boolean moreIn1 = ids1.size() > ids2.size();

		// iterate over the smaller of the collections.
		// normally, the collections are HashSets, so that
		// contains is O(1): this can improve running time a lot
		// (from several minutes to a few seconds for plan removal!).
		final Collection<T> iterated = moreIn1 ? ids2 : ids1;
		final Collection<T> tested = moreIn1 ? ids1 : ids2;

		for (T id : iterated) {
			if ( tested.contains( id ) ) return true;
		}

		return false;
	}

	private static boolean intersect(
			final Set<Id<Person>> set,
			final PlanString string) {
		if ( string == null ) return false;
		for ( Id id : set ) {
			if ( string.containsPerson( id ) ) return true;
		}
		return false;
	}
}
