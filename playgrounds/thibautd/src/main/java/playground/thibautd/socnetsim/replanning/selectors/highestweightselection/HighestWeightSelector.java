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
package playground.thibautd.socnetsim.replanning.selectors.highestweightselection;

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

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;

/**
 * Selects the plan combination with the highest (implementation specific)
 * weight.
 * <br>
 * To do so, it iteratively constructs the joint plan using a branch-and-bound
 * approach, which avoids exploring the full set of combinations.
 * @author thibautd
 */
public final class HighestWeightSelector implements GroupLevelPlanSelector {
	private static final double EPSILON = 1E-7;
	private final boolean forbidBlockingCombinations;
	private final WeightCalculator weightCalculator;

	public HighestWeightSelector(final WeightCalculator weightCalculator) {
		this( false , weightCalculator );
	}

	public HighestWeightSelector(
			final boolean isForRemoval,
			final WeightCalculator weightCalculator) {
		this.forbidBlockingCombinations = isForRemoval;
		this.weightCalculator = weightCalculator;
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
	// interface and abstract method
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public final GroupPlans selectPlans(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Map<Id, PersonRecord> personRecords = getPersonRecords( jointPlans , group );
		final ForbidenCombinations forbiden = new ForbidenCombinations();

		GroupPlans plans = null;

		int count = 0;
		do {
			count++;
			final PlanAllocation allocation = buildPlanString(
					new KnownStates(),
					forbiden,
					new IncompatiblePlanRecords(
						incompFactory.createIdentifier( jointPlans , group ),
						personRecords ),
					new ArrayList<PersonRecord>( personRecords.values() ),
					new PlanAllocation(),
					Double.NEGATIVE_INFINITY);

			plans = allocation == null ? null : toGroupPlans( allocation );
		} while (
				plans != null &&
				continueIterations( forbiden , personRecords , plans ) );

		assert forbidBlockingCombinations || count == 1 : count;
		assert plans == null || !forbiden.isForbidden( plans );

		return plans;
	}

	// /////////////////////////////////////////////////////////////////////////
	// "translation" to and from the internal data structures
	// /////////////////////////////////////////////////////////////////////////
	private static GroupPlans toGroupPlans(final PlanAllocation allocation) {
		Set<JointPlan> jointPlans = new HashSet<JointPlan>();
		List<Plan> individualPlans = new ArrayList<Plan>();

		for ( PlanRecord p : allocation.getPlans() ) {
			if ( p.jointPlan != null ) {
				jointPlans.add( p.jointPlan );
			}
			else {
				individualPlans.add( p.plan );
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
							return -Double.compare( o1.avgJointPlanWeight , o2.avgJointPlanWeight );
						}
					});
		}

		return map;
	}

	// /////////////////////////////////////////////////////////////////////////
	// "outer loop": search and forbid if blocking (if forbid blocking is true)
	// /////////////////////////////////////////////////////////////////////////

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
	private PlanAllocation buildPlanString(
			final KnownStates knownStates,
			final ForbidenCombinations forbidenCombinations,
			final IncompatiblePlanRecords incompatibleRecords,
			final List<PersonRecord> personsStillToAllocate,
			// needed to check whether a leave is forbidden or not
			final PlanAllocation currentAllocation,
			final double minimalWeightToObtain) {
		assert !intersectsRecords( personsStillToAllocate , currentAllocation );
		if ( knownStates.isUnfeasible( personsStillToAllocate , minimalWeightToObtain ) ) {
			return null;
		}

		PlanAllocation constructedString = knownStates.getCachedSolutionForRemainingAgents( personsStillToAllocate );
		if ( constructedString != null ) {
			assert constructedString.getPlans().size() == personsStillToAllocate.size() :
				constructedString.getPlans().size()+" plans for "+personsStillToAllocate.size()+" agents";
			return constructedString;
		}

		final FeasibilityChanger feasibilityChanger = new FeasibilityChanger();
		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			new ArrayList<PersonRecord>( personsStillToAllocate );
		final PersonRecord currentPerson = remainingPersons.remove(0);
		assert remainingPersons.size() == personsStillToAllocate.size() - 1;

		// get a list of plans in decreasing order of maximum possible weight.
		// The weight is always computed on the full joint plan, and thus consists
		// of the weight until now plus the upper bound
		final List<PlanRecord> records = new ArrayList<PlanRecord>();
		for ( PlanRecord r : currentPerson.plans ) {
			assert !r.isStillFeasible == (r.jointPlan != null && intersects( r.jointPlan.getIndividualPlans().keySet() , currentAllocation ));
			if ( r.isStillFeasible ) records.add( r );
		}

		weightPlanRecords(
				incompatibleRecords,
				records,
				remainingPersons );

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
		assert constructedString == null;

		final FeasibilityChanger localFeasibilityChanger = new FeasibilityChanger();
		for (PlanRecord r : records) {
			if ( constructedString != null &&
					r.cachedMaximumWeight <= constructedString.getWeight()) {
				break;
			}

			if ( r.cachedMaximumWeight <= minimalWeightToObtain) {
				break;
			}

			assert r.isStillFeasible;

			final PlanAllocation newAllocation = new PlanAllocation();
			newAllocation.add( r );
			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			if (r.jointPlan != null) {
				assert !intersects( r.jointPlan.getIndividualPlans().keySet() , currentAllocation );
				assert r.linkedPlans.size() == r.jointPlan.getIndividualPlans().size() -1;

				newAllocation.addAll( r.linkedPlans );
				actuallyRemainingPersons = filter( remainingPersons , r.jointPlan );

				assert actuallyRemainingPersons.size() + r.jointPlan.getIndividualPlans().size() == personsStillToAllocate.size();
			}

			tagIncompatiblePlansAsInfeasible(
					r,
					incompatibleRecords,
					localFeasibilityChanger);

			PlanAllocation newString = null;
			/*scope*/ {
				final PlanAllocation newCurrentAlloc = new PlanAllocation();
				newCurrentAlloc.addAll( currentAllocation.getPlans() );
				newCurrentAlloc.addAll( newAllocation.getPlans() );
				if ( !actuallyRemainingPersons.isEmpty() ) {
					newString = buildPlanString(
							knownStates,
							forbidenCombinations,
							incompatibleRecords,
							actuallyRemainingPersons,
							newCurrentAlloc,
							Math.max(
								minimalWeightToObtain,
								constructedString != null ?
									constructedString.getWeight() :
									Double.NEGATIVE_INFINITY) - newAllocation.getWeight() );
					if ( newString != null ) {
						assert newString.getPlans().size() == actuallyRemainingPersons.size() : newString.getPlans().size()+" plans for "+actuallyRemainingPersons.size()+" agents";
						newString.addAll( newAllocation.getPlans() );
						assert newString.getPlans().size() == personsStillToAllocate.size() : newString.getPlans().size()+" plans for "+personsStillToAllocate.size()+" agents";
					}
				}
				else {
					newString = newAllocation;
					assert newString.getPlans().size() == personsStillToAllocate.size() : newString.getPlans().size()+" plans for "+personsStillToAllocate.size()+" agents";
					if ( forbidBlockingCombinations && forbidenCombinations.isForbidden( newCurrentAlloc ) ) {
						// we are on a leaf (ie a full plan).
						// If some combinations are forbidden, check if this one is.
						newString = null;
					}
				}
			}
			localFeasibilityChanger.resetFeasibilities();

			if ( newString == null || newString.getWeight() <= minimalWeightToObtain ) continue;

			assert newString.getWeight() <= r.cachedMaximumWeight :
				getClass()+" weight higher than estimated max: "+newString.getPlans()+" has weight "+newString.getWeight()+" > "+r.cachedMaximumWeight;
			assert newString.getWeight() > minimalWeightToObtain : newString.getWeight() +"<="+ minimalWeightToObtain;

			if (constructedString == null ||
					newString.getWeight() > constructedString.getWeight()) {
				constructedString = newString;
			}
		}

		feasibilityChanger.resetFeasibilities();

		assert constructedString == null || constructedString.getPlans().size() == personsStillToAllocate.size() :
			constructedString.getPlans().size()+" plans for "+personsStillToAllocate.size()+" agents";
		if ( !forbidBlockingCombinations || !forbidenCombinations.partialAllocationCanLeadToForbidden( currentAllocation ) ) {
			knownStates.cacheSolution( personsStillToAllocate , constructedString , minimalWeightToObtain );
		}
		return constructedString;
	}

	private static void tagIncompatiblePlansAsInfeasible(
			final PlanRecord r,
			final IncompatiblePlanRecords incompatibleRecords,
			final FeasibilityChanger localFeasibilityChanger) {
		for ( PlanRecord incompatible : incompatibleRecords.getIncompatiblePlans( r ) ) {
			localFeasibilityChanger.markInfeasible( incompatible );
		}
	}

	private static void weightPlanRecords(
			final IncompatiblePlanRecords incompatibleRecords,
			final Collection<PlanRecord> records,
			final List<PersonRecord> remainingPersons) {
		final FeasibilityChanger localFeasibilityChanger = new FeasibilityChanger();

		for (PlanRecord r : records) {
			if ( r.isStillFeasible ) {
				tagIncompatiblePlansAsInfeasible(
						r,
						incompatibleRecords,
						localFeasibilityChanger);

				r.cachedMaximumWeight =
						getMaxWeightFromPersons(
								r,
								remainingPersons );

				localFeasibilityChanger.resetFeasibilities();
			}
			else {
				r.cachedMaximumWeight = Double.NEGATIVE_INFINITY;
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
			final List<PersonRecord> remainingPersons) {
		double score = planToSelect.avgJointPlanWeight;

		for (PersonRecord record : remainingPersons) {
			final double max = getMaxWeight(
					planToSelect ,
					record );
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
			final PlanRecord planToSelect,
			final PersonRecord record) {
		// case in jp: plan is fully determined
		if (planToSelect.jointPlan != null) {
			final Plan plan = planToSelect.jointPlan.getIndividualPlan( record.person.getId() );
			if (plan != null) {
				// the "==" part allows to take into account infinite weights
				assert record.getRecord( plan ).avgJointPlanWeight == planToSelect.avgJointPlanWeight ||
					Math.abs( record.getRecord( plan ).avgJointPlanWeight - planToSelect.avgJointPlanWeight ) < EPSILON;
				return planToSelect.avgJointPlanWeight;
			}
		}

		for (PlanRecord plan : record.plans ) {
			// the plans are sorted by decreasing weight:
			// consider the first valid plan

			if ( plan.isStillFeasible ) return plan.avgJointPlanWeight;
			// no need to continue if we now the result can only be infinitely neg.
			if ( plan.avgJointPlanWeight == Double.NEGATIVE_INFINITY ) break;
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

	private static boolean intersects(
			final Collection<Id> ids1,
			final PlanAllocation alloc) {
		for ( PlanRecord p : alloc.getPlans() ) {
			if ( ids1.contains( p.person.person.getId() ) ) return true;
		}
		return false;
	}

	private static boolean intersectsRecords(
			final Collection<PersonRecord> records,
			final PlanAllocation alloc) {
		for ( PlanRecord p : alloc.getPlans() ) {
			if ( records.contains( p.person ) ) return true;
		}
		return false;
	}
}

