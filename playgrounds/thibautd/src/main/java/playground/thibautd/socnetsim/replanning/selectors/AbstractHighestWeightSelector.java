/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractHighestWeightSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * Selects the plan combination with the highest (implementation specific)
 * weight.
 * <br>
 * To do so, it iteratively constructs the joint plan using a branch-and-bound
 * approach, which avoids exploring the full set of combinations.
 * @author thibautd
 */
public abstract class AbstractHighestWeightSelector implements GroupLevelPlanSelector {
	private static final Logger log =
		Logger.getLogger(AbstractHighestWeightSelector.class);

	private final boolean forbidBlockingCombinations;
	private final boolean exploreAll;

	protected AbstractHighestWeightSelector() {
		this( false );
	}

	protected AbstractHighestWeightSelector(final boolean isForRemoval) {
		this( isForRemoval , false );
	}

	/**
	 * @param exploreAll for test purposes
	 */
	protected AbstractHighestWeightSelector(
			final boolean isForRemoval,
			final boolean exploreAll) {
		this.forbidBlockingCombinations = isForRemoval;
		this.exploreAll = exploreAll;
	}

	// /////////////////////////////////////////////////////////////////////////
	// interface and abstract method
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public final GroupPlans selectPlans(final ReplanningGroup group) {
		if (log.isTraceEnabled()) log.trace( "handling group "+group );
		Map<Id, PersonRecord> personRecords = getPersonRecords( group );

		GroupPlans allocation = selectPlans( personRecords );

		if (log.isTraceEnabled()) log.trace( "returning allocation "+allocation );
		return allocation;
	}

	public abstract double getWeight(final Plan indivPlan);

	// /////////////////////////////////////////////////////////////////////////
	// "translation" to and from the internal data structures
	// /////////////////////////////////////////////////////////////////////////
	private GroupPlans toGroupPlans(final PlanString allocation) {
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

	private Map<Id, PersonRecord> getPersonRecords(final ReplanningGroup group) {
		final Map<Id, PersonRecord> map = new LinkedHashMap<Id, PersonRecord>();

		for (Person person : group.getPersons()) {
			final LinkedList<PlanRecord> plans = new LinkedList<PlanRecord>();
			for (Plan plan : person.getPlans()) {
				plans.add( new PlanRecord(
							plan,
							JointPlanFactory.getPlanLinks().getJointPlan( plan ),
							getWeight( plan )));
			}
			map.put(
					person.getId(),
					new PersonRecord( person , plans ) );
		}

		for (PersonRecord personRecord : map.values()) {
			Collections.sort(
					personRecord.plans,
					new Comparator<PlanRecord>() {
						@Override
						public int compare(
								final PlanRecord o1,
								final PlanRecord o2) {
							// sort in DECREASING order
							return -Double.compare( o1.weight , o2.weight );
						}
					});
			pruneUnplausiblePlans( personRecord );
		}

		return map;
	}

	/**
	 * removes all plans that will not be selected for sure,
	 * by keeping only the best joint plan for each agent combination.
	 * It assumes the plans sorted.
	 * It increases DRAMATICALLY the performance (several hundreds times faster),
	 * probably both due to reduced problem sized and tighter lower bounds
	 */
	private void pruneUnplausiblePlans(
			final PersonRecord personRecord) {
		if (exploreAll) return;
		final Iterator<PlanRecord> plans = personRecord.plans.iterator();
		final int maxIndivPlans = forbidBlockingCombinations ? 2 : 1;
		int nIndivPlans = 0;
		//final Set<Set<Id>> jointPlansGroups = new HashSet<Set<Id>>();

		while (plans.hasNext()) {
			final PlanRecord plan = plans.next();

			if (plan.jointPlan == null) {
				nIndivPlans++;
				if (nIndivPlans > maxIndivPlans) plans.remove();
			}
			//else {
			//	final Set<Id> group = plan.jointPlan.getIndividualPlans().keySet();
			//	if (!jointPlansGroups.add( group )) plans.remove();
			//}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// "outer loop": search and forbid if blocking (if forbid blocking is true)
	// /////////////////////////////////////////////////////////////////////////
	private GroupPlans selectPlans( final Map<Id, PersonRecord> personRecords ) {
		final ForbidenCombinations forbiden = new ForbidenCombinations();

		GroupPlans plans = null;

		int count = 0;
		do {
			count++;
			final PlanString allocation = buildPlanString(
				forbiden,
				personRecords,
				new ArrayList<PersonRecord>( personRecords.values() ),
				Collections.EMPTY_SET,
				null,
				Double.NEGATIVE_INFINITY);

			plans = allocation == null ? null : toGroupPlans( allocation );
		} while (
				plans != null &&
				continueIterations( forbiden , personRecords , plans ) );

		assert forbidBlockingCombinations || count == 1 : count;
		assert plans == null || !forbiden.isForbidden( plans );

		return plans;
	}

	private boolean continueIterations(
			final ForbidenCombinations forbiden,
			final Map<Id, PersonRecord> personRecords,
			final GroupPlans allocation) {
		if ( !forbidBlockingCombinations ) return false;

		if (log.isTraceEnabled()) log.trace( "checking if need to continue" );
		assert !forbiden.isForbidden( allocation ) : "forbidden combination was re-examined";

		if (isBlocking( personRecords, allocation )) {
			if (log.isTraceEnabled()) {
				log.trace( allocation+" is blocking" );
			}

			forbiden.forbid( allocation );
			return true;
		}

		if (log.isTraceEnabled()) {
			log.trace( allocation+" is not blocking" );
		}

		return false;
	}

	private boolean isBlocking(
			final Map<Id, PersonRecord> personRecords,
			final GroupPlans groupPlan) {
		return !searchForCombinationsWithoutForbiddenPlans(
				groupPlan,
				personRecords,
				new ArrayList<PersonRecord>( personRecords.values() ),
				Collections.EMPTY_SET);
	}

	private boolean searchForCombinationsWithoutForbiddenPlans(
			final GroupPlans forbidenPlans,
			final Map<Id, PersonRecord> allPersonsRecord,
			final List<PersonRecord> personsStillToAllocate,
			final Set<Id> alreadyAllocatedPersons) {
		final PersonRecord currentPerson = personsStillToAllocate.get(0);

		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			personsStillToAllocate.size() > 1 ?
			personsStillToAllocate.subList( 1, personsStillToAllocate.size() ) :
			Collections.EMPTY_LIST;
		final Set<Id> newAllocatedPersons = new HashSet<Id>(alreadyAllocatedPersons);
		newAllocatedPersons.add( currentPerson.person.getId() );

		List<PlanRecord> records = new ArrayList<PlanRecord>( currentPerson.plans );

		for (PlanRecord r : records) {
			// skip impossible plans
			if ( r.jointPlan == null &&
					forbidenPlans.getIndividualPlans().contains( r.plan ) ) {
				continue;
			}
			if ( r.jointPlan != null &&
					forbidenPlans.getJointPlans().contains( r.jointPlan ) ) {
				continue;
			}

			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			Set<Id> actuallyAllocatedPersons = newAllocatedPersons;
			JointPlan jointPlan = r.jointPlan ;
			if (jointPlan != null) {
				// normally, it is impossible that it is always the case if there
				// is a valid plan: a branch were this would be the case would
				// have a infinitely negative weight and not explored.
				if ( contains( jointPlan , alreadyAllocatedPersons ) ) continue;
				actuallyRemainingPersons = filter( remainingPersons , jointPlan );
				actuallyAllocatedPersons = new HashSet<Id>( actuallyAllocatedPersons );
				actuallyAllocatedPersons.addAll( jointPlan.getIndividualPlans().keySet() );
			}

			if ( actuallyRemainingPersons.size() > 0 ) {
				final boolean found = searchForCombinationsWithoutForbiddenPlans(
						forbidenPlans,
						allPersonsRecord,
						actuallyRemainingPersons,
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
			final ForbidenCombinations forbidenCombinations,
			final Map<Id, PersonRecord> allPersonsRecord,
			final List<PersonRecord> personsStillToAllocate,
			final Set<Id> alreadyAllocatedPersons,
			final PlanString str,
			final double minimalWeightToObtain) {
		final PersonRecord currentPerson = personsStillToAllocate.get(0);

		assert !alreadyAllocatedPersons.contains( currentPerson.person.getId() ) :
			"still to allocate: "+personsStillToAllocate+
			 ", already allocated: "+alreadyAllocatedPersons;
		if (log.isTraceEnabled()) {
			log.trace( "looking ar person "+currentPerson.person.getId()+
					" with already selected "+alreadyAllocatedPersons );
		}

		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			personsStillToAllocate.size() > 1 ?
			personsStillToAllocate.subList( 1, personsStillToAllocate.size() ) :
			Collections.EMPTY_LIST;
		final Set<Id> newAllocatedPersons = new HashSet<Id>(alreadyAllocatedPersons);
		newAllocatedPersons.add( currentPerson.person.getId() );

		// get a list of plans in decreasing order of maximum possible weight.
		// The weight is always computed on the full joint plan, and thus consists
		// of the weight until now plus the upper bound
		final List<PlanRecord> records = new ArrayList<PlanRecord>( currentPerson.plans );
		final double alreadyAllocatedWeight = str == null ? 0 : str.getWeight();
		for (PlanRecord r : records) {
			r.cachedMaximumWeight = exploreAll ?
				Double.POSITIVE_INFINITY :
				alreadyAllocatedWeight +
					getMaxWeightFromPersons(
							r,
							newAllocatedPersons,
							remainingPersons );

			r.cachedMinimumWeight = exploreAll ?
				Double.NEGATIVE_INFINITY :
				alreadyAllocatedWeight +
					getMinWeightFromPersons(
							r,
							newAllocatedPersons,
							remainingPersons );
		}

		// Sort in decreasing order of upper bound: we can stop as soon
		// as the constructed plan has weight greater than the upper bound
		// of the next branch.
		Collections.sort(
				records,
				new Comparator<PlanRecord>() {
					@Override
					public int compare(
							final PlanRecord o1,
							final PlanRecord o2) {
						// sort in DECREASING order
						return -Double.compare(
							o1.cachedMaximumWeight,
							o2.cachedMaximumWeight );
					}
				});
		final LinkedList<PlanRecord> stillToExploreSortedByLowerBound =
			new LinkedList<PlanRecord>( records );
		Collections.sort(
				stillToExploreSortedByLowerBound,
				new Comparator<PlanRecord>() {
					@Override
					public int compare(
							final PlanRecord o1,
							final PlanRecord o2) {
						return Double.compare(
							o1.cachedMinimumWeight,
							o2.cachedMinimumWeight );
					}
				});
		assert stillToExploreSortedByLowerBound.size() == records.size();

		// get the actual allocation, and stop when the allocation
		// is better than the maximum possible in remaining plans
		PlanString constructedString = null;

		for (PlanRecord r : records) {
			if (constructedString != null &&
					r.cachedMaximumWeight <= constructedString.getWeight()) {
				if (log.isTraceEnabled()) {
					log.trace( "maximum weight from now on: "+r.cachedMaximumWeight );
					log.trace( "weight obtained: "+constructedString.getWeight() );
					log.trace( " => CUTOFF by upper bound" );
				}
				break;
			}

			if (r.cachedMaximumWeight < minimalWeightToObtain) {
				if (log.isTraceEnabled()) {
					log.trace( "maximum weight from now on: "+r.cachedMaximumWeight );
					log.trace( "minimum weight to obtain: "+minimalWeightToObtain );
					log.trace( " => CUTOFF by lower bound" );
				}
				break;
			}

			stillToExploreSortedByLowerBound.remove( r );
			final double newMinimumWeightToObtain =
					stillToExploreSortedByLowerBound.size() > 0 ?
					stillToExploreSortedByLowerBound.getLast().cachedMinimumWeight :
					Double.NEGATIVE_INFINITY;

			PlanString tail = str;
			// TODO: find a better way to filter persons (should be
			// possible in PlanString), ie without having to create new collections
			// over and over, which is messy and inefficient
			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			Set<Id> actuallyAllocatedPersons = newAllocatedPersons;
			if (r.jointPlan != null) {
				// normally, it is impossible that it is always the case if there
				// is a valid plan: a branch were this would be the case would
				// have a infinitely negative weight and not explored.
				if ( contains( r.jointPlan , alreadyAllocatedPersons ) ) continue;
				tail = getOtherPlansAsString( r , allPersonsRecord , tail);
				actuallyRemainingPersons = filter( remainingPersons , r.jointPlan );
				actuallyAllocatedPersons = new HashSet<Id>( newAllocatedPersons );
				actuallyAllocatedPersons.addAll( r.jointPlan.getIndividualPlans().keySet() );
			}

			PlanString newString;
			if ( actuallyRemainingPersons.size() > 0 ) {
				newString = buildPlanString(
						forbidenCombinations,
						allPersonsRecord,
						actuallyRemainingPersons,
						actuallyAllocatedPersons,
						new PlanString( r , tail ),
						max(
							minimalWeightToObtain,
							newMinimumWeightToObtain,
							constructedString != null ?
								constructedString.getWeight() :
								Double.NEGATIVE_INFINITY));
			}
			else {
				newString = new PlanString( r , tail );

				if ( forbidBlockingCombinations && forbidenCombinations.isForbidden( newString ) ) {
					// we are on a leaf (ie a full plan).
					// If some combinations are forbidden, check if this one is.
					if ( log.isTraceEnabled() ) log.trace( "skipping forbiden string "+newString );
					newString = null;
				}
			}

			if (newString == null) continue;
			if (constructedString == null ||
					newString.getWeight() > constructedString.getWeight()) {
				constructedString = newString;
				if (log.isTraceEnabled()) log.trace( "new string "+constructedString+" with weight "+constructedString.getWeight() );
			}
			else if (log.isTraceEnabled()) {
				log.trace( "string "+newString+" with weight "+newString.getWeight()+" did not improve" );
			}
		}

		return constructedString;
	}

	private double max(
			final double d1,
			final double d2,
			final double d3) {
		return d1 > d2 ? Math.max( d1 , d3 ) : Math.max( d2 , d3 );
	}

	/**
	 * Gets the maximum plan weight that can be obtained from the
	 * plans of remainingPersons, given the alradySelected has been
	 * selected, and that planToSelect is about to be selected.
	 */
	private static double getMaxWeightFromPersons(
			final PlanRecord planToSelect,
			// the joint plans linking to persons with a plan
			// already selected cannot be selected.
			// This list contains the agent to be selected.
			final Collection<Id> personsSelected,
			final List<PersonRecord> remainingPersons) {
		double score = planToSelect.weight;

		// if the plan to select is a joint plan,
		// we know exactly what plan to get the score from.
		final JointPlan jointPlanToSelect = planToSelect.jointPlan;

		for (PersonRecord record : remainingPersons) {
			final double max = getMaxWeight( record , personsSelected , jointPlanToSelect );
			if (log.isTraceEnabled()) {
				log.trace( "estimated max weight for person "+
						record.person.getId()+
						" is "+max );
			}
			// if negative, no need to continue
			// moreover, returning here makes sure the branch has infinitely negative
			// weight, even if plans in it have infinitely positive weights
			if (max == Double.NEGATIVE_INFINITY) return Double.NEGATIVE_INFINITY;
			score += max;
		}

		return score;
	}

	/**
	 * @return the highest weight of a plan wich does not pertains to a joint
	 * plan shared with agents in personsSelected
	 */
	private static double getMaxWeight(
			final PersonRecord record,
			final Collection<Id> personsSelected,
			final JointPlan jointPlanToSelect) {
		// case in jp: plan is fully determined
		if (jointPlanToSelect != null) {
			final Plan plan = jointPlanToSelect.getIndividualPlan( record.person.getId() );
			if (plan != null) return record.getRecord( plan ).weight;
		}

		final Collection<Id> idsInJpToSelect =
			jointPlanToSelect == null ?
			Collections.EMPTY_SET :
			jointPlanToSelect.getIndividualPlans().keySet();

		for (PlanRecord plan : record.plans) {
			// the plans are sorted by decreasing weight:
			// consider the first valid plan

			if (plan.jointPlan == null) return plan.weight;

			// skip this plan if its participants already have a plan
			if (contains( plan.jointPlan , personsSelected )) continue;
			if (contains( plan.jointPlan , idsInJpToSelect )) continue;
			return plan.weight;
		}

		// this combination is impossible
		return Double.NEGATIVE_INFINITY;
	}

	/**
	 * Gets the maximum plan weight that can be obtained from the
	 * plans of remainingPersons, given the alradySelected has been
	 * selected, and that planToSelect is about to be selected.
	 */
	private static double getMinWeightFromPersons(
			final PlanRecord planToSelect,
			// the joint plans linking to persons with a plan
			// already selected cannot be selected.
			// This list contains the agent to be selected.
			final Collection<Id> personsSelected,
			final List<PersonRecord> remainingPersons) {
		double score = planToSelect.weight;

		// if the plan to select is a joint plan,
		// we know exactly what plan to get the score from.
		final JointPlan jointPlanToSelect = planToSelect.jointPlan;

		for (PersonRecord record : remainingPersons) {
			final double min = getMinWeight( record , personsSelected , jointPlanToSelect );
			if (log.isTraceEnabled()) {
				log.trace( "estimated min weight for person "+
						record.person.getId()+
						" is "+min );
			}
			// if infinite, no need to continue
			// moreover, returning here makes sure the branch has infinitely positive
			// weight, even if plans in it have infinitely negative weights
			if (min == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
			score += min;
		}

		return score;
	}

	/**
	 * @return the lowest weight of a plan wich does not pertains to a joint
	 * plan shared with agents in personsSelected
	 */
	private static double getMinWeight(
			final PersonRecord record,
			final Collection<Id> personsSelected,
			final JointPlan jointPlanToSelect) {
		// case in jp: plan is fully determined
		if (jointPlanToSelect != null) {
			final Plan plan = jointPlanToSelect.getIndividualPlan( record.person.getId() );
			if (plan != null) return record.getRecord( plan ).weight;
		}

		final Collection<Id> idsInJpToSelect =
			jointPlanToSelect == null ?
			Collections.EMPTY_SET :
			jointPlanToSelect.getIndividualPlans().keySet();

		for (Iterator<PlanRecord> it = record.plans.descendingIterator();
				it.hasNext(); ) {
			final PlanRecord plan = it.next();
			// the plans are iterated in increasing weight order:
			// consider the first valid plan

			if (plan.jointPlan == null) return plan.weight;

			// skip this plan if its participants already have a plan
			if (contains( plan.jointPlan , personsSelected )) continue;
			if (contains( plan.jointPlan , idsInJpToSelect )) continue;
			return plan.weight;
		}

		// this combination is impossible
		return Double.NEGATIVE_INFINITY;
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
			final Map<Id, PersonRecord> allPersonsRecords,
			final PlanString additionalTail) {
		PlanString str = additionalTail;

		for (Plan p : r.jointPlan.getIndividualPlans().values()) {
			if (p == r.plan) continue;

			str = new PlanString(
					allPersonsRecords.get( p.getPerson().getId() ).getRecord( p ),
					str);
		}

		return str;
	}

	private static boolean contains(
			final JointPlan jp,
			final Collection<Id> personsSelected) {
		for (Id id : personsSelected) {
			if (jp.getIndividualPlans().containsKey( id )) return true;
		}
		return false;
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes: data structures used during the search process
	// /////////////////////////////////////////////////////////////////////////

	private static final class PlanString {
		public final PlanRecord planRecord;
		public final PlanString tail;
		private final double weight;

		public PlanString(
				final PlanRecord head,
				final PlanString tail) {
			this.planRecord = head;
			this.tail = tail;
			this.weight = head.weight + (tail == null ? 0 : tail.getWeight());
		}

		public double getWeight() {
			return weight;
		}

		@Override
		public String toString() {
			return "("+planRecord+"; "+tail+")";
		}
	}

	private static class PersonRecord {
		final Person person;
		final LinkedList<PlanRecord> plans;

		public PersonRecord(
				final Person person,
				final LinkedList<PlanRecord> plans) {
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
							return -Double.compare( o1.weight , o2.weight );
						}
					});
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

	private static class PlanRecord {
		final Plan plan;
		/**
		 * The joint plan to which pertains the individual plan,
		 * if any.
		 */
		final JointPlan jointPlan;
		final double weight;
		double cachedMaximumWeight = Double.NaN;
		double cachedMinimumWeight = Double.NaN;

		public PlanRecord(
				final Plan plan,
				final JointPlan jointPlan,
				final double weight) {
			this.plan = plan;
			this.jointPlan = jointPlan;
			this.weight = weight;
		}

		@Override
		public String toString() {
			return "{PlanRecord: "+plan.getPerson().getId()+":"+plan.getScore()+
				" linkedWith:"+(jointPlan == null ? "[]" : jointPlan.getIndividualPlans().keySet())+
				" weight="+weight+"}";
		}
	}

	private static class ForbidenCombinations {
		private final List<GroupPlans> forbidden = new ArrayList<GroupPlans>();

		public void forbid(final GroupPlans plans) {
			forbidden.add( plans );
		}

		public boolean isForbidden(final PlanString ps) {
			for (GroupPlans p : forbidden) {
				if ( forbids( p , ps ) ) return true;
			}
			return false;
		}

		public boolean isForbidden(final GroupPlans groupPlans) {
			return forbidden.contains( groupPlans );
		}

		private static boolean forbids(
				final GroupPlans forbidden,
				final PlanString string) {
			PlanString tail = string;

			// check if all plans in the string are in the groupPlans
			// copying the list and removing the elements is much faster
			// than using "contains" on big lists.
			final List<Plan> plans = new ArrayList<Plan>( forbidden.getIndividualPlans() );
			while (tail != null) {
				final PlanRecord head = tail.planRecord;
				tail = tail.tail;

				if (head.jointPlan != null &&
						!forbidden.getJointPlans().contains( head.jointPlan )) {
					return false;
				}

				if (head.jointPlan == null &&
						!plans.remove( head.plan )) {
					return false;
				}
			}

			return true;
		}
	}
}

