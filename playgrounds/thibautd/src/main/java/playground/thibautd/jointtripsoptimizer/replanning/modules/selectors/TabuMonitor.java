/* *********************************************************************** *
 * project: org.matsim.*
 * TabuMonitor.java
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;
import org.jgap.Population;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Monitors the evolution and manages the tabu penalties for continuous tabu.
 * For discrete tabu, returns the tabu list.
 *
 * @author thibautd
 */
public class TabuMonitor implements Comparator<IChromosome> {

	private final double tabuRadius;
	private final double maxPenalty;
	private final double minImprovement;
	private final int monitoringPeriod;
	private final Configuration jgapConfig;

	private final List<IChromosome> tabuList = new ArrayList<IChromosome>(5);
	private int nextIterationToMonitor;
	private double lastBestScore;

	public TabuMonitor(
			final Configuration jgapConfig,
			final JointReplanningConfigGroup configGroup) {
		this.tabuRadius = 5d*3600d;
		this.maxPenalty = 50d;
		this.nextIterationToMonitor = 1;
		this.monitoringPeriod = 3;
		this.minImprovement = 3d;
		this.jgapConfig = jgapConfig;
	}

	/**
	 * Analyses the population and determines tabu elements.
	 */
	public void monitor(final List<IChromosome> population) {
		IChromosome fittest;
		double bestScore;

		if (this.jgapConfig.getGenerationNr()==this.nextIterationToMonitor) {
			this.nextIterationToMonitor += this.monitoringPeriod;

			fittest = determineFittestChromosome(population);
			bestScore = fittest.getFitnessValue();

			if (bestScore - this.lastBestScore < this.minImprovement) {
				this.tabuList.add(fittest);
			}

			this.lastBestScore = bestScore;
		}
	}

	private IChromosome determineFittestChromosome(
			final List<IChromosome> population) {
		IChromosome fittest = null;
		IChromosome currentChrom;
		double currentFitness = Double.NEGATIVE_INFINITY;

		for (int i=1; i < population.size(); i++) {
			currentChrom = population.get(i);
			if (currentChrom.getFitnessValue() > currentFitness) {
				fittest = currentChrom;
				currentFitness = currentChrom.getFitnessValue();
			}
		}
		return fittest;
	}

	public double getPenalizedFitness(final IChromosome chromosome) {
		double output = chromosome.getFitnessValue();
		double distance;

		for (IChromosome tabuElement : this.tabuList) {
			distance = getDistance(tabuElement, chromosome);
			if (distance < this.tabuRadius) {
				output -= this.maxPenalty * 
					((this.tabuRadius - distance) / this.tabuRadius);
			}
		}

		return output;
	}

	@Override
	//TODO: use memory not to have to compute the penalty at every comparison
	public int compare(
			final IChromosome chrom1,
			final IChromosome chrom2) {
		double penalisedFitness1 = getPenalizedFitness(chrom1);
		double penalisedFitness2 = getPenalizedFitness(chrom2);

		if (penalisedFitness1 < penalisedFitness2) {
			return -1;
		}
		else if (penalisedFitness1 > penalisedFitness2) {
			return 1;
		}
		return 0;
	}

	/**
	 * returns the distance 1 between double parts of the genes.
	 */
	//TODO: make abstract
	private double getDistance(final IChromosome chrom1, final IChromosome chrom2) {
		Gene[] genes1 = chrom1.getGenes();
		Gene[] genes2 = chrom2.getGenes();
		double val1, val2;
		double output = 0d;

		for (int i=0; i < genes1.length; i++) {
			if (genes1[i] instanceof DoubleGene) {
				val1 = ((DoubleGene) genes1[i]).doubleValue();
				val2 = ((DoubleGene) genes2[i]).doubleValue();
				output += Math.abs(val1 - val2);
			}
		}

		return output;
	}

	public List<IChromosome> getTabuList() {
		return this.tabuList;
	}
}

