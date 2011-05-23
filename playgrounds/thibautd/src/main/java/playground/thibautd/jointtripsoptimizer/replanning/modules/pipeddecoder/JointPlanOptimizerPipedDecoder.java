/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerPipedDecoder.java
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

import java.util.ArrayList;
import java.util.List;

import org.jgap.IChromosome;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;

/**
 * Modular decoder, decoding each "dimension" one after the other.
 * @author thibautd
 */
public class JointPlanOptimizerPipedDecoder implements JointPlanOptimizerDecoder {
	private final List<JointPlanOptimizerDimensionDecoder> decoders =
		new ArrayList<JointPlanOptimizerDimensionDecoder>();
	private final JointPlan plan;

	public JointPlanOptimizerPipedDecoder(final JointPlan plan) {
		this.plan = plan;
	}

	/**
	 * Add a decoder.
	 * The order is important.
	 */
	public void addDecoder(final JointPlanOptimizerDimensionDecoder decoder) {
		this.decoders.add(decoder);
	}

	/**
	 * execute the decoders in the order they were added.
	 */
	@Override
	public JointPlan decode(final IChromosome chromosome) {
		JointPlan outputPlan = this.plan;

		for (JointPlanOptimizerDimensionDecoder decoder : this.decoders) {
			outputPlan = decoder.decode(chromosome, outputPlan);
		}

		return outputPlan;
	}
}

