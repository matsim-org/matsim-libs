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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointPlans;
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

	private final ExtraPlanRemover remover;
	private final Random random;

	public GroupStrategyManager(
			final GroupLevelPlanSelector selectorForRemoval,
			final GroupStrategyRegistry registry,
			final int maxPlanPerAgent) {
		this.registry = registry;
		this.random = MatsimRandom.getLocalInstance();
		this.remover = new DumbExtraPlanRemover(
				selectorForRemoval,
				maxPlanPerAgent );
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
			remover.removePlansInGroup(
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

	private static void logAlloc(
			final ReplanningGroup g,
			final GroupPlanStrategy strategy) {
		if ( !log.isTraceEnabled() ) return;
	
		final List<Id> ids = new ArrayList<Id>();
		for (Person p : g.getPersons()) ids.add( p.getId() );

		log.trace( "group "+ids+" gets strategy "+strategy );
	}
}

