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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import java.util.Random;

import org.apache.log4j.Logger;

import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import org.matsim.core.gbl.MatsimRandom;

/**
 * Creates suitable initial random populations.
 * @author thibautd
 */
public class JointPlanOptimizerPopulationFactory {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerPopulationFactory.class);


	private final JointPlanOptimizerJGAPConfiguration jgapConfig;
	private final int populationSize;
	private final int nBooleanGenes;
	private final int nDoubleGenes;
	private final double dayDuration;

	private final RandomGenerator randomGenerator;

	public JointPlanOptimizerPopulationFactory(
			JointPlanOptimizerJGAPConfiguration jgapConfig) {
		boolean dayDurationNotSet = true;
		double dayDurationInit = 0d;
		Gene[] sampleGenes = jgapConfig.getSampleChromosome().getGenes();
		int nBoolInit = 0;
		int nDoubleInit = 0;

		this.jgapConfig = jgapConfig;
		this.populationSize = jgapConfig.getPopulationSize();

		for (Gene currentGene : sampleGenes) {
			if (currentGene instanceof BooleanGene) {
				nBoolInit++;
			} else if (currentGene instanceof DoubleGene) {
				if (dayDurationNotSet) {
					dayDurationInit = ((DoubleGene) currentGene).getUpperBound();
					dayDurationNotSet = false;
				}
				nDoubleInit++;
			}
		}

		this.nBooleanGenes = nBoolInit;
		this.nDoubleGenes = nDoubleInit;
		this.dayDuration = dayDurationInit;

		this.randomGenerator = jgapConfig.getRandomGenerator();
	}

	public Population createRandomInitialPopulation() {
		IChromosome[] chromosomes = new IChromosome[this.populationSize];
		DoubleGene newDoubleGene;
		double[] randomDurations = new double[this.nDoubleGenes + 1];
		double scalingFactor = 0d;

		try {
			for (int i=0; i < this.populationSize; i++) {
				// MUST be initialized here: the Chromosome constructor copies
				// the reference to the array, not the genes it contains.
				Gene[] currentGenes = new Gene[this.nBooleanGenes + this.nDoubleGenes];
				// /////////////////////////////////////////////////////////////
				// initialize the genes randomly
				for (int j=0; j < this.nBooleanGenes; j++) {
					currentGenes[j] = new BooleanGene(this.jgapConfig,
							this.randomGenerator.nextBoolean());
				}

				scalingFactor = 0d;
				for (int j=0; j <= this.nDoubleGenes; j++) {
					randomDurations[j] = this.randomGenerator.nextDouble();
					scalingFactor += randomDurations[j];
				}

				scalingFactor = this.dayDuration / scalingFactor;

				// /////////////////////////////////////////////////////////////
				// scale the total duration (considering also last activity) to
				// one day
				for (int j=0; j < this.nDoubleGenes; j++) {
					newDoubleGene =  new DoubleGene(this.jgapConfig, 0d, this.dayDuration);
					newDoubleGene.setAllele(scalingFactor * randomDurations[j]);

					currentGenes[this.nBooleanGenes + j] = newDoubleGene;
				}

				chromosomes[i] = new JointPlanOptimizerJGAPChromosome(
						this.jgapConfig, currentGenes);
			}

			return new Population(this.jgapConfig, chromosomes);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		//should never get there!
		throw new RuntimeException("GA population has not been initialized");
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

