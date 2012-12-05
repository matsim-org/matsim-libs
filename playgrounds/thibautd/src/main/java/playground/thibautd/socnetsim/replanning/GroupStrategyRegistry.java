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

import org.matsim.core.gbl.MatsimRandom;

/**
 * @author thibautd
 */
public final class GroupStrategyRegistry {
	private final List<GroupPlanStrategy> strategies = new ArrayList<GroupPlanStrategy>();
	private final List<Double> weights = new ArrayList<Double>();
	private double sumOfWeights = 0;

	public final void addStrategy(
			final GroupPlanStrategy strategy,
			final double weight) {
		strategies.add( strategy );
		weights.add( weight );
		sumOfWeights += weight;
	}

	public GroupPlanStrategy chooseStrategy() {
		final double choice = MatsimRandom.getRandom().nextDouble();
		double cumul = 0;
		int i = 0;

		Iterator<Double> iter = weights.iterator();
		while ( choice > cumul ) {
			cumul += iter.next();
			i++;
		}

		return strategies.get( i );
	}



}

