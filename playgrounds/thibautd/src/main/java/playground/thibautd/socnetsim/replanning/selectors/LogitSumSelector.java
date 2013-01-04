/* *********************************************************************** *
 * project: org.matsim.*
 * LogitSumSelector.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;

/**
 * A selector which draws gumbel-distributed scores for individual plans.
 * Without joint plan, this results in logit marginals for the selection for
 * each agent.
 *
 * @author thibautd
 */
public class LogitSumSelector extends AbstractHighestWeightSelector {
	private static final Logger log =
		Logger.getLogger(LogitSumSelector.class);

	private final Random random;
	private final double scaleParameter;

	public LogitSumSelector(
			final Random random,
			final double scaleParameter) {
		this.random = random;
		this.scaleParameter = scaleParameter;
	}

	@Override
	public double getWeight(final Plan indivPlan) {
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
			log.warn( "underflow 1 for choice "+choice );
		}

		value = Math.log( -value );
		if (value == Double.MIN_VALUE) {
			log.warn( "underflow 2 for choice "+choice );
		}

		return -value / scaleParameter;
	}
}

