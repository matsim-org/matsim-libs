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
import java.util.Random;

import org.apache.log4j.Logger;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;

import org.matsim.core.gbl.MatsimRandom;

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
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPMutation.class);


	private static final long serialVersionUID = 1L;

	private final Double MUTATION_PROB;
	private final int CHROMOSOME_SIZE;

	private final Random randomGenerator;
	private final Configuration jgapConfig;

	public JointPlanOptimizerJGAPMutation(
			Configuration config,
			JointReplanningConfigGroup configGroup,
			int chromosomeSize) {
		this.CHROMOSOME_SIZE = chromosomeSize;
		this.MUTATION_PROB = configGroup.getMutationProbability();

		this.jgapConfig = config;

		this.randomGenerator = MatsimRandom.getLocalInstance();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {
		Gene geneToMute;
		Gene currentGene;
		Gene mutatedGene;
		int geneToMuteIndex;
		Double freeSpace;

		log.debug("mutation operator called to operate");

		for (IChromosome currentChromosome : 
				// no need to check cast (Chromosomes implement IChromosome)
				(List<IChromosome>) a_population.getChromosomes()) {
			if (this.randomGenerator.nextDouble() > this.MUTATION_PROB) {
				//perform mutation
				geneToMuteIndex = this.randomGenerator.nextInt(this.CHROMOSOME_SIZE);
				geneToMute = currentChromosome.getGene(geneToMuteIndex);
				mutatedGene = null;

				if (geneToMute instanceof BooleanGene) {
					mutatedGene = mutateBoolean((BooleanGene) geneToMute);
				}
				else if (geneToMute instanceof DoubleGene) {

					freeSpace = ((DoubleGene) geneToMute).getUpperBound();
					for (int i=0; i < this.CHROMOSOME_SIZE; i++) {
						currentGene = currentChromosome.getGene(i);
						if ((i!=geneToMuteIndex)&&
								(currentGene instanceof DoubleGene)) {
							freeSpace -= ((DoubleGene) currentGene).doubleValue();
						}
					}

					mutatedGene = mutateDouble((DoubleGene) geneToMute, freeSpace);
				}
				a_candidateChromosome.add(mutatedGene);
			}
		}
	}

	private Gene mutateBoolean(BooleanGene gene) {
		Gene mutatedGene = null;

		try {
			 mutatedGene = new BooleanGene(this.jgapConfig, !gene.booleanValue());
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return mutatedGene;
	}

	private Gene mutateDouble(DoubleGene gene, Double freeSpace) {
		// TODO
		Gene mutatedGene = null;

		try {
			 mutatedGene = new DoubleGene(this.jgapConfig, gene.getLowerBound(),
					 gene.getUpperBound());
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return mutatedGene;
	}
}

