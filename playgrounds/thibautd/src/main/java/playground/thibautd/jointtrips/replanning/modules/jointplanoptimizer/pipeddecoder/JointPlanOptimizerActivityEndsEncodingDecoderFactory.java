/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerActivityEndsEncodingDecoderFactory.java
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.pipeddecoder;

import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.PlansCalcRoute;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerDecoder;

/**
 * Creates decoders for the encoding by activity ends.
 *
 * @author thibautd
 */
public class JointPlanOptimizerActivityEndsEncodingDecoderFactory {

	private final JointPlan plan;
	private final JointReplanningConfigGroup configGroup;
	private final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final PlansCalcRoute routingAlgorithm;
	private final Network network;
	private final int firstDoubleGeneIndex;
	private final JointPlanOptimizerPartialDecoderFactory partialFactory;

	public JointPlanOptimizerActivityEndsEncodingDecoderFactory(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final int numJointEpisodes,
			final int numEpisodes,
			final int firstDoubleGeneIndex) {
		this.plan = plan;
		this.configGroup = configGroup;
		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorFactory;
		this.routingAlgorithm = routingAlgorithm;
		this.network = network;
		this.firstDoubleGeneIndex = firstDoubleGeneIndex;
		// "externalize" the creation of part common between the full and the
		// partial decoder, for consistency reasons
		this.partialFactory = new JointPlanOptimizerPartialDecoderFactory(
				plan, configGroup, numJointEpisodes, numEpisodes);
	}

	public JointPlanOptimizerDecoder createDecoder() {
		JointPlanOptimizerPipedDecoder output = (JointPlanOptimizerPipedDecoder)
			this.partialFactory.createDecoder();

		output.addDecoder(new DurationDecoderActivityEndsEncoding(
			this.plan,
			this.configGroup,
			this.legTravelTimeEstimatorFactory,
			this.routingAlgorithm,
			this.network,
			this.firstDoubleGeneIndex,
			// order the ids to make iteration order deterministic
			new TreeSet<Id>( plan.getIndividualPlans().keySet() )));

		return output;
	}
}

