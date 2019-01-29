/* *********************************************************************** *
 * project: org.matsim.*
 * LossWeight.java
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class LossWeight implements WeightCalculator {
	// XXX beware if make configurable: mustn't be random!
	private final WeightCalculator baseWeight = new ScoreWeight();

	@Override
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup replanningGroup) {
		final double planScore = baseWeight.getWeight( indivPlan , replanningGroup );
		final double bestScore = getBestScore( indivPlan.getPerson() , replanningGroup );

		return planScore - bestScore;
	}

	private double getBestScore(
			final Person person,
			final ReplanningGroup group) {
		double best = Double.NEGATIVE_INFINITY;

		for ( final Plan p : person.getPlans() ) {
			final double score = baseWeight.getWeight( p , group );
			if ( score > best ) best = score;
		}

		return best;
	}
}

