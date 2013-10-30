/* *********************************************************************** *
 * project: org.matsim.*
 * CoalitionSelector.java
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
package playground.thibautd.socnetsim.replanning.selectors.coalitionselector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.ScoreWeight;
import playground.thibautd.socnetsim.replanning.selectors.WeightCalculator;
import playground.thibautd.utils.MapUtils;

/**
 * Selects a combination of plans without "blocking coalition",
 * that is, there is no group of agents which can improve their utility by trading
 * together.
 * @author thibautd
 */
public class CoalitionSelector implements GroupLevelPlanSelector {
	private final WeightCalculator weight;

	public CoalitionSelector() {
		this( new ScoreWeight() );
	}

	public CoalitionSelector(final WeightCalculator weight) {
		this.weight = weight;
	}

	@Override
	public GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		try {
			final Map<Id, PointingAgent> agents = new LinkedHashMap<Id, PointingAgent>();
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan = new HashMap<JointPlan, Collection<PlanRecord>>();

			// create agents
			for ( Person person : group.getPersons() ) {
				final PointingAgent agent =
					new PointingAgent(
							person,
							group,
							weight);
				agents.put( person.getId() , agent );

				for ( PlanRecord r : agent.getRecords() ) {
					final JointPlan jp = jointPlans.getJointPlan( r.getPlan() );
					if ( jp != null ) {
						MapUtils.getCollection(
								jp,
								recordsPerJointPlan ).add( r );
					}
				}
			}

			// iterate
			final GroupPlans groupPlans = new GroupPlans();
			while ( !agents.isEmpty() ) {
				doIteration(
						jointPlans,
						agents,
						recordsPerJointPlan,
						groupPlans );
			}

			return groupPlans;
		}
		catch ( Exception e ) {
			throw new RuntimeException( "exception with group "+group , e );
		}
	}

	private static void doIteration(
			final JointPlans jointPlans,
			final Map<Id, PointingAgent> agents,
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan,
			final GroupPlans groupPlans) {
		// Agents all point their prefered feasible plan, feasible meaning that
		// no co-traveler was already alocated a plan.
		// If all participants of a joint plan point it, it is selected, as they
		// represent a blocking coalition for other allocations of the remaining
		// plans.
		for ( Map.Entry<Id, PointingAgent> entry : agents.entrySet() ) {
			final PointingAgent agent = entry.getValue();

			final Plan pointedPlan = agent.getPointedPlan();
			final JointPlan pointedJointPlan = jointPlans.getJointPlan( pointedPlan );

			if ( pointedJointPlan == null ) {
				groupPlans.addIndividualPlan( pointedPlan );

				final Id id = entry.getKey();
				markJointPlansAsInfeasible( id , agents , recordsPerJointPlan );
				return;
			}
			else if ( areAllPlansPointed( recordsPerJointPlan.get( pointedJointPlan ) ) ) {

				groupPlans.addJointPlan( pointedJointPlan );

				for ( Id id : pointedJointPlan.getIndividualPlans().keySet() ) {
					markJointPlansAsInfeasible( id , agents , recordsPerJointPlan );
				}
				return;
			}
		}

		markLeastPointedJointPlanAsInfeasible(
				recordsPerJointPlan );
	}

	private static void markJointPlansAsInfeasible(
			final Id id,
			final Map<Id, PointingAgent> agents,
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan) {
		final Iterator<Map.Entry<JointPlan, Collection<PlanRecord>>> it =
			recordsPerJointPlan.entrySet().iterator();
		while ( it.hasNext() ) {
			final Map.Entry<JointPlan, Collection<PlanRecord>> jpEntry = it.next();
			if ( !jpEntry.getKey().getIndividualPlans().containsKey( id ) ) continue;

			for ( PlanRecord record : jpEntry.getValue() ) {
				record.setInfeasible();
			}
			it.remove();
		}

		agents.remove( id );
	}

	private static void markLeastPointedJointPlanAsInfeasible(
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan) {
		int minPoint = Integer.MAX_VALUE;
		Map.Entry<JointPlan , Collection<PlanRecord>> toMark = null;
		for ( Map.Entry<JointPlan , Collection<PlanRecord>> entry : recordsPerJointPlan.entrySet() ) {
			final JointPlan jointPlan = entry.getKey();
			final Collection<PlanRecord> records = entry.getValue();

			int nPoint = 0;
			for ( PlanRecord r : records ) {
				if ( jointPlan.getIndividualPlan( r.getPlan().getPerson().getId() ) == r.getPlan() ) nPoint++;
			}

			if ( nPoint > 1 && nPoint < minPoint ) {
				minPoint = nPoint;
				toMark = entry;
			}
		}

		if ( toMark == null ) {
			throw new RuntimeException( "could not find least pointed joint plan in "+recordsPerJointPlan );
		}

		for ( PlanRecord r : toMark.getValue() ) r.setInfeasible();
		recordsPerJointPlan.remove( toMark.getKey() );
	}

	private static boolean areAllPlansPointed(
			final Collection<PlanRecord> records) {
		for ( PlanRecord record : records ) {
			assert record.isFeasible();
			if ( record.getAgent().getPointedPlan() != record.getPlan() ) {
				return false;
			}
		}
		return true;
	}
}

