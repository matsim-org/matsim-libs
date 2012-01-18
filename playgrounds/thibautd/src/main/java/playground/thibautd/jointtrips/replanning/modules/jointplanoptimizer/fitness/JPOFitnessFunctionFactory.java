/* *********************************************************************** *
 * project: org.matsim.*
 * JPOFitnessFunctionFactory.java
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;

/**
 * @author thibautd
 */
public class JPOFitnessFunctionFactory {
	final JointPlanOptimizerJGAPConfiguration jgapConfig;
	final JointPlan plan;
	final JointReplanningConfigGroup configGroup;
	final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	final PlansCalcRoute routingAlgorithm;
	final Network network;
	final int numJointEpisodes;
	final int numEpisodes;
	final int nMembers;
	final ScoringFunctionFactory scoringFunctionFactory;

	public JPOFitnessFunctionFactory(
			final JointPlanOptimizerJGAPConfiguration jgapConfig,
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final int numJointEpisodes,
			final int numEpisodes,
			final int nMembers,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.jgapConfig = jgapConfig;
		this.plan = plan;
		this.configGroup = configGroup;
		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorFactory;
		this.routingAlgorithm = routingAlgorithm;
		this.network = network;
		this.numJointEpisodes = numJointEpisodes;
		this.numEpisodes = numEpisodes;
		this.nMembers = nMembers;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	public AbstractJointPlanOptimizerFitnessFunction createFitnessFunction() {
			return new JointPlanOptimizerOTFFitnessFunction(
					plan,
					configGroup,
					legTravelTimeEstimatorFactory,
					routingAlgorithm,
					network,
					numJointEpisodes,
					numEpisodes,
					nMembers,
					scoringFunctionFactory);
	}
}

