/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerDecoderFactory.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * @author thibautd
 */
public class JointPlanOptimizerDecoderFactory {

	private final JointPlan plan;
	private final JointReplanningConfigGroup configGroup;
	private final LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final PlansCalcRoute routingAlgorithm;
	private final Network network;
	private final int numJointEpisodes;
	private final int numEpisodes;

	public JointPlanOptimizerDecoderFactory(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final int numJointEpisodes,
			final int numEpisodes) {
		this.plan = plan;
		this.configGroup = configGroup;
		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorFactory;
		this.routingAlgorithm = routingAlgorithm;
		this.network = network;
		this.numJointEpisodes = numJointEpisodes;
		this.numEpisodes = numEpisodes;
	}

	public JointPlanOptimizerDecoder createDecoder() {
		JointPlanOptimizerPipedDecoder output =
			new JointPlanOptimizerPipedDecoder(this.plan);

		if (configGroup.getOptimizeToggle()) {
			output.addDecoder(new ToggleDecoder(this.plan));
		}

		output.addDecoder(new DurationDecoder(
			this.plan,
			this.configGroup,
			this.legTravelTimeEstimatorFactory,
			this.routingAlgorithm,
			this.network,
			this.numJointEpisodes,
			this.numEpisodes));

		return output;
	}
}

