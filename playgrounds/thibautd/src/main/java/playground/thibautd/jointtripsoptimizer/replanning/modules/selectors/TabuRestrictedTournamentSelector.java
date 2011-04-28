/* *********************************************************************** *
 * project: org.matsim.*
 * TabuRestrictedTournamentSelector.java
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

import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;

/**
 * Quick implementation of a RTS with tabu capabilities on toggle genes.
 * @author thibautd
 */
public class TabuRestrictedTournamentSelector extends RestrictedTournamentSelector {
	//cannot use delegation:  add protected
	private static final long serialVersionUID = 1L;

	private int nextIterToMonitor = 1;
	private final int monitoringPeriod = 10;
	private final double minImprovement = 3d;
	private final int numBool;
	private final int maxNumberOfTabuElems;

	private int indexInTabuList = 0;
	private double lastBestFitness = Double.NEGATIVE_INFINITY;

	private final RandomGenerator generator;
	private final boolean[][] tabuSequences;

	public TabuRestrictedTournamentSelector(
			final JointPlanOptimizerJGAPConfiguration jgapConfig,
			final ChromosomeDistanceComparator distanceComparator
			) throws InvalidConfigurationException {
		super(jgapConfig, distanceComparator);

		this.numBool = jgapConfig.getNumJointEpisodes();
		this.maxNumberOfTabuElems = (int) Math.pow(2, this.numBool) - 1;
		this.tabuSequences = new boolean[this.maxNumberOfTabuElems][this.numBool];

		this.generator = jgapConfig.getRandomGenerator();
	}

	@Override
	protected void selectChromosomes(
			final int nToSelect,
			final Population nextGeneration) {
		super.selectChromosomes(nToSelect, nextGeneration);
		updateTabu(nextGeneration);
	}

	@Override
	protected void add(final IChromosome chromosome) {
		correctTabu(chromosome);
		super.add(chromosome);
	}

	/**
	 * Checks if chromosomes are to be added to the tabu list, and does so if
	 * yes.
	 */
	private void updateTabu(final Population population) {
		if (super.getConfiguration().getGenerationNr() == nextIterToMonitor) {
			IChromosome fittest = population.determineFittestChromosome();
			double fitness = fittest.getFitnessValue();

			if ((fitness - this.lastBestFitness < this.minImprovement) &&
					(this.indexInTabuList < this.maxNumberOfTabuElems)) {
				for (int i=0; i < this.numBool; i++) {
					this.tabuSequences[this.indexInTabuList][i] =
						((BooleanGene) fittest.getGene(i)).booleanValue();
				}
				this.indexInTabuList++;
			}

			this.nextIterToMonitor += this.monitoringPeriod;
			this.lastBestFitness = fitness;
		}
	}

	/**
	 * randomly mutates tabu chromosomes until they are not tabu.
	 * Could be done smarter (by not generating tabu values)
	 */
	private void correctTabu(final IChromosome chromosome) {
		boolean[] toCorrect = new boolean[this.numBool];
		int index;

		for (int i=0; i < this.numBool; i++) {
			toCorrect[i] = ((BooleanGene) chromosome.getGene(i)).booleanValue();
		}

		// randomly mutate until the chromosome isn't tabu
		while (isTabu(toCorrect)) {
			index = this.generator.nextInt(this.numBool);
			toCorrect[index] = !toCorrect[index];
		}

		// update chromosome values
		for (int i=0; i < this.numBool; i++) {
			chromosome.getGene(i).setAllele(toCorrect[i]);
		}
	}

	/**
	 * @return true if a matching tabu element is found.
	 */
	private boolean isTabu(boolean[] toggleValues) {
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
}
