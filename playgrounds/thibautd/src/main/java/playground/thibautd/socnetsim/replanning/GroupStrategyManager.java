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
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.utils.MapUtils;

/**
 * Implements the group-level replanning logic.
 * Not very different from the standard StrategyManager.
 * @author thibautd
 */
public class GroupStrategyManager {
	private static final Logger log =
		Logger.getLogger(GroupStrategyManager.class);

	// unfortunately, the stopwatch is initilized at the contruction
	// of abstract controler, and we need to build this thing before
	// construction the Controller...
	// So the only way to get the stop watch is via a getter or as a parameter.
	private IterationStopWatch stopWatch = null;
	public void setStopWatch(final IterationStopWatch sw) {
		this.stopWatch = sw;
	}

	private final GroupStrategyRegistry registry;

	private final Random random;
	private final List<Listener> listeners = new ArrayList<Listener>( 1 );

	public GroupStrategyManager(
			final GroupStrategyRegistry registry ) {
		this.registry = registry;
		this.random = MatsimRandom.getLocalInstance();
	}

	public void addListener(final Listener l) {
		listeners.add( l );
	}

	public final void run(
			final int iteration,
			final ControllerRegistry controllerRegistry) {
		final Population population = controllerRegistry.getScenario().getPopulation();
		final JointPlans jointPlans = controllerRegistry.getJointPlans();
		final Collection<ReplanningGroup> groups = controllerRegistry.getGroupIdentifier().identifyGroups( population );

		final Map<GroupPlanStrategy, List<ReplanningGroup>> strategyAllocations =
			new LinkedHashMap<GroupPlanStrategy, List<ReplanningGroup>>();
		if ( stopWatch != null ) stopWatch.beginOperation( "remove plans alloc strategy" );
		for (ReplanningGroup g : groups) {
			registry.getExtraPlanRemover().removePlansInGroup(
					jointPlans,
					g );

			final GroupPlanStrategy strategy = registry.chooseStrategy( iteration , random.nextDouble() );
			final List<ReplanningGroup> alloc = MapUtils.getList( strategy , strategyAllocations );

			notifyAlloc( g , strategy );
			alloc.add( g );
		}
		if ( stopWatch != null ) stopWatch.endOperation( "remove plans alloc strategy" );

		for (Map.Entry<GroupPlanStrategy, List<ReplanningGroup>> e : strategyAllocations.entrySet()) {
			final GroupPlanStrategy strategy = e.getKey();
			final List<ReplanningGroup> toHandle = e.getValue();
			log.info( "passing "+toHandle.size()+" groups to strategy "+strategy );
			if ( stopWatch != null ) stopWatch.beginOperation( "strategy "+strategy );
			strategy.run(
					controllerRegistry.createReplanningContext( iteration ),
					jointPlans,
					toHandle );
			if ( stopWatch != null ) stopWatch.endOperation( "strategy "+strategy );
			log.info( "strategy "+strategy+" finished" );
		}
	}

	private void notifyAlloc(
			final ReplanningGroup g,
			final GroupPlanStrategy strategy) {
		for ( Listener l : listeners ) l.notifyAlloc( g , strategy );
	}

	public static interface Listener {
		public void notifyAlloc(
				final ReplanningGroup g,
				final GroupPlanStrategy strategy );
	}
}

