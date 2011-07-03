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

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPModeGene;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Class mutating random members from the previous generation.
 * The type of mutation differs depending on the gene:
 * <ul>
 * <li> "toggle" gene: the value is changed
 * <li> duration gene: GENOCOP (Michalewicz and Janikow, 1996) like "non-uniform
 *  mutation" (preserves the inequality constraints) or "uniform" (the probability
 *  of choosing one or the other is set in the config file).
 *  <li> mode gene: the list is shuffled
 * </ul>
 *
 * @author thibautd
 */
public class JointPlanOptimizerJGAPMutation implements GeneticOperator {
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
	private final boolean inPlace;
	private final double nonUniformProb;

	public JointPlanOptimizerJGAPMutation(
			final JointPlanOptimizerJGAPConfiguration config,
			final JointReplanningConfigGroup configGroup,
			final int chromosomeSize,
			final List<Integer> nDurationGenes) {
		this.CHROMOSOME_SIZE = chromosomeSize;
		this.MUTATION_PROB = configGroup.getMutationProbability();
		this.NUM_ITER = configGroup.getMaxIterations();
		this.NON_UNIFORMITY_PARAM = configGroup.getMutationNonUniformity();
		this.DAY_DURATION = config.getDayDuration();
		this.inPlace = configGroup.getInPlaceMutation();
		this.nonUniformProb = configGroup.getNonUniformMutationProbability();

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
			final List a_candidateChromosomes) {
		if (inPlace) {
			operateInPlace(a_population, a_candidateChromosomes);
		}
		else {
			operateOnPreviousGen(a_population, a_candidateChromosomes);
		}
	}

	/**
	 * Randomly mutates the offsprings of the previously applied operators
	 * (classical mutation).
	 */
	public void operateInPlace(
			final Population a_population,
			final List a_candidateChromosome
			) {
		int populationSize = a_population.size();
		int nOffsprings = a_candidateChromosome.size();
		Gene geneToMute;
		double freeSpace;
		IChromosome currentChromosome;
		//IChromosome copyOfChromosome = null;

		for (int j= populationSize; j < nOffsprings; j++) {
			currentChromosome = a_population.getChromosome(j);
			Collections.shuffle(this.geneIndices, (Random) this.randomGenerator);

			//for each gene in the chromosome:
			for (int i : this.geneIndices) {
				if (this.randomGenerator.nextDouble() < this.MUTATION_PROB) {
					//perform mutation

					geneToMute = currentChromosome.getGene(i);

					if (geneToMute instanceof BooleanGene) {
						mutateBoolean((BooleanGene) geneToMute);
					}
					else if (geneToMute instanceof DoubleGene) {

						freeSpace = getFreeSpace(
								i,
								currentChromosome);

						if (this.randomGenerator.nextDouble() < this.nonUniformProb) {
							mutateDoubleNonUniform((DoubleGene) geneToMute, freeSpace);
						}
						else {
							mutateDouble((DoubleGene) geneToMute, freeSpace);
						}
					}
					else if (geneToMute instanceof JointPlanOptimizerJGAPModeGene) {
						geneToMute.setToRandomValue(this.randomGenerator);
					}
				}
			}
		}
	}

	/**
	 * Mutates copies of the chromosomes of the previous generation ("jgap-like"
	 * mutation)
	 */
	public void operateOnPreviousGen(
			final Population a_population,
			final List a_candidateChromosome
			) {
		int populationSize = a_population.size();
		Gene geneToMute;
		double freeSpace;
		IChromosome currentChromosome;
		IChromosome copyOfChromosome = null;

		for (int j=0; j < populationSize; j++) {
			currentChromosome = a_population.getChromosome(j);
			Collections.shuffle(this.geneIndices, (Random) this.randomGenerator);

			//for each gene in the chromosome:
			for (int i : this.geneIndices) {
				if (this.randomGenerator.nextDouble() < this.MUTATION_PROB) {
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
								i,
								copyOfChromosome);

						if (this.randomGenerator.nextDouble() < this.nonUniformProb) {
							mutateDoubleNonUniform((DoubleGene) geneToMute, freeSpace);
						}
						else {
							mutateDouble((DoubleGene) geneToMute, freeSpace);
						}
					}
					else if (geneToMute instanceof JointPlanOptimizerJGAPModeGene) {
						geneToMute.setToRandomValue(this.randomGenerator);
					}
				}
			}
			copyOfChromosome = null;
		}
	}

	/**
	 * @param indexToMute index of the DoubleGene to mute in the Chromosome.
	 * @param chromosome the chromosome to mute.
	 * @return the upper bound on the gene value.
	 */
	protected final double getFreeSpace(
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
		double lowerBound = 0;
		double upperBound = freeSpace;
		int iter = this.jgapConfig.getGenerationNr();

		if (this.randomGenerator.nextInt(2) == 0) {
			value += delta(iter, upperBound - value);
		} else {
			value -= delta(iter, value - lowerBound);
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

