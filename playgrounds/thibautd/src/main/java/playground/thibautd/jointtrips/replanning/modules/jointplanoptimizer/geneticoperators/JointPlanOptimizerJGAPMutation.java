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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators;

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
import org.jgap.Population;
import org.jgap.RandomGenerator;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;

import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPModeGene;

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

	private final int NUM_ITER;
	private final double NON_UNIFORMITY_PARAM;

	private final ConstraintsManager constraintsManager;
	private final RandomGenerator randomGenerator;
	private final Configuration jgapConfig;
	private final boolean inPlace;
	private final double nonUniformProb;

	public JointPlanOptimizerJGAPMutation(
			final JointPlanOptimizerJGAPConfiguration config,
			final JointReplanningConfigGroup configGroup,
			final ConstraintsManager constraintsManager) {
		this.MUTATION_PROB = configGroup.getMutationProbability();
		this.NUM_ITER = configGroup.getMaxIterations();
		this.NON_UNIFORMITY_PARAM = configGroup.getMutationNonUniformity();
		this.inPlace = configGroup.getInPlaceMutation();
		this.nonUniformProb = configGroup.getNonUniformMutationProbability();

		this.constraintsManager = constraintsManager;
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
	private void operateInPlace(
			final Population a_population,
			final List a_candidateChromosomes
			) {
		int populationSize = jgapConfig.getPopulationSize();
		int nOffsprings = a_candidateChromosomes.size();
		Gene geneToMute;
		Tuple<Double, Double> allowedRange;
		IChromosome currentChromosome;

		// Kind of ugly, but this is the way to do it: the 
		// "candidate chromosomes" are in fact pop.getChromosomes()!
		for (int j= populationSize; j < nOffsprings; j++) {
			currentChromosome = (IChromosome) a_candidateChromosomes.get(j);

			//for each gene in the chromosome:
			for (int i = 0; i < currentChromosome.size(); i++) {
				if (this.randomGenerator.nextDouble() < this.MUTATION_PROB) {
					//perform mutation
					geneToMute = currentChromosome.getGene(i);

					if (geneToMute instanceof BooleanGene) {
						mutateBoolean((BooleanGene) geneToMute);
					}
					else if (geneToMute instanceof DoubleGene) {
						allowedRange = constraintsManager.getAllowedRange( currentChromosome , i );

						if (this.randomGenerator.nextDouble() < this.nonUniformProb) {
							mutateDoubleNonUniform((DoubleGene) geneToMute, allowedRange);
						}
						else {
							mutateDouble((DoubleGene) geneToMute, allowedRange);
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
	private void operateOnPreviousGen(
			final Population a_population,
			final List a_candidateChromosome) {
		// use the fixed population size. otherwise, newly created chromosomes
		// would be used as well.
		int populationSize = jgapConfig.getPopulationSize();
		Gene geneToMute;
		Tuple<Double, Double> allowedRange;
		IChromosome currentChromosome;
		IChromosome copyOfChromosome = null;

		// we cannot iterate over the chromosomes list,
		// as it also contains the newly created chromosomes!
		for (int j=0; j < populationSize; j++) {
			currentChromosome = a_population.getChromosome(j);

			//for each gene in the chromosome:
			for (int i = 0; i < currentChromosome.size(); i++) {
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
						allowedRange = constraintsManager.getAllowedRange( copyOfChromosome , i );

						if (this.randomGenerator.nextDouble() < this.nonUniformProb) {
							mutateDoubleNonUniform((DoubleGene) geneToMute, allowedRange);
						}
						else {
							mutateDouble((DoubleGene) geneToMute, allowedRange);
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

	private static final void mutateBoolean(
			final BooleanGene gene) {
		gene.setAllele(!gene.booleanValue());
	}

	private final void mutateDoubleNonUniform(
			final DoubleGene gene,
			final Tuple<Double, Double> allowedRange) {
		// GENOCOP (Michalewicz, Janikow, 1996) like "non uniform" mutation.
		double value = gene.doubleValue();
		double lowerBound = allowedRange.getFirst();
		double upperBound = lowerBound + allowedRange.getSecond();
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
			final Tuple<Double, Double> allowedRange) {
		gene.setAllele(allowedRange.getFirst() + this.randomGenerator.nextDouble() * allowedRange.getSecond());
	}

	private final double delta(
			final int t,
			final double y) {
		double r = this.randomGenerator.nextDouble();
		double exponant = Math.pow((1d - ((double) t)/this.NUM_ITER), this.NON_UNIFORMITY_PARAM);

		return (y * (1 - Math.pow(r, exponant)));
	}
}

