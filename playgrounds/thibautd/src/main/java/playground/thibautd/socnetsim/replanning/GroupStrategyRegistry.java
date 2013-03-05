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
package playground.thibautd.socnetsim.replanning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author thibautd
 */
public final class GroupStrategyRegistry {
	private static final Logger log = Logger.getLogger( GroupStrategyRegistry.class );
	private final List<GroupPlanStrategy> strategies = new ArrayList<GroupPlanStrategy>();
	private final List<Double> weights = new ArrayList<Double>();
	private double sumOfWeights = 0;

	public final void addStrategy(
			final GroupPlanStrategy strategy,
			final double weight) {
		if ( weight <= 0.0 ) {
			log.info( "strategy "+strategy+" with weight "+weight+" will not be added: weight negative or null." );
			return;
		}
		strategies.add( strategy );
		weights.add( weight );
		sumOfWeights += weight;
	}

	public GroupPlanStrategy chooseStrategy( final double randomDraw ) {
		if (randomDraw < 0 || randomDraw > 1) throw new IllegalArgumentException( ""+randomDraw );
		final double choice = randomDraw * sumOfWeights;
		int i = 0;

		Iterator<Double> iter = weights.iterator();
		double cumul = iter.next();
		while ( choice > cumul ) {
			cumul += iter.next();
			i++;
		}

		return strategies.get( i );
	}
}

