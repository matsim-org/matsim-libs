/* *********************************************************************** *
 * project: org.matsim.*
 * TabuAndEvolutionMonitor.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.selectors;

import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.Population;
import org.jgap.audit.IEvolutionMonitor;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerPopulationFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * @author thibautd
 */
public class TabuAndEvolutionMonitor implements IEvolutionMonitor, TabuMonitor {
	private static final Logger log =
		Logger.getLogger(TabuAndEvolutionMonitor.class);


	private int numberOfUnmonitoredIterations;
	private int nextIterToMonitor = numberOfUnmonitoredIterations;
	private final int monitoringPeriod;
	private final double minImprovement;
	private final int numBool;
	private final int maxNumberOfTabuElems;
	private final int maxIterations;

	private int indexInTabuList = 0;
	private double lastBestFitness = Double.NEGATIVE_INFINITY;
	private double allTimeBestFitness = Double.NEGATIVE_INFINITY;

	private final boolean[][] tabuSequences;
	private final JointPlanOptimizerPopulationFactory populationFactory;

	private final Configuration jgapConfig;

	private boolean quitNow = false;

	public TabuAndEvolutionMonitor(
			final JointPlanOptimizerJGAPConfiguration jgapConfig,
			final JointReplanningConfigGroup configGroup) {
		this.monitoringPeriod = configGroup.getMonitoringPeriod();
		this.numberOfUnmonitoredIterations = configGroup.getMinIterations();
		this.maxIterations = configGroup.getMaxIterations();
		// TODO: multiply by the number of members of the clique
		this.minImprovement = configGroup.getMinImprovement();
		this.jgapConfig = jgapConfig;

		this.numBool = jgapConfig.getNumJointEpisodes();
		this.maxNumberOfTabuElems = (int) Math.pow(2, this.numBool) - 1;
		this.tabuSequences = new boolean[this.maxNumberOfTabuElems][this.numBool];

		this.populationFactory = new JointPlanOptimizerPopulationFactory(jgapConfig);
	}


	/*
	 * =========================================================================
	 * TabuMonitor methods and related helpers
	 * =========================================================================
	 */
	/**
	 * Checks if chromosomes are to be added to the tabu list, and does so if
	 * yes.
	 */
	@Override
	public void updateTabu(final Population population) {
		if (this.jgapConfig.getGenerationNr() == nextIterToMonitor) {
			IChromosome fittest = population.determineFittestChromosome();
			double fitness = fittest.getFitnessValue();
			int step = this.monitoringPeriod;

			if (fitness - this.lastBestFitness < this.minImprovement) {
				if (fitness < this.allTimeBestFitness) {
					// new toggle schematas are worst than tabu ones: quit.
					this.quitNow = true;
					return;
				}
				else {
					this.allTimeBestFitness = fitness;
				}

				if (this.indexInTabuList < this.maxNumberOfTabuElems) {
					boolean[] newTabuSequence = new boolean[this.numBool];
					for (int i=0; i < this.numBool; i++) {
						newTabuSequence[i] =
							((BooleanGene) fittest.getGene(i)).booleanValue();
					}

					// add the new sequence only if it is not already tabu (possible
					// only with a "penalty" strategy, where tabu chromosomes are kept)
					if (!isTabu(newTabuSequence)) {
						this.tabuSequences[this.indexInTabuList] = newTabuSequence;
						this.indexInTabuList++;

						// reinitialize population
						population.clear();
						population.addChromosomes(populationFactory.createRandomInitialPopulation());
						fitness = Double.NEGATIVE_INFINITY;
						step = this.numberOfUnmonitoredIterations;
					}
					else {
						log.warn("new tabu sequence already tabu: this is unexpected!");
						quitNow = true;
					}
				}
				else {
					//not enough improvement and tabu list full: we quit
					this.quitNow = true;
				}
			}

			this.nextIterToMonitor += step;
			this.lastBestFitness = fitness;
		}
	}

	/**
	 * Randomly mutates tabu chromosomes until they are not tabu.
	 * Could be done smarter (by not generating tabu values)
	 */
	@Override
	public void correctTabu(final IChromosome chromosome) {
		boolean[] toCorrect = new boolean[this.numBool];
		int index;

		for (int i=0; i < this.numBool; i++) {
			toCorrect[i] = ((BooleanGene) chromosome.getGene(i)).booleanValue();
		}

		if (isTabu(toCorrect)) {
			RandomGenerator generator = this.jgapConfig.getRandomGenerator();
			// randomly mutate until the chromosome isn't tabu
			while (isTabu(toCorrect)) {
				index = generator.nextInt(this.numBool);
				toCorrect[index] = !toCorrect[index];
			}

			// update chromosome values
			for (int i=0; i < this.numBool; i++) {
				chromosome.getGene(i).setAllele(toCorrect[i]);
			}
			chromosome.setFitnessValueDirectly(JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE);
		}
		
	}

	/**
	 * @return true if a matching tabu element is found.
	 */
	private boolean isTabu(final boolean[] toggleValues) {
		boolean[] currentTabuValues;
		boolean areEqual;

		for (int i=0; i < this.indexInTabuList; i++) {
			currentTabuValues = this.tabuSequences[i];

			areEqual = true;
			for (int j=0; j < this.numBool; j++) {
				if (currentTabuValues[j] != toggleValues[j]) {
					areEqual = false;
					break;
				}
			}

			if (areEqual) {
				return true;
			}
		}

		// we didn't found any matching tabu element
		return false;
	}

	/*
	 * =========================================================================
	 * IEvolutionMonitor methods and related helpers
	 * =========================================================================
	 */
	@Override
	public boolean nextCycle(
			final Population population,
			final  List<String> messages) {
		int iteration = this.jgapConfig.getGenerationNr();

		if (iteration == this.maxIterations) {
			return false;
		}

		return !quitNow;
	}

	@Override
	public void start(final Configuration config) {
		if (this.jgapConfig != config) {
			throw new IllegalArgumentException("the monitor must be ran "+
					"with the config used to initialize it");
		}
	}
}

