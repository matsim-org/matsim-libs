/* *********************************************************************** *
 * project: org.matsim.*
 * BasicFitness.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.tsplanoptimizer.framework.FitnessFunction;
import playground.thibautd.tsplanoptimizer.framework.Solution;

/**
 * A simple fitness function, which embeds a Matsim scoring function.
 *
 * @author thibautd
 */
public class BasicFitness implements FitnessFunction<Plan> {
	private static final Logger log =
		Logger.getLogger(BasicFitness.class);

	// print a lot of information.
	// to use with one thread only.
	private final static boolean DEBUG = false;
	private final ScoringFunctionFactory factory;
	private final double NEGATIVE_DURATION_PENALTY = 100;

	public BasicFitness(
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.factory = scoringFunctionFactory;
	}

	@Override
	public double computeFitnessValue(final Solution<? extends Plan> solution) {
		double score;

		Plan plan = solution.getPhenotype();

		ScoringFunction scoringFunction = factory.createNewScoringFunction( plan.getPerson() );

		if (DEBUG) log.debug( "start scoring" );

		double accumulatedNegativeDuration = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (DEBUG) log.debug( "handle plan element "+pe );

			if (pe instanceof Activity) {
				scoringFunction.handleActivity( (Activity) pe );

				double duration = ((Activity) pe).getEndTime() - ((Activity) pe).getStartTime();
				if (duration != Time.UNDEFINED_TIME && duration < 0) {
					// all matsim scoring functions do not take that into account
					accumulatedNegativeDuration += duration;
				}
			}
			else if (pe instanceof Leg) {
				scoringFunction.handleLeg( (Leg) pe );
			}
			else {
				throw new RuntimeException( "unknown PlanElement type "+pe.getClass() );
			}
		}

		scoringFunction.finish();

		score = scoringFunction.getScore();

		if (Double.isNaN( score )) {
			throw new RuntimeException( "got a NaN score for plan "+plan );
		}

		if (DEBUG) {
			log.debug( "scoring ended." );
			log.debug( "score: "+score );
			log.debug( "accumulated negative duration: "+accumulatedNegativeDuration );
		}
		return score + (accumulatedNegativeDuration * NEGATIVE_DURATION_PENALTY);
	}
}

