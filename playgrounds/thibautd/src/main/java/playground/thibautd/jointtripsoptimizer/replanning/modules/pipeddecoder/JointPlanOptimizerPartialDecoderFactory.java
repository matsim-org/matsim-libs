/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerPartialDecoderFactory.java
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

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Creates decoders which does not decodes durations, for usage with "on the fly"
 * duration scoring.
 *
 * @author thibautd
 */
public class JointPlanOptimizerPartialDecoderFactory {

	private final JointPlan plan;
	private final JointReplanningConfigGroup configGroup;
	private final int numJointEpisodes;
	private final int numEpisodes;

	public JointPlanOptimizerPartialDecoderFactory(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final int numJointEpisodes,
			final int numEpisodes) {
		this.plan = plan;
		this.configGroup = configGroup;
		this.numJointEpisodes = numJointEpisodes;
		this.numEpisodes = numEpisodes;
	}

	public JointPlanOptimizerDecoder createDecoder() {
		JointPlanOptimizerPipedDecoder output =
			new JointPlanOptimizerPipedDecoder(this.plan);

		if (configGroup.getOptimizeToggle()) {
			output.addDecoder(new ToggleDecoder(this.plan));
		}

		if (configGroup.getModeToOptimize()) {
			output.addDecoder(new ModeDecoder(
						this.plan,
						this.configGroup,
						this.numJointEpisodes,
						this.numEpisodes));
		}

		return output;
	}
}

