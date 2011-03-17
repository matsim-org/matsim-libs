/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPCrossOver.java
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

import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.Configuration;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Cross breeds joint plans.
 * It does the following:
 * - on discrete variables: uniform cross-over.
 * - on continuous variables: GENOCOP-like "arithmetic" cross overs.
 *
 * assumes the following structure for the chromosome: [boolean genes]-[Double genes]
 * @author thibautd
 */
public class JointPlanOptimizerJGAPCrossOver implements GeneticOperator {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPCrossOver.class);


	private static final long serialVersionUID = 1L;

	private final double CO_RATE;
	private final int N_BOOL;
	private final int N_DOUBLE;

	private final RandomGenerator randomGenerator;

	public JointPlanOptimizerJGAPCrossOver(
			Configuration config,
			JointReplanningConfigGroup configGroup,
			int numJointEpisodes,
			int numEpisodes
			) {
		this.CO_RATE = configGroup.getCrossOverProbability();
		this.N_BOOL = numJointEpisodes;
		this.N_DOUBLE = numEpisodes;
		this.randomGenerator = config.getRandomGenerator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {
		int populationSize = a_population.size();
		int numOfCo = (int) Math.ceil(this.CO_RATE * populationSize);
		int index1;
		IChromosome mate1;
		int index2;
		IChromosome mate2;

		for (int i=0; i < numOfCo; i++) {
			// draw random parents
			index1 = this.randomGenerator.nextInt(populationSize);
			index2 = this.randomGenerator.nextInt(populationSize);
			mate1 = (IChromosome) a_population.getChromosome(index1).clone();
			mate2 = (IChromosome) a_population.getChromosome(index2).clone();

			doBooleanCrossOver(mate1, mate2);
			doDoubleCrossOver(mate1, mate2);

			a_candidateChromosome.add(mate1);
			a_candidateChromosome.add(mate2);
		}
	}

	/**
	 * Performs a uniform cross-over on the boolean valued genes.
	 */
	private final void doBooleanCrossOver(IChromosome mate1, IChromosome mate2) {
		boolean value1;
		boolean value2;
		// loop over boolean genes
		for (int i=0; i < this.N_BOOL; i++) {
			value1 = ((BooleanGene) mate1.getGene(i)).booleanValue();
			value2 = ((BooleanGene) mate2.getGene(i)).booleanValue();

			// exchange values with proba O.5
			if (this.randomGenerator.nextInt(2) == 0) {
				mate1.getGene(i).setAllele(value2);
				mate2.getGene(i).setAllele(value1);
			}
		}
	}

	/**
	 * Performs a "GENOCOP-like" "Whole arithmetic cross-over" on the double
	 * valued genes.
	 * @todo: implement the other GENOCOP COs.
	 */
	private final void doDoubleCrossOver(IChromosome mate1, IChromosome mate2) {
		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		double randomCoef = this.randomGenerator.nextDouble();

		for (int i=this.N_BOOL; i < this.N_BOOL + this.N_DOUBLE; i++) {
			gene1 = (DoubleGene) mate1.getGene(i);
			gene2 = (DoubleGene) mate2.getGene(i);
			oldValue1 = gene1.doubleValue();
			oldValue2 = gene2.doubleValue();
			
			gene1.setAllele(randomCoef*oldValue1 + (1 - randomCoef)*oldValue2);
			gene2.setAllele(randomCoef*oldValue2 + (1 - randomCoef)*oldValue1);
		}
	}
}

