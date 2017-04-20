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
import java.util.Collections;
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
			final RecordsOfJointPlan recordsPerJointPlan = new RecordsOfJointPlan( jointPlans );

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
					recordsPerJointPlan.addRecord( r );
				}
			}

			// iterate
			log.trace( "start iterations" );
			final GroupPlans groupPlans = new GroupPlans();
			final Counter counter = new Counter( "do iteration # " );
			int nextSize = 1;
			final int maxSize = agents.size();
			while ( !agents.isEmpty() ) {
				if ( log.isTraceEnabled() ) {
					counter.incCounter();
					final int nAllocated = groupPlans.getAllIndividualPlans().size();
					if ( nAllocated > nextSize ) {
						log.trace( nAllocated+" / "+maxSize+" allocated plans" );
						while( nAllocated > nextSize ) nextSize *= 2;
					}
				}
				try {
					doIteration(
							jointPlans,
							agents,
							recordsPerJointPlan,
							groupPlans);
				}
				catch ( Throwable e ) {
					log.error( "got error searching for plans for group "+group );
					log.error( "plan allocation at time of error: "+groupPlans );
					throw e;
				}
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
			final RecordsOfJointPlan recordsPerJointPlan,
			final GroupPlans groupPlans) {
		// Agents all point their preferred feasible plan, feasible meaning that
		// no co-traveler was already allocated a plan.
		// If all participants of a joint plan point it, it is selected, as they
		// represent a blocking coalition for other allocations of the remaining
		// plans.
		boolean didSomething = false;
		for( Iterator<PointingAgent> agentIterator = agents.values().iterator();
			 agentIterator.hasNext(); ) {
			final PointingAgent agent = agentIterator.next();
			if ( agent.isOff() ) {
				agentIterator.remove();
				continue;
			}

			final Plan pointedPlan = agent.getPointedPlan();
			final JointPlan pointedJointPlan = jointPlans.getJointPlan( pointedPlan );

			if ( pointedJointPlan == null ) {
				// agent points its individual plan
				groupPlans.addIndividualPlan( pointedPlan );

				final Id id = agent.getPointedPlan().getPerson().getId();
				markJointPlansAsInfeasible( id , agents , recordsPerJointPlan , jointPlans );
				agentIterator.remove();
				didSomething = true;
			}
			else if ( areAllPlansPointed( recordsPerJointPlan.getRecords( pointedJointPlan ) ) ) {
				// found a dominating joint plan
				groupPlans.addJointPlan( pointedJointPlan );

				for ( Id id : pointedJointPlan.getIndividualPlans().keySet() ) {
					markJointPlansAsInfeasible( id , agents , recordsPerJointPlan , jointPlans );
				}
				agentIterator.remove();
				didSomething = true;
			}
		}

		// need to check that agents are not empty, as there might be a last iteration just to remove "switched off" agents,
		// that might cause problems with some conflict solvers (as there is no conflict to solve)
		if ( !didSomething && !agents.isEmpty() ) {
			conflictSolver.attemptToSolveConflicts(
					recordsPerJointPlan );
		}
	}

	private static void markJointPlansAsInfeasible(
			final Id id,
			final Map<Id, PointingAgent> agents,
			final RecordsOfJointPlan recordsPerJointPlan,
			final JointPlans jointPlans ) {
		final PointingAgent agent = agents.get( id );
		agent.switchOff();

		for ( PlanRecord plan : agent.getRecords() ) {
			final JointPlan jointPlan = jointPlans.getJointPlan( plan.getPlan() );
			if ( jointPlan == null ) continue;

			// could already be handled
			if ( !recordsPerJointPlan.contains( jointPlan ) ) continue;
			for ( PlanRecord record : recordsPerJointPlan.remove( jointPlan ) ) {
				record.setInfeasible();
			}
		}
	}

	private static boolean areAllPlansPointed(
			final Collection<PlanRecord> records) {
		for ( PlanRecord record : records ) {
			assert record.isFeasible();
			if ( !record.isPointed() ) {
				return false;
			}
		}
		return true;
	}

	public interface ConflictSolver {
		void attemptToSolveConflicts( RecordsOfJointPlan recordsPerJointPlan );
	}

	public static class RecordsOfJointPlan {
		private final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan = new HashMap<>();
		private final Map<Plan, PlanRecord> recordsPerIndividualPlan = new HashMap<>();
		private final JointPlans jointPlans;

		private int maxJointPlanSize = 0;

		public void addRecord( final PlanRecord r ) {
			final JointPlan jp = jointPlans.getJointPlan( r.getPlan() );
			if ( jp != null ) {
				MapUtils.getCollection(
						jp,
						recordsPerJointPlan ).add( r );
				maxJointPlanSize = Math.max( maxJointPlanSize , jp.getIndividualPlans().size() );
			}
			else {
				recordsPerIndividualPlan.put( r.getPlan() , r );
			}
		}

		public RecordsOfJointPlan( final JointPlans jointPlans ) {
			this.jointPlans = jointPlans;
		}

		public Collection<PlanRecord> getRecords( final JointPlan jointPlan ) {
			return recordsPerJointPlan.get( jointPlan );
		}

		public Collection<PlanRecord> getRecords( final Plan plan ) {
			final JointPlan jp = jointPlans.getJointPlan( plan );
			return jp != null ?
					recordsPerJointPlan.get( jp ) :
					Collections.singleton( recordsPerIndividualPlan.get( plan ) );
		}

		public boolean contains( final JointPlan jointPlan ) {
			return recordsPerJointPlan.containsKey( jointPlan );
		}

		public Collection<PlanRecord> remove( final JointPlan jointPlan ) {
			return recordsPerJointPlan.remove( jointPlan );
		}

		public Collection<PlanRecord> removeJointPlan( final Plan plan ) {
			return recordsPerJointPlan.remove( jointPlans.getJointPlan( plan ) );
		}

		public Iterable<Map.Entry<JointPlan, Collection<PlanRecord>>> entrySet() {
			return recordsPerJointPlan.entrySet();
		}

		public Iterable<Collection<PlanRecord>> values() {
			return recordsPerJointPlan.values();
		}

		public int getMaxJointPlanSize() {
			return maxJointPlanSize;
		}

		@Override
		public String toString() {
			return "RecordsOfJointPlan{" +
					"recordsPerJointPlan ("+recordsPerJointPlan.size()+")=" + recordsPerJointPlan +
					", recordsPerIndividualPlan ("+recordsPerIndividualPlan.size()+")=" + recordsPerIndividualPlan +
					'}';
		}
	}
}

