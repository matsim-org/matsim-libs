/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerMultiFitnessFunction.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jgap.IChromosome;

import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;

/**
 * A fitness function wrapping several fitness functions, executed according to
 * a specified weight.
 *
 * This is meant to execute "optimising" fitness functions with given probabilities.
 * The result of the different fitness functions must be consistent - no check is done
 * at Runtime.
 *
 * @author thibautd
 */
public class JointPlanOptimizerMultiFitnessFunction extends AbstractJointPlanOptimizerFitnessFunction {
	private static final long serialVersionUID = 1L;

	//private final JointPlanOptimizerJGAPConfiguration jgapConfig;
	private final Random random;

	private final List<Tuple<Double, AbstractJointPlanOptimizerFitnessFunction>> fitnesses =
		new ArrayList<Tuple<Double, AbstractJointPlanOptimizerFitnessFunction>>();
	private double totalWeight = 0;

	public JointPlanOptimizerMultiFitnessFunction(
			final JointPlanOptimizerJGAPConfiguration jgapConfig) {
		//this.jgapConfig = jgapConfig;
		this.random = new Random(jgapConfig.getRandomGenerator().nextLong());
	}

	/**
	 * @return the decoder associated to the first fitness function, null if no
	 * fitness function is referenced.
	 */
	@Override
	public JointPlanOptimizerDecoder getDecoder() {
		try {
			return fitnesses.get(0).getSecond().getDecoder();
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public void addFitness(
			final AbstractJointPlanOptimizerFitnessFunction fitness,
			final double weight) {
		totalWeight += weight;
		fitnesses.add(
				new Tuple<Double, AbstractJointPlanOptimizerFitnessFunction>(
					weight,
					fitness) );
	}

	@Override
	protected double evaluate(final IChromosome chromosome) {
		double choice = random.nextDouble() * totalWeight;
		double currentUpperBound = 0;

		for (Tuple<Double, AbstractJointPlanOptimizerFitnessFunction>
				tuple : fitnesses) {
			currentUpperBound += tuple.getFirst();
			if (choice <= currentUpperBound) {
				return tuple.getSecond().getFitnessValue(chromosome);
			}
		}

		throw new RuntimeException("no fitness used");
	}
}

