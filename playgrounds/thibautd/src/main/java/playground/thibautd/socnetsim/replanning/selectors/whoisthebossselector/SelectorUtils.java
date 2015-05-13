/* *********************************************************************** *
 * project: org.matsim.*
 * HighestWeightSelectorUtils.java
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
package playground.thibautd.socnetsim.replanning.selectors.whoisthebossselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
final class SelectorUtils {
	private SelectorUtils() {}

	public static boolean isBlocking(
			final IncompatiblePlanRecords incompatibleRecords,
			final Map<Id, PersonRecord> personRecords,
			final GroupPlans groupPlan) {
		final FeasibilityChanger everybodyChanger = new FeasibilityChanger( true );
		final FeasibilityChanger infeasibility = new FeasibilityChanger();

		for ( PersonRecord person : personRecords.values() ) {
			for ( PlanRecord plan : person.plans ) everybodyChanger.changeIfNecessary( plan );
		}

		for ( Plan p : groupPlan.getAllIndividualPlans() ) {
			final PersonRecord person = personRecords.get( p.getPerson().getId() );
			final PlanRecord plan = person.getRecord( p );
			assert plan.isStillFeasible;
			infeasibility.changeIfNecessary( plan );
		}

		final GroupPlans relevantForbiddenPlans = new GroupPlans();
		final GroupPlans nonBlockingPlan = new GroupPlans();
		final boolean isBlocking = !searchForCombinationsWithoutForbiddenPlans(
				incompatibleRecords,
				relevantForbiddenPlans,
				nonBlockingPlan,
				toSortedList( incompatibleRecords , personRecords ));
		infeasibility.resetFeasibilities();
		everybodyChanger.resetFeasibilities();

		if ( isBlocking ) {
			// only consider the plans of the group plan
			final List<Plan> ps = new ArrayList<Plan>( groupPlan.getIndividualPlans() );
			final List<JointPlan> jps = new ArrayList<JointPlan>( groupPlan.getJointPlans() );
			ps.retainAll( relevantForbiddenPlans.getIndividualPlans() );
			jps.retainAll( relevantForbiddenPlans.getJointPlans() );
		}

		return isBlocking;
	}

	private static boolean searchForCombinationsWithoutForbiddenPlans(
			final IncompatiblePlanRecords incompatibleRecords,
			final GroupPlans relevantForbiddenPlans,
			final GroupPlans constructedPlan,
			final List<PersonRecord> personsStillToAllocate) {
		//if ( !remainsFeasible( personsStillToAllocate ) ) return false;
		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			new ArrayList<PersonRecord>( personsStillToAllocate );
		final PersonRecord currentPerson = remainingPersons.remove(0);

		for ( PersonRecord pr : remainingPersons ) {
			if ( !remainsPossible( pr.plans ) ) {
				for ( PlanRecord plan : pr.plans ) {
					add( relevantForbiddenPlans , plan );
				}
				return false;
			}
		}

		final Set<Branch> exploredBranches = new HashSet<Branch>();
		// examine plans from worst to best. This increases a lot the chances
		// that the non-blocked plan found for a given leave is also non-blocked
		// for the next leave
		int i = currentPerson.plans.size() - 1;
		for (PlanRecord r = currentPerson.plans.get( i );
				r != null;
				r = i-- > 0 ? currentPerson.plans.get( i ) : null ) {
			// skip forbidden plans
			if ( !r.isStillFeasible ) {
				// remember that this plan was used for determining if blocking
				// or not
				add( relevantForbiddenPlans , r );
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
				assert containsAllIds( personsStillToAllocate , r.jointPlan.getIndividualPlans().keySet() ); 
				actuallyRemainingPersons = filter( remainingPersons , r.jointPlan );
			}

			boolean found = true;
			if ( !actuallyRemainingPersons.isEmpty() ) {
				final FeasibilityChanger feasibilityChanger = new FeasibilityChanger();
				tagIncompatiblePlansAsInfeasible(
						r,
						incompatibleRecords,
						feasibilityChanger);

				found = searchForCombinationsWithoutForbiddenPlans(
						incompatibleRecords,
						relevantForbiddenPlans,
						constructedPlan,
						actuallyRemainingPersons);

				feasibilityChanger.resetFeasibilities();
			}

			if (found) {
				if ( constructedPlan != null ) add( constructedPlan , r );
				return true;
			}
		}

		return false;
	}

	private static boolean remainsPossible(
			final List<PlanRecord> plans) {
		for ( PlanRecord p : plans ) {
			if ( p.isStillFeasible ) return true;
		}
		return false;
	}

	private static void add(
			final GroupPlans constructedPlan,
			final PlanRecord r) {
		if ( r.jointPlan == null ) {
			constructedPlan.addIndividualPlan( r.plan );
		}
		else {
			constructedPlan.addJointPlan( r.jointPlan );
		}
	}

	public static void tagIncompatiblePlansAsInfeasible(
			final PlanRecord r,
			final IncompatiblePlanRecords incompatibleRecords,
			final FeasibilityChanger localFeasibilityChanger) {
		for ( PlanRecord incompatible : incompatibleRecords.getIncompatiblePlans( r ) ) {
			localFeasibilityChanger.changeIfNecessary( incompatible );
		}
	}

	public static List<PersonRecord> filter(
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

	public static boolean intersects(
			final Collection<Id<Person>> ids1,
			final PlanAllocation alloc) {
		for ( PlanRecord p : alloc.getPlans() ) {
			if ( ids1.contains( p.person.person.getId() ) ) return true;
		}
		return false;
	}

	public static boolean containsAllIds(
			final List<PersonRecord> persons,
			final Set<Id<Person>> ids) {
		final Collection<Id<Person>> remainingIds = new HashSet< >( ids );

		for ( PersonRecord p : persons ) {
			remainingIds.remove( p.person.getId() );
			if ( remainingIds.isEmpty() ) return true;
		}

		return false;
	}

	public static PlanAllocation merge(
			final PlanAllocation a1,
			final PlanAllocation a2) {
		final PlanAllocation merged = new PlanAllocation();
		if ( a1 != null ) merged.addAll( a1.getPlans() );
		if ( a2 != null ) merged.addAll( a2.getPlans() );
		return merged;
	}

	public static GroupPlans toGroupPlans(final PlanAllocation allocation) {
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

	public static List<PersonRecord> toSortedList(
			final IncompatiblePlanRecords incompatibleRecords,
			final Map<Id, PersonRecord> personRecords) {
		final List<PersonRecord> list = new ArrayList<PersonRecord>( personRecords.values() );

		Collections.sort(
				list,
				new Comparator<PersonRecord>() {
					@Override
					public int compare(
							final PersonRecord o1,
							final PersonRecord o2) {
						final int nIncomp1 = countIncomp( o1 );
						final int nIncomp2 = countIncomp( o2 );
						// sort by decreasing number of incompatible plans.
						// The idea is that all the combinatorial mess
						// happens close to the root, to avoid having to go deep
						// before realizing we're stuck
						return nIncomp2 - nIncomp1;
					}

					private int countIncomp(final PersonRecord person) {
						int c = 0;
						for ( PlanRecord p : person.prunedPlans ) {
							c += incompatibleRecords.getIncompatiblePlans( p ).size();
						}
						return c;
					}
				});
		return list;
	}


}

