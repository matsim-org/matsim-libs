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

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.WeightCalculator;

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
	private final IncompatiblePlansIdentifierFactory incompFactory;

	public HighestWeightSelector(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final WeightCalculator weightCalculator) {
		this( false , incompFactory , weightCalculator );
	}

	public HighestWeightSelector(
			final boolean isForRemoval,
			final IncompatiblePlansIdentifierFactory incompFactory,
			final WeightCalculator weightCalculator) {
		this.forbidBlockingCombinations = isForRemoval;
		this.weightCalculator = weightCalculator;
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

		PersonRecordsPlansPruner.prunePlans(
				incompatibleRecords,
				personRecords );

		final PlanAllocation allocation = buildPlanString(
				new KnownStates(),
				new KnownFeasibleAllocations( 20 ),
				incompatibleRecords,
				SelectorUtils.toSortedList( incompatibleRecords , personRecords ),
				personRecords,
				forbidBlockingCombinations ? new PlanAllocation() : null,
				incompatibleRecords.getAllIncompatibilityGroupIds(),
				Double.NEGATIVE_INFINITY).allocation;

		assert allocation == null || allocation.getPlans().size() == group.getPersons().size() :
				allocation.getPlans().size()+" != "+group.getPersons().size();

		final GroupPlans plans = allocation == null ? null : SelectorUtils.toGroupPlans( allocation );

		return plans;
	}

	// /////////////////////////////////////////////////////////////////////////
	// "translation" to and from the internal data structures
	// /////////////////////////////////////////////////////////////////////////

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
		}

		return map;
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
	private AllocationRecord buildPlanString(
			final KnownStates knownStates,
			final KnownFeasibleAllocations knownFeasibleAllocations,
			final IncompatiblePlanRecords incompatibleRecords,
			final List<PersonRecord> personsStillToAllocate,
			final Map<Id, PersonRecord> allPersons,
			// needed to check whether a leave is forbidden or not
			final PlanAllocation currentAllocation,
			final Set<Id> allowedIncompatibilityGroups,
			final double minimalWeightToObtain) {
		if ( knownStates != null &&
				knownStates.isUnfeasible(
					personsStillToAllocate,
					allowedIncompatibilityGroups,
					minimalWeightToObtain) ) {
			assert null ==
					buildPlanString(
						null,
						new KnownFeasibleAllocations( 0 ),
						incompatibleRecords,
						personsStillToAllocate,
						allPersons,
						currentAllocation,
						allowedIncompatibilityGroups,
						minimalWeightToObtain).allocation;
			return new AllocationRecord( null , false );
		}

		/*scope of cachedAlloc*/ {
			final PlanAllocation cachedAlloc = knownStates == null ? null :
				knownStates.getCached(
					personsStillToAllocate,
					allowedIncompatibilityGroups );

			if ( cachedAlloc != null ) {
				if ( !forbidBlockingCombinations ||
						!SelectorUtils.isBlocking(
							knownFeasibleAllocations,
							incompatibleRecords,
							allPersons,
							SelectorUtils.toGroupPlans(
								SelectorUtils.merge(
									currentAllocation,
									cachedAlloc ) ) ) ) {
					//assert cachedAlloc.equals(
					//		buildPlanString(
					//			null,
					//			new KnownFeasibleAllocations( 0 ),
					//			incompatibleRecords,
					//			personsStillToAllocate,
					//			allPersons,
					//			currentAllocation,
					//			allowedIncompatibilityGroups,
					//			Double.NEGATIVE_INFINITY).allocation ):
					//	"cachedWeight="+cachedAlloc.getWeight()+
					//	 " searchWeight="+
					//		buildPlanString(
					//			null,
					//			new KnownFeasibleAllocations( 0 ),
					//			incompatibleRecords,
					//			personsStillToAllocate,
					//			allPersons,
					//			currentAllocation,
					//			allowedIncompatibilityGroups,
					//			Double.NEGATIVE_INFINITY).allocation.getWeight();
					return new AllocationRecord( cachedAlloc , false );
				}
			}
		}

		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			new ArrayList<PersonRecord>( personsStillToAllocate );
		final PersonRecord currentPerson = remainingPersons.remove(0);
		assert remainingPersons.size() == personsStillToAllocate.size() - 1;

		// get a list of plans in decreasing order of maximum possible weight.
		// The weight is always computed on the full joint plan, and thus consists
		// of the weight until now plus the upper bound
		final List<PlanRecord> records = new ArrayList<PlanRecord>();
		for ( PlanRecord r : currentPerson.prunedPlans ) {
			if ( r.isStillFeasible ) records.add( r );
		}

		if ( records.isEmpty() ) return new AllocationRecord( null , false );

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
						return Double.compare(
							o2.cachedMaximumWeight,
							o1.cachedMaximumWeight );
					}
				});

		// get the actual allocation, and stop when the allocation
		// is better than the maximum possible in remaining plans
		// or worst than the worst possible at a higher level
		PlanAllocation constructedString = null;
		boolean resultsFromBlocking = false;
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
				assert currentAllocation == null ||
						!SelectorUtils.intersects( r.jointPlan.getIndividualPlans().keySet() , currentAllocation );
				assert r.linkedPlans.size() == r.jointPlan.getIndividualPlans().size() -1;

				newAllocation.addAll( r.linkedPlans );
				actuallyRemainingPersons = SelectorUtils.filter( remainingPersons , r.jointPlan );

				assert actuallyRemainingPersons.size() + r.jointPlan.getIndividualPlans().size() == personsStillToAllocate.size();
			}

			PlanAllocation newString = null;

			if ( !actuallyRemainingPersons.isEmpty() ) {
				final FeasibilityChanger localFeasibilityChanger = new FeasibilityChanger();
				SelectorUtils.tagIncompatiblePlansAsInfeasible(
						r,
						incompatibleRecords,
						localFeasibilityChanger);

				if ( currentAllocation != null ) currentAllocation.addAll( newAllocation.getPlans() );
				final AllocationRecord record = buildPlanString(
						knownStates,
						knownFeasibleAllocations,
						incompatibleRecords,
						actuallyRemainingPersons,
						allPersons,
						currentAllocation,
						newIncompatibilityGroups(
							incompatibleRecords,
							allowedIncompatibilityGroups,
							r ),
						actualMinToObtain(
							constructedString,
							minimalWeightToObtain)
							- newAllocation.getWeight() );
				if ( currentAllocation != null ) currentAllocation.removeAll( newAllocation.getPlans() );
				localFeasibilityChanger.resetFeasibilities();

				newString = record.allocation;

				if ( record.resultsFromBlocking ) {
					resultsFromBlocking = true;

					assert forbidBlockingCombinations;
					if ( record.allocation == null &&
							constructedString == null &&
							knownFeasibleAllocations.isKownAsBlocking(
								SelectorUtils.toGroupPlans(
										currentAllocation ) ) ) {
						// early cuttoff if this branch is always blocking.
						return new AllocationRecord( null , true );
					}
				}

				if ( newString != null ) {
					assert newString.getPlans().size() == actuallyRemainingPersons.size() : newString.getPlans().size()+" plans for "+actuallyRemainingPersons.size()+" agents";
					newString.addAll( newAllocation.getPlans() );
					assert newString.getPlans().size() == personsStillToAllocate.size() : newString.getPlans().size()+" plans for "+personsStillToAllocate.size()+" agents";
				}
			}
			else {
				if ( newAllocation.getWeight() <= actualMinToObtain( constructedString , minimalWeightToObtain ) ) continue;
				assert !forbidBlockingCombinations || currentAllocation != null;

				if (forbidBlockingCombinations &&
					SelectorUtils.isBlocking(
							knownFeasibleAllocations,
							incompatibleRecords,
							allPersons,
							SelectorUtils.toGroupPlans(
								SelectorUtils.merge(
									currentAllocation,
									newAllocation ) ) ) ) {
					resultsFromBlocking = true;

					if ( constructedString == null &&
							knownFeasibleAllocations.isKownAsBlocking(
								SelectorUtils.toGroupPlans(
										currentAllocation ) ) ) {
						// early cuttoff if this branch is always blocking
						return new AllocationRecord( null , true );
					}
				}
				else {
					newString = newAllocation;
				}
			}

			if ( newString == null ||
					newString.getWeight() <= actualMinToObtain( constructedString , minimalWeightToObtain ) ) continue;

			assert newString.getWeight() <= r.cachedMaximumWeight :
				getClass()+" weight higher than estimated max: "+newString.getPlans()+" has weight "+newString.getWeight()+" > "+r.cachedMaximumWeight;
			assert newString.getWeight() > minimalWeightToObtain :
				newString.getWeight() +"<="+ minimalWeightToObtain;
			assert constructedString == null || newString.getWeight() > constructedString.getWeight() :
				newString.getWeight() +"<="+ minimalWeightToObtain;

			constructedString = newString;
		}

		assert constructedString == null || constructedString.getPlans().size() == personsStillToAllocate.size() :
			constructedString.getPlans().size()+" plans for "+personsStillToAllocate.size()+" agents";

		if ( knownStates != null && !resultsFromBlocking ) {
			knownStates.cache(
					personsStillToAllocate,
					allowedIncompatibilityGroups,
					constructedString ,
					minimalWeightToObtain);
		}

		return new AllocationRecord( constructedString , resultsFromBlocking );
	}

	private static double actualMinToObtain(
			final PlanAllocation constructedString,
			final double minimalWeightToObtain) {
		assert constructedString == null || constructedString.getWeight() > minimalWeightToObtain;
		return Math.max(
			minimalWeightToObtain,
			constructedString != null ?
				constructedString.getWeight() :
				Double.NEGATIVE_INFINITY);
	}

	private static Set<Id> newIncompatibilityGroups(
			final IncompatiblePlanRecords incompatibleRecords,
			final Set<Id> allowedIncompatibilityGroups,
			final PlanRecord r ) {
		final Set<Id> forbid = incompatibleRecords.getIncompatibilityGroups( r );

		if ( forbid.isEmpty() ) return allowedIncompatibilityGroups;

		final Set<Id> newSet = new HashSet<Id>( allowedIncompatibilityGroups );
		newSet.removeAll( forbid );

		return newSet;
	}

	private static void weightPlanRecords(
			final IncompatiblePlanRecords incompatibleRecords,
			final Collection<PlanRecord> records,
			final List<PersonRecord> remainingPersons) {
		final FeasibilityChanger localFeasibilityChanger = new FeasibilityChanger();

		for (PlanRecord r : records) {
			if ( r.isStillFeasible ) {
				SelectorUtils.tagIncompatiblePlansAsInfeasible(
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

		for (PlanRecord plan : record.prunedPlans ) {
			// the plans are sorted by decreasing weight:
			// consider the first valid plan

			if ( plan.isStillFeasible ) return plan.avgJointPlanWeight;
			// no need to continue if we now the result can only be infinitely neg.
			if ( plan.avgJointPlanWeight == Double.NEGATIVE_INFINITY ) break;
		}

		// this combination is impossible
		return Double.NEGATIVE_INFINITY;
	}
}

class AllocationRecord {
	public final PlanAllocation allocation;
	public final boolean resultsFromBlocking;

	public AllocationRecord(
			final PlanAllocation allocation,
			final boolean resultsFromBlocking) {
		this.allocation = allocation;
		this.resultsFromBlocking = resultsFromBlocking;
	}
}
