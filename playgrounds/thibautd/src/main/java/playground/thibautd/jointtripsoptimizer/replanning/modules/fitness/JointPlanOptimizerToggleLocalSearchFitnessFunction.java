/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerToggleLocalSearchFitnessFunction.java
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;

/**
 * Scores the plan, modifying the toggle genes to place them on a local optimum.
 * @author thibautd
 */
public class JointPlanOptimizerToggleLocalSearchFitnessFunction extends AbstractJointPlanOptimizerFitnessFunction {

	private final JointPlanOptimizerDecoder decoder;
	private final AbstractJointPlanOptimizerFitnessFunction nonOptimizingFitness;
	private final Random random;

	public JointPlanOptimizerToggleLocalSearchFitnessFunction(
			final Configuration jgapConfig,
			final JointPlanOptimizerDecoder fullDecoder,
			final AbstractJointPlanOptimizerFitnessFunction nonOptimizingFitness) {
		this.random = new Random(jgapConfig.getRandomGenerator().nextLong());
		this.decoder = fullDecoder;
		this.nonOptimizingFitness = nonOptimizingFitness;
	}

	@Override
	public JointPlanOptimizerDecoder getDecoder() {
		return decoder;
	}

	@Override
	protected double evaluate(final IChromosome chromosome) {
		double bestScore = nonOptimizingFitness.getFitnessValue(chromosome);

		List<BooleanGene> genes = new ArrayList<BooleanGene>();
		for (Gene gene : chromosome.getGenes()) {
			if (gene instanceof BooleanGene) {
				genes.add((BooleanGene) gene);
			}
		}

		Collections.shuffle(genes, random);

		boolean newAllele;
		double currentScore;
		for (BooleanGene gene : genes) {
			newAllele = !gene.booleanValue();
			gene.setAllele(newAllele);
			currentScore = nonOptimizingFitness.getFitnessValue(chromosome);

			if (currentScore > bestScore) {
				// remember the improvement
				bestScore = currentScore;
			}
			else {
				//revert changes
				gene.setAllele( !newAllele );
			}
		}

		return bestScore;
	}
}

