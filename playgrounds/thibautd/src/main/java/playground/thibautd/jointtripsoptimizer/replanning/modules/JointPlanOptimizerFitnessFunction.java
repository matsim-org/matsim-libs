/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerFitnessFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * @author thibautd
 */
public class JointPlanOptimizerFitnessFunction extends FitnessFunction {

	private static final long serialVersionUID = 1L;

	private final JointPlanOptimizerDecoder decoder;
	private final ScoringFunctionFactory scoringFunctionFactory;

	//TODO: suppress (for dummy tests only)
	public JointPlanOptimizerFitnessFunction() {
		super();
		this.decoder = null;
		this.scoringFunctionFactory = null;
	}

	public JointPlanOptimizerFitnessFunction(
			JointPlan plan,
			LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			PlansCalcRoute routingAlgorithm,
			Network network,
			int numJointEpisodes,
			int numEpisodes,
			ScoringFunctionFactory scoringFunctionFactory) {
		super();
		this.decoder = new JointPlanOptimizerDecoder(plan, legTravelTimeEstimatorFactory,
				routingAlgorithm, network, numJointEpisodes, numEpisodes);
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	@Override
	protected double evaluate(IChromosome chromosome) {
		JointPlan plan = this.decoder.decode(chromosome);
		return this.getScore(plan);
	}

	private double getScore(JointPlan plan) {
		ScoringFunction fitnessFunction = this.scoringFunctionFactory.createNewScoringFunction(plan);
		Activity currentActivity;
		Leg currentLeg;

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				currentActivity = (Activity) pe;
				fitnessFunction.startActivity(currentActivity.getStartTime(), currentActivity);
				fitnessFunction.endActivity(currentActivity.getEndTime());
			}
			else if (pe instanceof Leg) {
				currentLeg = (Leg) pe;
				fitnessFunction.startLeg(currentLeg.getDepartureTime(), currentLeg);
				fitnessFunction.endLeg(currentLeg.getDepartureTime() +
						currentLeg.getTravelTime());
			}
			else {
				throw new IllegalArgumentException("unrecognized plan element type");
			}
		}

		fitnessFunction.finish();

		return fitnessFunction.getScore();
	}

	public JointPlanOptimizerDecoder getDecoder() {
		return this.decoder;
	}

}

