/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPMutation.java
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

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Class mutating random members from the previous generation.
 * The type of mutation differs depending on the gene:
 * -"toggle" gene: the value is changed with probability 1.
 * -duration gene: GENOCOP (Michalewicz and Janikow, 1996) like "non-uniform
 *  mutation" (preserves the inequality constraints).
 *
 * @author thibautd
 */
public class JointPlanOptimizerJGAPMutation implements GeneticOperator {

	private static final long serialVersionUID = 1L;

	private final double MUTATION_PROB;
	private final int CHROMOSOME_SIZE;

	private final int NUM_ITER;
	private final double NON_UNIFORMITY_PARAM;

	private final RandomGenerator randomGenerator;
	private final Configuration jgapConfig;

	public JointPlanOptimizerJGAPMutation(
			Configuration config,
			JointReplanningConfigGroup configGroup,
			int chromosomeSize) {
		this.CHROMOSOME_SIZE = chromosomeSize;
		this.MUTATION_PROB = configGroup.getMutationProbability();
		this.NUM_ITER = configGroup.getMaxIterations();
		this.NON_UNIFORMITY_PARAM = configGroup.getMutationNonUniformity();

		this.jgapConfig = config;

		this.randomGenerator = config.getRandomGenerator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {
		int populationSize = a_population.size();
		Gene geneToMute;
		double freeSpace;
		IChromosome currentChromosome;
		IChromosome copyOfChromosome = null;
		// no need to check cast (Chromosomes implement IChromosome)

		for (int j=0; j < populationSize; j++) {
			currentChromosome = a_population.getChromosome(j);
			//for each gene in the chromosome:
			for (int i=0; i < this.CHROMOSOME_SIZE; i++) {
				if (this.randomGenerator.nextDouble() > this.MUTATION_PROB) {
					//perform mutation

					// if not already done for this chromosome, make a copy of
					// the current chromosome and add it to the candidates.
					if (copyOfChromosome == null) {
						copyOfChromosome = (IChromosome) currentChromosome.clone();
						a_candidateChromosome.add(copyOfChromosome);
					}
					geneToMute = copyOfChromosome.getGene(i);

					if (geneToMute instanceof BooleanGene) {
						mutateBoolean((BooleanGene) geneToMute);
					}
					else if (geneToMute instanceof DoubleGene) {

						freeSpace = getFreeSpace(
								(DoubleGene) geneToMute,
								currentChromosome);

						mutateDouble((DoubleGene) geneToMute, freeSpace);
					}
				}
			}
			copyOfChromosome = null;
		}
	}

	private final double getFreeSpace(
			DoubleGene geneToMute,
			IChromosome chromosome) {
		double freeSpace = geneToMute.getUpperBound();

		for (Gene gene : chromosome.getGenes()) {
			if ((gene != geneToMute)&&(gene instanceof DoubleGene)) {
				freeSpace -= ((DoubleGene) gene).doubleValue();
			}
		}

		return freeSpace;
	}

	private final void mutateBoolean(BooleanGene gene) {
		gene.setAllele(!gene.booleanValue());
	}

	private final void mutateDouble(DoubleGene gene, Double freeSpace) {
		// GENOCOP (Michalewicz, Janikow, 1996) like "non uniform" mutation.
		double value = gene.doubleValue();
		int iter = this.jgapConfig.getGenerationNr();

		if (this.randomGenerator.nextInt(2) == 0) {
			value += delta(iter, gene.getUpperBound() - value);
		} else {
			value -= delta(iter, value - gene.getLowerBound());
		}

		gene.setAllele(value);
	}

	private final double delta(int t, double y) {
		double r = this.randomGenerator.nextDouble();
		double exponant = Math.pow((1d - t/this.NUM_ITER), this.NON_UNIFORMITY_PARAM);

		return (y * (1 - Math.pow(r, exponant)));
	}
}

