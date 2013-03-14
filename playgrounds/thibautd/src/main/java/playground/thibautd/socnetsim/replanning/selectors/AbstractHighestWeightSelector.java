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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
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
	private static final double EPSILON = 1E-7;
	private final boolean forbidBlockingCombinations;
	private final boolean pruneSimilarBranches;
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
		this( isForRemoval , exploreAll , true );
	}

	protected AbstractHighestWeightSelector(
			final boolean isForRemoval,
			final boolean exploreAll,
			final boolean pruneSimilarBranches) {
		this.forbidBlockingCombinations = isForRemoval;
		this.exploreAll = exploreAll;
		this.pruneSimilarBranches = pruneSimilarBranches;
	}

	// /////////////////////////////////////////////////////////////////////////
	// interface and abstract method
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public final GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		Map<Id, PersonRecord> personRecords = getPersonRecords( jointPlans , group );

		GroupPlans allocation = selectPlans( personRecords );

		return allocation;
	}

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
	public abstract double getWeight(
			final Plan indivPlan,
			final ReplanningGroup replanningGroup);

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
				final double w = getWeight( plan , group );
				if ( Double.isNaN( w ) ) throw new IllegalArgumentException( "NaN weights are not allowed" );
				weights.put( plan , w );
			}
		}

		final Map<JointPlan, Collection<PlanRecord>> recordsPerJp = new HashMap<JointPlan, Collection<PlanRecord>>();
		for (Person person : group.getPersons()) {
			final LinkedList<PlanRecord> plans = new LinkedList<PlanRecord>();
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
							return -Double.compare( o1.avgJointPlanWeight , o2.avgJointPlanWeight );
						}
					});
		}

		return map;
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
				new ArrayList<PersonRecord>( personRecords.values() ),
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

		assert !forbiden.isForbidden( allocation ) : "forbidden combination was re-examined";

		if (isBlocking( personRecords, allocation )) {
			forbiden.forbid( allocation );
			return true;
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
				Collections.<Id> emptySet());
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
			Collections.<PersonRecord> emptyList();

		final List<PlanRecord> records = new ArrayList<PlanRecord>( currentPerson.plans );

		final KnownBranches knownBranches = new KnownBranches( pruneSimilarBranches );
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

			final Set<Id> cotravelers = r.jointPlan == null ? null : r.jointPlan.getIndividualPlans().keySet();
			if ( knownBranches.isExplored( cotravelers ) ) continue;
			// if we do not find anything here, it is impossible to find allowed
			// plans with the remaining agents. No need to re-explore.
			knownBranches.tagAsExplored( cotravelers );

			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			Set<Id> actuallyAllocatedPersons = new HashSet<Id>(alreadyAllocatedPersons);
			actuallyAllocatedPersons.add( currentPerson.person.getId() );
			if (r.jointPlan != null) {
				if ( intersect( r.jointPlan.getIndividualPlans().keySet(), alreadyAllocatedPersons ) ) continue;
				actuallyRemainingPersons = filter( remainingPersons , r.jointPlan );
				actuallyAllocatedPersons.addAll( r.jointPlan.getIndividualPlans().keySet() );
			}

			if ( !actuallyRemainingPersons.isEmpty() ) {
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
			final List<PersonRecord> personsStillToAllocate,
			final PlanString str,
			final double minimalWeightToObtain) {
		final FeasibilityChanger feasibilityChanger = new FeasibilityChanger();
		final PersonRecord currentPerson = personsStillToAllocate.get(0);
		tagJointPlansOfPersonAsInfeasible(
				currentPerson,
				feasibilityChanger);

		assert str == null || !str.containsPerson( currentPerson.person.getId() );

		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			personsStillToAllocate.size() > 1 ?
			personsStillToAllocate.subList( 1, personsStillToAllocate.size() ) :
			Collections.<PersonRecord> emptyList();

		final FeasibilityChanger localFeasibilityChanger = new FeasibilityChanger();
		// get a list of plans in decreasing order of maximum possible weight.
		// The weight is always computed on the full joint plan, and thus consists
		// of the weight until now plus the upper bound
		final List<PlanRecord> records = new ArrayList<PlanRecord>( currentPerson.plans );
		final double alreadyAllocatedWeight = str == null ? 0 : str.getWeight();
		for (PlanRecord r : records) {
			if ( r.jointPlan != null ) {
				tagJointPlansOfPartnersAsInfeasible(
						r,
						localFeasibilityChanger);
			}
			r.cachedMaximumWeight = exploreAll ?
				Double.POSITIVE_INFINITY :
				alreadyAllocatedWeight +
					getMaxWeightFromPersons(
							r,
							str,
							remainingPersons );
			localFeasibilityChanger.resetFeasibilities();
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

		// get the actual allocation, and stop when the allocation
		// is better than the maximum possible in remaining plans
		// or worst than the worst possible at a higher level
		PlanString constructedString = null;
		// the plans got after this step only depend on the agents still to
		// allocate. We can stop at the first found solution.
		final KnownBranches knownBranches = new KnownBranches( pruneSimilarBranches );

		for (PlanRecord r : records) {
			if (!exploreAll &&
					constructedString != null &&
					r.cachedMaximumWeight <= constructedString.getWeight()) {
				break;
			}

			if (!exploreAll && r.cachedMaximumWeight < minimalWeightToObtain) {
				break;
			}

			if (!r.isStillFeasible) {
				assert intersect( r.jointPlan.getIndividualPlans().keySet() , str );
				continue;
			}

			final Set<Id> cotravelers = r.jointPlan == null ? null : r.jointPlan.getIndividualPlans().keySet();
			if ( knownBranches.isExplored( cotravelers ) ) continue;
			// if we find something, it is the best given the joint structure
			// (plans are sorted by avg joint plan weight): do not search more
			// for this particular structure.
			// If we do not find anything, trying again with the same structure
			// will not change anything.
			knownBranches.tagAsExplored( cotravelers );

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

				tagJointPlansOfPartnersAsInfeasible(
						r,
						localFeasibilityChanger);
			}


			PlanString newString;
			if ( !actuallyRemainingPersons.isEmpty() ) {
				newString = buildPlanString(
						forbidenCombinations,
						actuallyRemainingPersons,
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

			assert newString.getWeight() <= r.cachedMaximumWeight :
				getClass()+" weight higher than estimated max: "+newString.getWeight()+" > "+r.cachedMaximumWeight;

			if (constructedString == null ||
					newString.getWeight() > constructedString.getWeight()) {
				constructedString = newString;
			}
		}

		feasibilityChanger.resetFeasibilities();
		return constructedString;
	}

	private static void tagJointPlansOfPersonAsInfeasible(
			final PersonRecord person,
			final FeasibilityChanger changer) {
		for ( PlanRecord pr : person.plans ) {
			if ( pr.jointPlan == null ) continue;
			for ( PlanRecord p : pr.linkedPlans ) {
				changer.markInfeasible( p );
			}
		}
	}

	private static void tagJointPlansOfPartnersAsInfeasible(
			final PlanRecord r,
			final FeasibilityChanger changer) {
		if ( r.jointPlan == null ) return;
		final Id ego = r.plan.getPerson().getId();
		for ( PlanRecord linkedPlan : r.linkedPlans ) {
			final PersonRecord cotrav = linkedPlan.person;
			for ( PlanRecord pr : cotrav.plans ) {
				if ( pr.jointPlan == null ) continue;
				for ( PlanRecord colinkedPlan : pr.linkedPlans ) {
					if ( colinkedPlan.person.person.getId().equals( ego ) ) continue;
					final PersonRecord colinkedPerson = colinkedPlan.person;
					for ( PlanRecord colinkedpr : colinkedPerson.plans ) {
						if ( colinkedpr.isStillFeasible &&
								colinkedpr.jointPlan != null &&
								intersect(
									colinkedpr.jointPlan.getIndividualPlans().keySet() ,
									r.jointPlan.getIndividualPlans().keySet() )) {
							changer.markInfeasible( colinkedpr );
						}
					}
				}
			}
		}
	}

	/**
	 * Gets the maximum plan weight that can be obtained from the
	 * plans of remainingPersons, given the alradySelected has been
	 * selected, and that planToSelect is about to be selected.
	 */
	private static double getMaxWeightFromPersons(
			final PlanRecord planToSelect,
			final PlanString string,
			final List<PersonRecord> remainingPersons) {
		double score = planToSelect.avgJointPlanWeight;

		// if the plan to select is a joint plan,
		// we know exactly what plan to get the score from.
		final JointPlan jointPlanToSelect = planToSelect.jointPlan;

		for (PersonRecord record : remainingPersons) {
			final double max = getMaxWeight(
					// for assertions:
					planToSelect , string ,
					// actually useful:
					record , jointPlanToSelect );
			// if negative, no need to continue
			// moreover, returning here makes sure the branch has infinitely negative
			// weight, even if plans in it have infinitely positive weights
			if (max == Double.NEGATIVE_INFINITY) return Double.NEGATIVE_INFINITY;
			// avoid making the bound too tight, to avoid messing up with the rounding error
			score += max + EPSILON;
		}

		return score;
	}

	/**
	 * @return the highest weight of a plan wich does not pertains to a joint
	 * plan shared with agents in personsSelected
	 */
	private static double getMaxWeight(
			// arguments used for assertions
			final PlanRecord planToSelect,
			final PlanString string,
			// actual "useful" arguments
			final PersonRecord record,
			final JointPlan jointPlanToSelect) {
		// case in jp: plan is fully determined
		if (jointPlanToSelect != null) {
			final Plan plan = jointPlanToSelect.getIndividualPlan( record.person.getId() );
			if (plan != null) return record.getRecord( plan ).avgJointPlanWeight;
		}

		final Collection<Id> idsInJpToSelect =
			jointPlanToSelect == null ?
			Collections.<Id> emptySet() :
			jointPlanToSelect.getIndividualPlans().keySet();

		for (PlanRecord plan : record.plans) {
			// the plans are sorted by decreasing weight:
			// consider the first valid plan

			assert plan.jointPlan == null ||
				!plan.isStillFeasible ==
				plan.jointPlan.getIndividualPlans().containsKey( planToSelect.plan.getPerson().getId() ) ||
				intersect( plan.jointPlan.getIndividualPlans().keySet() , string ) ||
				intersect( plan.jointPlan.getIndividualPlans().keySet() , idsInJpToSelect );

			if ( plan.isStillFeasible ) return plan.avgJointPlanWeight;
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
			final PlanString additionalTail) {
		PlanString str = additionalTail;

		for (PlanRecord p : r.linkedPlans) {
			str = new PlanString( p , str );
		}

		return str;
	}

	private static boolean intersect(
			final Collection<Id> ids1,
			final Collection<Id> ids2) {
		final boolean moreIn1 = ids1.size() > ids2.size();

		// iterate over the smaller of the collections.
		// normally, the collections are HashSets, so that
		// contains is O(1): this can improve running time a lot
		// (from several minutes to a few seconds for plan removal!).
		final Collection<Id> iterated = moreIn1 ? ids2 : ids1;
		final Collection<Id> tested = moreIn1 ? ids1 : ids2;

		for (Id id : iterated) {
			if ( tested.contains( id ) ) return true;
		}

		return false;
	}

	private static boolean intersect(
			final Set<Id> set,
			final PlanString string) {
		if ( string == null ) return false;
		for ( Id id : set ) {
			if ( string.containsPerson( id ) ) return true;
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
			this.weight = head.avgJointPlanWeight + (tail == null ? 0 : tail.getWeight());
		}

		public double getWeight() {
			return weight;
		}

		public boolean containsPerson(final Id id) {
			return planRecord.plan.getPerson().getId().equals( id ) ||
				(tail != null && tail.containsPerson( id ));
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
							return -Double.compare( o1.avgJointPlanWeight , o2.avgJointPlanWeight );
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
		PersonRecord person;
		final Plan plan;
		/**
		 * The joint plan to which pertains the individual plan,
		 * if any.
		 */
		final JointPlan jointPlan;
		/**
		 * the plan records corresponding to the other plans in the joint plan
		 */
		final Collection<PlanRecord> linkedPlans = new HashSet<PlanRecord>();
		final double avgJointPlanWeight;
		double cachedMaximumWeight = Double.NaN;
		// true if all partners are still unallocated
		boolean isStillFeasible = true;

		public PlanRecord(
				final Plan plan,
				final JointPlan jointPlan,
				final double weight) {
			this.plan = plan;
			this.jointPlan = jointPlan;
			this.avgJointPlanWeight = weight;
		}

		@Override
		public String toString() {
			return "{PlanRecord: "+plan.getPerson().getId()+":"+plan.getScore()+
				" linkedWith:"+(jointPlan == null ? "[]" : jointPlan.getIndividualPlans().keySet())+
				" weight="+avgJointPlanWeight+"}";
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
					assert !forbidden.getIndividualPlans().contains( head.plan ) : "planString contains duplicates";
					return false;
				}
			}

			return true;
		}
	}

	private static class KnownBranches {
		private final boolean prune;
		private final List<Set<Id>> branches = new ArrayList<Set<Id>>();

		public KnownBranches(final boolean prune) {
			this.prune = prune;
		}

		public void tagAsExplored(final Set<Id> branch) {
			if (prune) branches.add( branch );
		}

		public boolean isExplored(final Set<Id> branch) {
			return prune && branches.contains( branch );
		}
	}

	private static class FeasibilityChanger {
		private final List<PlanRecord> changedRecords = new ArrayList<PlanRecord>();

		public void markInfeasible( final PlanRecord r ) {
			if ( r.isStillFeasible ) changedRecords.add( r );
			r.isStillFeasible = false;
		}

		public void resetFeasibilities() {
			for ( PlanRecord r : changedRecords ) r.isStillFeasible = true;
			changedRecords.clear();
		}
	}
}

