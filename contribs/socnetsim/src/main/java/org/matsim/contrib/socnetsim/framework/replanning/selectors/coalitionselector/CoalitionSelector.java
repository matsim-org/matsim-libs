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
package org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import org.matsim.core.utils.collections.MapUtils;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.ScoreWeight;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.WeightCalculator;
import org.matsim.core.utils.misc.Counter;

/**
 * Selects a combination of plans without "blocking coalition",
 * that is, there is no group of agents which can improve their utility by trading
 * together.
 * @author thibautd
 */
public class CoalitionSelector implements GroupLevelPlanSelector {
	private static final Logger log = Logger.getLogger( CoalitionSelector.class );
	
	private final ConflictSolver conflictSolver;
	private final WeightCalculator weight;

	public CoalitionSelector() {
		this( new ScoreWeight() );
	}

	public CoalitionSelector(final WeightCalculator weight) {
		this( weight ,  new LeastPointedPlanPruningConflictSolver() );
	}

	public CoalitionSelector(
			final WeightCalculator weight,
			final ConflictSolver conflictSolver) {
		this.weight = weight;
		this.conflictSolver = conflictSolver;
	}


	@Override
	public GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		try {
			final Map<Id, PointingAgent> agents = new LinkedHashMap<Id, PointingAgent>();
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan = new HashMap<JointPlan, Collection<PlanRecord>>();

			// create agents
			log.trace( "initialize pointing agents" );
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
			log.trace( "start iterations" );
			final GroupPlans groupPlans = new GroupPlans();
			final Counter counter = new Counter( "do iteration # " );
			while ( !agents.isEmpty() ) {
				if ( log.isTraceEnabled() ) counter.incCounter();
				doIteration(
						jointPlans,
						agents,
						recordsPerJointPlan,
						groupPlans );
			}
			if ( log.isTraceEnabled() )
				log.trace( "did "+counter.getCounter()+
						" iterations ("+(((double) counter.getCounter()) / group.getPersons().size())+" per person)" );

			return groupPlans;
		}
		catch ( Exception e ) {
			throw new RuntimeException( "exception with group "+group , e );
		}
	}

	private void doIteration(
			final JointPlans jointPlans,
			final Map<Id, PointingAgent> agents,
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan,
			final GroupPlans groupPlans) {
		// Agents all point their preferred feasible plan, feasible meaning that
		// no co-traveler was already allocated a plan.
		// If all participants of a joint plan point it, it is selected, as they
		// represent a blocking coalition for other allocations of the remaining
		// plans.
		for ( Map.Entry<Id, PointingAgent> entry : agents.entrySet() ) {
			final PointingAgent agent = entry.getValue();

			final Plan pointedPlan = agent.getPointedPlan();
			final JointPlan pointedJointPlan = jointPlans.getJointPlan( pointedPlan );

			if ( pointedJointPlan == null ) {
				// agent points its individual plan
				groupPlans.addIndividualPlan( pointedPlan );

				final Id id = entry.getKey();
				markJointPlansAsInfeasible( id , agents , recordsPerJointPlan , jointPlans );
				return;
			}
			else if ( areAllPlansPointed( recordsPerJointPlan.get( pointedJointPlan ) ) ) {
				// found a dominating joint plan
				groupPlans.addJointPlan( pointedJointPlan );

				for ( Id id : pointedJointPlan.getIndividualPlans().keySet() ) {
					markJointPlansAsInfeasible( id , agents , recordsPerJointPlan , jointPlans );
				}
				return;
			}
		}

		conflictSolver.attemptToSolveConflicts(
				recordsPerJointPlan );
	}

	private static void markJointPlansAsInfeasible(
			final Id id,
			final Map<Id, PointingAgent> agents,
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan,
			final JointPlans jointPlans ) {
		final Iterator<Map.Entry<JointPlan, Collection<PlanRecord>>> it =
			recordsPerJointPlan.entrySet().iterator();

		final PointingAgent agent = agents.get( id );

		for ( PlanRecord plan : agent.getRecords() ) {
			final JointPlan jointPlan = jointPlans.getJointPlan( plan.getPlan() );
			if ( jointPlan == null ) continue;

			// could already be handled
			if ( !recordsPerJointPlan.containsKey( jointPlan ) ) continue;
			for ( PlanRecord record : recordsPerJointPlan.remove( jointPlan ) ) {
				record.setInfeasible();
			}
		}

		agents.remove( id );
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

	public interface ConflictSolver {
		void attemptToSolveConflicts( Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan );
	}
}

