/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.thibautd.socnetsim.replanning.selectors;

import java.util.Random;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector.WeightCalculator;

public class LogitWeight implements WeightCalculator {
	private final Random random;
	private final double scaleParameter;
	
	public LogitWeight(
			final Random random,
			final double scale) {
		this.random = random;
		this.scaleParameter = scale;
	}
	
	@Override
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup group) {
		final Double score = indivPlan.getScore();
		if (score == null) return Double.POSITIVE_INFINITY;
		return score + nextErrorTerm();
	}

	private double nextErrorTerm() {
		// "inversion sampling": sample a number between 0 and 1,
		// and apply the inverse of the CDF to it.
		double choice = random.nextDouble();

		double value = Math.log( choice );
		if (value == Double.MIN_VALUE) {
			LogitSumSelector.log.warn( "underflow 1 for choice "+choice );
		}

		value = Math.log( -value );
		if (value == Double.MIN_VALUE) {
			LogitSumSelector.log.warn( "underflow 2 for choice "+choice );
		}

		return -value / scaleParameter;
	}
}
