/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractJointPlanOptimizerFitnessFunction.java
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

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPChromosome;

/**
 * Extend this class to provide a fitness function for the joint replanning algorithm.
 *
 *
 * @author thibautd
 */
abstract public class AbstractJointPlanOptimizerFitnessFunction extends FitnessFunction {
	public static final double NO_FITNESS_VALUE = Double.NEGATIVE_INFINITY;
	private double lastComputedFitnessValue = NO_FITNESS_VALUE;

	/**
	 * @return a decoder which creates plans consistent with the scores
	 */
	abstract public JointPlanOptimizerDecoder getDecoder(); 

	/**
	 * @return the individual scores for the members of the clique
	 */
	abstract protected double[] evaluateIndividuals(final IChromosome chromosome);

	/**
	 * Reimplements the jgap default by allowing a negative fitness and
	 * setting the individual scores.
	 */
	@Override
	public double getFitnessValue(final IChromosome a_subject) {
		double[] fitnessValues = evaluateIndividuals(a_subject);
		double fitnessValue = evaluate(a_subject);
		this.lastComputedFitnessValue = fitnessValue;

		try {
			((JointPlanOptimizerJGAPChromosome) a_subject).setIndividualScores(fitnessValues);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
					"can only run AbstractJointPlanOptimizerFitnessFunction with JointPlanOptimizerJGAPChromosome chromosomes",
					e);
		}

		return fitnessValue;
	}

	@Override
	public double getLastComputedFitnessValue() {
		return this.lastComputedFitnessValue;
	}
}

