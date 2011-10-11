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
package playground.thibautd.jointtripsoptimizer.replanning.modules.fitness;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerDecoderFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerPartialDecoderFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * @author thibautd
 */
public class JPOFitnessFunctionFactory implements MatsimFactory {
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
		if (configGroup.getIsMemetic()) {
			JointPlanOptimizerMultiFitnessFunction fitness =
				new JointPlanOptimizerMultiFitnessFunction(jgapConfig);

			JointPlanOptimizerDecoder partial = (new JointPlanOptimizerPartialDecoderFactory(
				plan,
				configGroup,
				numJointEpisodes,
				numEpisodes)).createDecoder();
			JointPlanOptimizerDecoder full = (new JointPlanOptimizerDecoderFactory(
				plan,
				configGroup,
				legTravelTimeEstimatorFactory,
				routingAlgorithm,
				network,
				numJointEpisodes,
				numEpisodes,
				nMembers)).createDecoder();

			JointPlanOptimizerOTFFitnessFunction otfFitness = new JointPlanOptimizerOTFFitnessFunction(
					plan,
					configGroup,
					legTravelTimeEstimatorFactory,
					routingAlgorithm,
					network,
					numJointEpisodes,
					numEpisodes,
					nMembers,
					false,
					scoringFunctionFactory,
					partial,
					full);

			JointPlanOptimizerOTFFitnessFunction durationMemeticFitness = new JointPlanOptimizerOTFFitnessFunction(
					plan,
					configGroup,
					legTravelTimeEstimatorFactory,
					routingAlgorithm,
					network,
					numJointEpisodes,
					numEpisodes,
					nMembers,
					true,
					scoringFunctionFactory,
					partial,
					full);

			JointPlanOptimizerToggleLocalSearchFitnessFunction toggleMemetic =
				new JointPlanOptimizerToggleLocalSearchFitnessFunction(
					jgapConfig,
					full,
					otfFitness);

			fitness.addFitness(otfFitness, configGroup.getDirectFitnessWeight());
			fitness.addFitness(durationMemeticFitness, configGroup.getDurationMemeticFitnessWeight());
			fitness.addFitness(toggleMemetic, configGroup.getToggleMemeticFitnessWeight());

			return fitness;
		} else {
			return new JointPlanOptimizerOTFFitnessFunction(
					plan,
					configGroup,
					legTravelTimeEstimatorFactory,
					routingAlgorithm,
					network,
					numJointEpisodes,
					numEpisodes,
					nMembers,
					false,
					scoringFunctionFactory);
		}
	}
}

