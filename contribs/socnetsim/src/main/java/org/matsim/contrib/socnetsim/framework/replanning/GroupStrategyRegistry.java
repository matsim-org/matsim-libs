/* *********************************************************************** *
 * project: org.matsim.*
 * GroupStrategyRegistry.java
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
package org.matsim.contrib.socnetsim.framework.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.core.utils.collections.MapUtils;

/**
 * @author thibautd
 */
public final class GroupStrategyRegistry {
	private final static MapUtils.Factory<SubpopulationRegistry> registryFactory =
		new MapUtils.Factory<SubpopulationRegistry>() {
			@Override
			public SubpopulationRegistry create() {
				return new SubpopulationRegistry();
			}
		};

	private ExtraPlanRemover remover = null;
	private final Map<String, SubpopulationRegistry> populationRegistries = new HashMap<String, SubpopulationRegistry>();

	public final void addStrategy(
			final GroupPlanStrategy strategy,
			final String subpopulation,
			final double weight,
			final int lastIteration) {
		final SubpopulationRegistry registry = 
			MapUtils.getArbitraryObject(
					subpopulation,
					populationRegistries,
					registryFactory );
		registry.addStrategy(
				strategy,
				weight,
				lastIteration );
	}

	public GroupPlanStrategy chooseStrategy(
			final int iteration,
			final String subpopulation,
			final double randomDraw ) {
		final SubpopulationRegistry registry = 
			MapUtils.getArbitraryObject(
					subpopulation,
					populationRegistries,
					registryFactory );
		return registry.chooseStrategy(
				iteration,
				randomDraw );
	}

	public ExtraPlanRemover getExtraPlanRemover() {
		if ( this.remover == null ) {
			throw new IllegalStateException( "no removal selector factory defined" );
		}
		return this.remover;
	}

	public void setExtraPlanRemover(
			final ExtraPlanRemover remover) {
		if ( this.remover != null ) {
			throw new IllegalStateException( "already removal selector "+this.remover );
		}
		this.remover = remover;
	}
}

class SubpopulationRegistry {
	private static final Logger log = Logger.getLogger( SubpopulationRegistry.class );
	private final List<GroupPlanStrategy> strategies = new ArrayList<GroupPlanStrategy>();
	private final List<Double> weights = new ArrayList<Double>();
	private final List<Integer> lastIters = new ArrayList<Integer>();

	public final void addStrategy(
			final GroupPlanStrategy strategy,
			final double weight,
			final int lastIteration) {
		if ( weight <= 0.0 ) {
			log.info( "strategy "+strategy+" with weight "+weight+" will not be added: weight negative or null." );
			return;
		}
		strategies.add( strategy );
		weights.add( weight );
		lastIters.add( lastIteration < 0 ? Integer.MAX_VALUE : lastIteration );
	}

	public GroupPlanStrategy chooseStrategy(
			final int iteration,
			final double randomDraw ) {
		if (randomDraw < 0 || randomDraw > 1) throw new IllegalArgumentException( ""+randomDraw );
		final double choice = randomDraw * calcSumOfWeights( iteration );

		double cumul = 0;
		for ( int i = 0; i < weights.size(); i++ ) {
			if ( iteration <= lastIters.get( i ) ) {
				cumul += weights.get( i );
				if ( cumul > choice ) return strategies.get( i );
			}
		}

		throw new RuntimeException( "choice="+randomDraw+" not found in "+calcSumOfWeights( iteration ) );
	}

	private double calcSumOfWeights(final int iteration) {
		double sum = 0;
		for ( int i = 0; i < weights.size(); i++ ) {
			if ( iteration <= lastIters.get( i ) ) {
				sum += weights.get( i );
			}
		}
		return sum;
	}

}
