/* *********************************************************************** *
 * project: org.matsim.*
 * BasicJointPlanFitnessFunction.java
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
package playground.thibautd.jointtrips.replanning.modules.jointtripsselector;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.FitnessFunction;
import playground.thibautd.tsplanoptimizer.framework.Solution;

/**
 * A general fitness function for scoring joint plan solutions: it just gets
 * the plan returned by {@link Solution#getRepresentedPlan()} and parses it.
 *
 * @author thibautd
 */
public class BasicJointPlanFitnessFunction implements FitnessFunction {
	private final ScoringFunctionFactory factory;
	private final Map<Solution, Double> knownScores = new HashMap<Solution, Double>();

	public BasicJointPlanFitnessFunction(
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.factory = scoringFunctionFactory;
	}

	@Override
	public double computeFitnessValue(final Solution solution) {
		Double score = knownScores.get( solution );

		if (score == null) {
			JointPlan jointPlan = (JointPlan) solution.getRepresentedPlan();

			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				ScoringFunction scoringFunction = factory.createNewScoringFunction( indivPlan );

				for (PlanElement pe : indivPlan.getPlanElements()) {
					if (pe instanceof Activity) {
						scoringFunction.handleActivity( (Activity) pe );
					}
					else if (pe instanceof Leg) {
						scoringFunction.handleLeg( (Leg) pe );
					}
					else {
						throw new RuntimeException( "unknown PlanElement type "+pe.getClass() );
					}
				}

				scoringFunction.finish();
				indivPlan.setScore( scoringFunction.getScore() );
			}

			score = jointPlan.getScore();
			if (Double.isNaN( score )) {
				throw new RuntimeException( "got a NaN score for plan "+jointPlan.getIndividualPlanElements() );
			}
			knownScores.put( solution , score );
		}

		return score;
	}
}

