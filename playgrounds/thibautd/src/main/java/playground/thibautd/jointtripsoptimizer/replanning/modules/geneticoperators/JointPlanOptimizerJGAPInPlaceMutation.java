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
package playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.replanning.modules.fitness.JointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Mutates toggle genes "in place".
 * The genes of the <u>candidate</u> chromosomes are mutated according to some 
 * probability, and the resulting offspring reimplaces the original chromosome
 * if it is fittest.
 *
 * Using this mutation operator can be seen as the usage of a local optimizer
 * for toggle gene, thus making useless the modification of those genes in genetic
 * operators.
 *
 * @author thibautd
 */
public class JointPlanOptimizerJGAPInPlaceMutation implements GeneticOperator {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPMutation.class);


	private static final long serialVersionUID = 1L;

	private final double MUTATION_PROB;
	private final int CHROMOSOME_SIZE;
	private final double DAY_DURATION;

	private final int NUM_ITER;
	private final double NON_UNIFORMITY_PARAM;

	private final List<Integer> nDurationGenes = new ArrayList<Integer>();
	private final List<Integer> geneIndices = new ArrayList<Integer>();

	private final RandomGenerator randomGenerator;
	private final Configuration jgapConfig;

	public JointPlanOptimizerJGAPInPlaceMutation(
			final JointPlanOptimizerJGAPConfiguration config,
			final JointReplanningConfigGroup configGroup,
			final int chromosomeSize,
			final List<Integer> nDurationGenes) {
		this.CHROMOSOME_SIZE = chromosomeSize;
		this.MUTATION_PROB = configGroup.getMutationProbability();
		this.NUM_ITER = configGroup.getMaxIterations();
		this.NON_UNIFORMITY_PARAM = configGroup.getMutationNonUniformity();
		this.DAY_DURATION = config.getDayDuration();

		// construct the list of gene indices.
		// the order in which genes are examined is determined by the
		// order of this list, shuffled for each chromosome.
		// This ensures that all genes have the same "status", as mutation
		// of a gene influences the mutation on the following.
		for (int i=0; i < chromosomeSize; i++) {
			this.geneIndices.add(i);
		}

		this.nDurationGenes.clear();
		this.nDurationGenes.addAll(nDurationGenes);

		this.jgapConfig = config;

		this.randomGenerator = config.getRandomGenerator();
	}

	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {
		int populationSize = a_candidateChromosome.size();
		Gene geneToMute;
		double freeSpace;
		IChromosome currentChromosome;
		IChromosome copyOfChromosome = null;

		for (int j=0; j < populationSize; j++) {
			currentChromosome = (IChromosome) a_candidateChromosome.get(j);
			Collections.shuffle(this.geneIndices, (Random) this.randomGenerator);

			//for each gene in the chromosome:
			for (int i : this.geneIndices) {
				if (this.randomGenerator.nextDouble() < this.MUTATION_PROB) {
					//perform mutation

					// if not already done for this chromosome, make a copy of
					// the current chromosome
					if (copyOfChromosome == null) {
						copyOfChromosome = (IChromosome) currentChromosome.clone();
					}
					copyOfChromosome.setFitnessValueDirectly(
							JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE);
					geneToMute = copyOfChromosome.getGene(i);

					if (geneToMute instanceof BooleanGene) {
						mutateBoolean((BooleanGene) geneToMute);
					}
					//else if (geneToMute instanceof DoubleGene) {

					//	freeSpace = getFreeSpace(
					//			i,
					//			copyOfChromosome);

					//	//mutateDoubleNonUniform((DoubleGene) geneToMute, freeSpace);
					//	mutateDouble((DoubleGene) geneToMute, freeSpace);
					//}
					//else if (geneToMute instanceof JointPlanOptimizerJGAPModeGene) {
					//	geneToMute.setToRandomValue(this.randomGenerator);
					//}
					if (copyOfChromosome.getFitnessValue() > 
							currentChromosome.getFitnessValue()) {
						a_candidateChromosome.remove(currentChromosome);
						a_candidateChromosome.add(copyOfChromosome);
						currentChromosome = copyOfChromosome;
						copyOfChromosome = null;
					}
				}
			}

			//replace mutated chromosome if new one better
			if ( (!(copyOfChromosome==null)) && 
					(copyOfChromosome.getFitnessValue() > 
					 currentChromosome.getFitnessValue()) ) {
				a_candidateChromosome.remove(currentChromosome);
				a_candidateChromosome.add(copyOfChromosome);
			}
			copyOfChromosome = null;
		}
	}

	private final double getFreeSpace(
			final int indexToMute,
			final IChromosome chromosome) {
		double freeSpace = DAY_DURATION;
		int geneCount = 0;
		int totalCount = 0;
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		int currentNGenes = nGenesIterator.next();
		boolean inGoodPlan = false;

		Gene[] genes = chromosome.getGenes();
		Gene gene;
		for (int i=0; i < genes.length; i++) {
			gene = genes[i];
			if ( !(gene instanceof DoubleGene) ) {
				continue;
			}

			if (geneCount == currentNGenes) {
				// end of an individual plan reached
				if (inGoodPlan) {
					// we were in the plan of the mutated chromosome:
					// we are done.
					return freeSpace;
				}
				// else, we begin a new initial plan
				freeSpace = DAY_DURATION;
				geneCount = 0;
				currentNGenes = nGenesIterator.next();
			}

			if ((i != indexToMute)) {
				freeSpace -= ((DoubleGene) gene).doubleValue();
			}
			else {
				inGoodPlan = true;
			}

			geneCount++;
		}

		return freeSpace;
	}

	private final void mutateBoolean(
			final BooleanGene gene) {
		gene.setAllele(!gene.booleanValue());
	}

	private final void mutateDoubleNonUniform(
			final DoubleGene gene,
			final Double freeSpace) {
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

	private final void mutateDouble(
			final DoubleGene gene,
			final Double freeSpace) {
		gene.setAllele(this.randomGenerator.nextDouble() * freeSpace);
	}

	private final double delta(final int t,final double y) {
		double r = this.randomGenerator.nextDouble();
		double exponant = Math.pow((1d - t/this.NUM_ITER), this.NON_UNIFORMITY_PARAM);

		return (y * (1 - Math.pow(r, exponant)));
	}
}

