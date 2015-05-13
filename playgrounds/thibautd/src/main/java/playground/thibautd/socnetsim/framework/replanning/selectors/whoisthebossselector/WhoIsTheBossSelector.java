/* *********************************************************************** *
 * project: org.matsim.*
 * WhoIsTheBossSelector.java
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
package playground.thibautd.socnetsim.framework.replanning.selectors.whoisthebossselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.ivt.utils.MapUtils;
import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifier;
import playground.thibautd.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.framework.replanning.selectors.WeightCalculator;

/**
 * Attempt at a selector without joint utility.
 * Agents are parsed in random order.
 * At his turn, each agent selects his best plan.
 * Linked plans are automatically selected ( hence the "boss" ).
 * @author thibautd
 */
public class WhoIsTheBossSelector implements GroupLevelPlanSelector {
	private static final Logger log =
		Logger.getLogger(WhoIsTheBossSelector.class);

	private final Random random;
	private final boolean forbidBlockingCombinations;
	private final WeightCalculator weightCalculator;
	private final IncompatiblePlansIdentifierFactory incompFactory;

	public WhoIsTheBossSelector(
			final Random random,
			final IncompatiblePlansIdentifierFactory incompFactory,
			final WeightCalculator weightCalculator) {
		this( false , random , incompFactory , weightCalculator );
	}

	public WhoIsTheBossSelector(
			final boolean isForRemoval,
			final Random random,
			final IncompatiblePlansIdentifierFactory incompFactory,
			final WeightCalculator weightCalculator) {
		this.random = random;
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
		if ( log.isTraceEnabled() ) {
			log.trace( "selecting plan for group "+group );
		}

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
				incompatibleRecords,
				putInRandomOrder( personRecords ),
				personRecords,
				new PlanAllocation(),
				incompatibleRecords.getAllIncompatibilityGroupIds());

		assert allocation == null || allocation.getPlans().size() == group.getPersons().size() :
				allocation.getPlans().size()+" != "+group.getPersons().size();

		final GroupPlans plans = allocation == null ? null : SelectorUtils.toGroupPlans( allocation );

		if ( log.isTraceEnabled() ) {
			log.trace( "found plans "+plans );
		}

		return plans;
	}

	// /////////////////////////////////////////////////////////////////////////
	// "translation" to and from the internal data structures
	// /////////////////////////////////////////////////////////////////////////

	private List<PersonRecord> putInRandomOrder(
			final Map<Id, PersonRecord> personRecords) {
		final List<PersonRecord> records = new ArrayList<PersonRecord>( personRecords.values() );
		// make sure order deterministic
		Collections.sort(
				records,
				new Comparator<PersonRecord>() {
					@Override
					public int compare(
						final PersonRecord o1,
						final PersonRecord o2) {
						return o1.person.getId().compareTo( o2.person.getId() );
					}
				}); 
		Collections.shuffle( records , random );
		return records;
	}

	private Map<Id, PersonRecord> getPersonRecords(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Map<Id, PersonRecord> map = new LinkedHashMap<Id, PersonRecord>();
		final Map<Plan, Double> weights = new HashMap<Plan, Double>();

		final Map<JointPlan, Collection<PlanRecord>> recordsPerJp = new HashMap<JointPlan, Collection<PlanRecord>>();
		for (Person person : group.getPersons()) {
			final List<PlanRecord> plans = new ArrayList<PlanRecord>();
			for (Plan plan : person.getPlans()) {
				final double w = weightCalculator.getWeight( plan , group );
				if ( Double.isNaN( w ) ) throw new IllegalArgumentException( "NaN weights are not allowed" );
				weights.put( plan , w );

				final JointPlan jp = jointPlans.getJointPlan( plan );
				final PlanRecord r = new PlanRecord(
							plan,
							jp,
							w);
				plans.add( r );

				if ( jp != null ) {
					MapUtils.getCollection(
							jp,
							recordsPerJp ).add( r );
				}
			}
			final PersonRecord pr =
					new PersonRecord(
						person,
						plans );
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
	private PlanAllocation buildPlanString(
			final IncompatiblePlanRecords incompatibleRecords,
			final List<PersonRecord> personsStillToAllocate,
			final Map<Id, PersonRecord> allPersons,
			// needed to check whether a leave is forbidden or not
			final PlanAllocation currentAllocation,
			final Set<Id> allowedIncompatibilityGroups) {
		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			new ArrayList<PersonRecord>( personsStillToAllocate );
		final PersonRecord currentPerson = remainingPersons.remove(0);
		assert remainingPersons.size() == personsStillToAllocate.size() - 1;

		if ( log.isTraceEnabled() ) {
			log.trace(
					"look at person "+currentPerson.person.getId()+
					" at level "+
					(currentAllocation == null ? 0 : currentAllocation.getPlans().size() ) );
		}

		// get a list of plans in decreasing order of maximum possible weight.
		// The weight is always computed on the full joint plan, and thus consists
		// of the weight until now plus the upper bound
		final List<PlanRecord> records = new ArrayList<PlanRecord>();
		for ( PlanRecord r : currentPerson.prunedPlans ) {
			if ( r.isStillFeasible ) records.add( r );
		}

		if ( records.isEmpty() ) return null;

		for (PlanRecord r : records) {
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

			if ( !actuallyRemainingPersons.isEmpty() ) {
				final FeasibilityChanger localFeasibilityChanger = new FeasibilityChanger();
				SelectorUtils.tagIncompatiblePlansAsInfeasible(
						r,
						incompatibleRecords,
						localFeasibilityChanger);

				if ( currentAllocation != null ) currentAllocation.addAll( newAllocation.getPlans() );
				final PlanAllocation record = buildPlanString(
						incompatibleRecords,
						actuallyRemainingPersons,
						allPersons,
						currentAllocation,
						newIncompatibilityGroups(
							incompatibleRecords,
							allowedIncompatibilityGroups,
							r ));
				if ( currentAllocation != null ) currentAllocation.removeAll( newAllocation.getPlans() );
				localFeasibilityChanger.resetFeasibilities();

				if ( record != null ) {
					// something was found: just return it
					return SelectorUtils.merge( newAllocation , record );
				}
			}
			else {
				assert !forbidBlockingCombinations || currentAllocation != null;

				if (!forbidBlockingCombinations ||
					!SelectorUtils.isBlocking(
							incompatibleRecords,
							allPersons,
							SelectorUtils.toGroupPlans(
								SelectorUtils.merge(
									currentAllocation,
									newAllocation ) ) ) ) {
					// this is a valid plan and everybody has a plan. we're done.
					return newAllocation;
				}
			}
		}

		return null;
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
}
