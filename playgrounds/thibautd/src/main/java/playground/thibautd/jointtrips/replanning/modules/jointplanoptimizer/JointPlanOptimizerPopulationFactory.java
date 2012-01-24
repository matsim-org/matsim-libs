/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerPopulationFactory.java
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer;

import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.ConstraintsManager;

/**
 * Creates suitable initial random populations.
 * @author thibautd
 */
public class JointPlanOptimizerPopulationFactory {
	private final JointPlanOptimizerJGAPConfiguration jgapConfig;

	public JointPlanOptimizerPopulationFactory(
			final JointPlanOptimizerJGAPConfiguration jgapConfig) {
		this.jgapConfig = jgapConfig;
	}

	public Population createRandomInitialPopulation() {
		JointPlanOptimizerJGAPChromosome sampleChrom = (JointPlanOptimizerJGAPChromosome)
			this.jgapConfig.getSampleChromosome();
		int populationSize = this.jgapConfig.getPopulationSize();
		IChromosome[] chromosomes = new IChromosome[populationSize];

		try {
			for (int i=0; i < populationSize; i++) {
				chromosomes[i] = createRandomChromosome();
			}
			if (chromosomes[0].equals(chromosomes[1])) {
				// would be better in a unit test...
				throw new RuntimeException( "random initial genotype creation created identical chromosomes!" );
			}

			return new Population(this.jgapConfig, chromosomes);
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException( e );
		}
	}

	public JointPlanOptimizerJGAPChromosome createRandomChromosome() {
		JointPlanOptimizerJGAPChromosome sampleChrom = (JointPlanOptimizerJGAPChromosome)
			this.jgapConfig.getSampleChromosome();
		JointPlanOptimizerJGAPChromosome newChrom = (JointPlanOptimizerJGAPChromosome) sampleChrom.clone();

		RandomGenerator random = jgapConfig.getRandomGenerator();
		ConstraintsManager constraints = jgapConfig.getConstraintsManager();

		for (Gene gene : newChrom.getGenes()) {
			if ( !(gene instanceof DoubleGene) ) {
				gene.setToRandomValue( random );
			}
		}

		constraints.randomiseChromosome( newChrom );
		return newChrom;
	}

	public Genotype createRandomInitialGenotype() {
		try {
			return new Genotype(this.jgapConfig, this.createRandomInitialPopulation());
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		//should never get there!
		throw new RuntimeException("GA population has not been initialized");
	}
}

