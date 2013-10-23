/* *********************************************************************** *
 * project: org.matsim.*
 * GroupStrategyManager.java
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
package playground.thibautd.socnetsim.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;

/**
 * Implements the group-level replanning logic.
 * Not very different from the standard StrategyManager.
 * @author thibautd
 */
public class GroupStrategyManager {
	private static final Logger log =
		Logger.getLogger(GroupStrategyManager.class);

	private final GroupStrategyRegistry registry;

	private final GroupLevelPlanSelector selectorForRemoval;
	private final int maxPlanPerAgent;
	private final Random random;
	private RemovedPlanListener removalListener = new RemovedPlanListener() {
		@Override
		public void notifyPlanRemovedForAgent(Plan plan, Person person) {}
	};

	public GroupStrategyManager(
			final GroupLevelPlanSelector selectorForRemoval,
			final GroupStrategyRegistry registry,
			final int maxPlanPerAgent) {
		this.selectorForRemoval = selectorForRemoval;
		this.registry = registry;
		this.maxPlanPerAgent = maxPlanPerAgent;
		this.random = MatsimRandom.getLocalInstance();
	}

	public final void run(
			final int iteration,
			final ControllerRegistry controllerRegistry) {
		final Population population = controllerRegistry.getScenario().getPopulation();
		final JointPlans jointPlans = controllerRegistry.getJointPlans();
		final Collection<ReplanningGroup> groups = controllerRegistry.getGroupIdentifier().identifyGroups( population );

		final Map<GroupPlanStrategy, List<ReplanningGroup>> strategyAllocations =
			new LinkedHashMap<GroupPlanStrategy, List<ReplanningGroup>>();
		for (ReplanningGroup g : groups) {
			removeExtraPlans(
					jointPlans,
					g );

			final GroupPlanStrategy strategy = registry.chooseStrategy( iteration , random.nextDouble() );
			List<ReplanningGroup> alloc = strategyAllocations.get( strategy );

			if (alloc == null) {
				alloc = new ArrayList<ReplanningGroup>();
				strategyAllocations.put( strategy , alloc );
			}

			logAlloc( g , strategy );
			alloc.add( g );
		}

		for (Map.Entry<GroupPlanStrategy, List<ReplanningGroup>> e : strategyAllocations.entrySet()) {
			final GroupPlanStrategy strategy = e.getKey();
			final List<ReplanningGroup> toHandle = e.getValue();
			log.info( "passing "+toHandle.size()+" groups to strategy "+strategy );
			strategy.run(
					controllerRegistry.createReplanningContext( iteration ),
					jointPlans,
					toHandle );
			log.info( "strategy "+strategy+" finished" );
		}
	}

	private final void removeExtraPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		int c = 0;
		while ( removeOneExtraPlan( jointPlans , group ) ) c++;
		if (c > 0) {
			// in the clique setting, it is impossible.
			// replace by a warning (or nothing) when there is a setting
			// in which this makes sense.
			throw new RuntimeException( (c+1)+" removal iterations were performed for group "+group );
		}
	}

	private final boolean removeOneExtraPlan(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		if (log.isTraceEnabled()) log.trace( "removing plans for group "+group );
		if (maxPlanPerAgent <= 0) return false;

		GroupPlans toRemove = null;
		boolean stillSomethingToRemove = false;
		final List<Person> personsToHandle = new ArrayList<Person>( group.getPersons() );
		while ( !personsToHandle.isEmpty() ) {
			final Person person = personsToHandle.remove( 0 );
			if (person.getPlans().size() <= maxPlanPerAgent) continue;
			if (log.isTraceEnabled()) log.trace( "Too many plans for person "+person );

			if (toRemove == null) {
				toRemove = selectorForRemoval.selectPlans( jointPlans , group );
				if (log.isTraceEnabled()) log.trace( "plans to remove will be taken from "+toRemove );
				if ( toRemove == null ) {
					throw new RuntimeException(
							"The selector for removal returned no plan "
							+"for group "+group );
				}
			}

			for (Plan plan : toRemove( jointPlans , person , toRemove )) {
				final Person personToHandle = plan.getPerson();
				if (log.isTraceEnabled()) log.trace( "removing plan "+plan+" for person "+personToHandle );
				removalListener.notifyPlanRemovedForAgent( plan , personToHandle );

				if ( personToHandle != person ) {
					final boolean removed = personsToHandle.remove( personToHandle );
					if ( !removed ) throw new RuntimeException( "person "+personToHandle+" is not part of the persons to handle!" );
				}

				final boolean removed = personToHandle.getPlans().remove( plan );
				if ( !removed ) throw new RuntimeException( "could not remove plan "+plan+" of person "+personToHandle );

				final JointPlan jpToRemove = jointPlans.getJointPlan( plan );
				if ( jpToRemove != null ) jointPlans.removeJointPlan( jpToRemove );
			}

			if (!stillSomethingToRemove && person.getPlans().size() > maxPlanPerAgent) {
				stillSomethingToRemove = true;
			}
		}

		if (stillSomethingToRemove && log.isTraceEnabled()) {
			log.trace( "still something to remove for group "+group );
		}

		return stillSomethingToRemove;
	}

	private static final Collection<Plan> toRemove(
			final JointPlans jointPlans,
			final Person person,
			final GroupPlans toRemove) {
		for (Plan plan : person.getPlans()) {
			JointPlan jp = jointPlans.getJointPlan( plan );

			if (jp != null && toRemove.getJointPlans().contains( jp )) {
				return jp.getIndividualPlans().values();
			}

			if (jp == null && toRemove.getIndividualPlans().contains( plan )) {
				return Collections.singleton( plan );
			}
		}
		throw new IllegalArgumentException();
	}

	public void setRemovedPlanListener(final RemovedPlanListener l) {
		this.removalListener = l;
	}

	private static void logAlloc(
			final ReplanningGroup g,
			final GroupPlanStrategy strategy) {
		if ( !log.isTraceEnabled() ) return;
	
		final List<Id> ids = new ArrayList<Id>();
		for (Person p : g.getPersons()) ids.add( p.getId() );

		log.trace( "group "+ids+" gets strategy "+strategy );
	}

	public static interface RemovedPlanListener {
		public void notifyPlanRemovedForAgent(Plan plan, Person person);
	}
}

