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
package playground.thibautd.socnetsim.framework.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.utils.objectattributes.ObjectAttributes;

import org.matsim.core.utils.collections.MapUtils;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;

/**
 * Implements the group-level replanning logic.
 * Not very different from the standard StrategyManager.
 * @author thibautd
 */
public class GroupStrategyManager {
	private static final Logger log =
		Logger.getLogger(GroupStrategyManager.class);

	private IterationStopWatch stopWatch;

	private final GroupIdentifier groupIdentifier;
	private final GroupStrategyRegistry registry;

	private final Random random;
	private final List<Listener> listeners = new ArrayList<Listener>( 1 );

	public GroupStrategyManager(
			final IterationStopWatch stopWatch,
			final GroupIdentifier groupIdentifier,
			final GroupStrategyRegistry registry ) {
		this.stopWatch = stopWatch;
		this.groupIdentifier = groupIdentifier;
		this.registry = registry;
		this.random = MatsimRandom.getLocalInstance();
	}

	public void addListener(final Listener l) {
		listeners.add( l );
	}

	public final void run(
			final ReplanningContext context,
			final Scenario scenario) {
		final Population population = scenario.getPopulation();
		final JointPlans jointPlans = (JointPlans) scenario.getScenarioElement( JointPlans.ELEMENT_NAME );
		final Collection<ReplanningGroup> groups = groupIdentifier.identifyGroups( population );

		final Map<GroupPlanStrategy, List<ReplanningGroup>> strategyAllocations =
			new LinkedHashMap<GroupPlanStrategy, List<ReplanningGroup>>();
		if ( stopWatch != null ) stopWatch.beginOperation( "remove plans alloc strategy" );
		for (ReplanningGroup g : groups) {
			registry.getExtraPlanRemover().removePlansInGroup(
					jointPlans,
					g );

			final String subpop = identifySubpopulation( g , scenario );
			final GroupPlanStrategy strategy = registry.chooseStrategy( context.getIteration() , subpop , random.nextDouble() );
			final List<ReplanningGroup> alloc = MapUtils.getList( strategy , strategyAllocations );

			notifyAlloc( context.getIteration() , g , strategy );
			alloc.add( g );
		}
		if ( stopWatch != null ) stopWatch.endOperation( "remove plans alloc strategy" );

		for (Map.Entry<GroupPlanStrategy, List<ReplanningGroup>> e : strategyAllocations.entrySet()) {
			final GroupPlanStrategy strategy = e.getKey();
			final List<ReplanningGroup> toHandle = e.getValue();
			log.info( "passing "+toHandle.size()+" groups to strategy "+strategy );
			if ( stopWatch != null ) stopWatch.beginOperation( "strategy "+strategy );
			strategy.run(
					context,
					jointPlans,
					toHandle );
			if ( stopWatch != null ) stopWatch.endOperation( "strategy "+strategy );
			log.info( "strategy "+strategy+" finished" );
		}
	}

	private static String identifySubpopulation(
			final ReplanningGroup g,
			final Scenario sc) {
		final String attName = sc.getConfig().plans().getSubpopulationAttributeName();
		final ObjectAttributes atts = sc.getPopulation().getPersonAttributes();

		String name = null;

		for ( Person p : g.getPersons() ) {
			final String persSubPop = (String) atts.getAttribute( p.getId().toString() , attName );
			if ( persSubPop == null && name != null ) throw new RuntimeException( "inconsistent subpopulations in group "+g );
			if ( name != null && !name.equals( persSubPop ) ) throw new RuntimeException( "inconsistent subpopulations in group "+g );
			name = persSubPop;
		}

		return name;
	}

	private void notifyAlloc(
			final int i,
			final ReplanningGroup g,
			final GroupPlanStrategy strategy) {
		for ( Listener l : listeners ) l.notifyAlloc( i , g , strategy );
	}

	public static interface Listener {
		public void notifyAlloc(
				final int iteration,
				final ReplanningGroup g,
				final GroupPlanStrategy strategy );
	}
}

