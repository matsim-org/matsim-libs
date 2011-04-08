/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPCrossOver.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Cross breeds joint plans.
 * It does the following:
 * - on discrete variables: uniform cross-over.
 * - on continuous variables: GENOCOP-like "arithmetical" cross overs.
 *
 * assumes the following structure for the chromosome: [boolean genes]-[Double genes]
 * @author thibautd
 */
public class JointPlanOptimizerJGAPCrossOver implements GeneticOperator {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPCrossOver.class);

	private static final long serialVersionUID = 1L;

	private static final double EPSILON = 1e-10;

	private final double WHOLE_CO_RATE;
	private final double SIMPLE_CO_RATE;
	private final double SINGLE_CO_RATE;
	private final int N_BOOL;
	private final int N_DOUBLE;
	private final int N_MODE;

	private final List<Integer> nDurationGenes = new ArrayList<Integer>();

	private final double DAY_DURATION;

	private final RandomGenerator randomGenerator;

	public JointPlanOptimizerJGAPCrossOver(
			JointPlanOptimizerJGAPConfiguration config,
			JointReplanningConfigGroup configGroup,
			int numBooleanGenes,
			int numDoubleGenes,
			int numModeGenes,
			List<Integer> nDurationGenes
			) {
		this.WHOLE_CO_RATE = configGroup.getWholeCrossOverProbability();
		this.SIMPLE_CO_RATE = configGroup.getSimpleCrossOverProbability();
		this.SINGLE_CO_RATE = configGroup.getSingleCrossOverProbability();
		this.N_BOOL = numBooleanGenes;
		this.N_DOUBLE = numDoubleGenes;
		this.N_MODE = numModeGenes;
		this.DAY_DURATION = config.getDayDuration();
		this.nDurationGenes.clear();
		this.nDurationGenes.addAll(nDurationGenes);
		this.randomGenerator = config.getRandomGenerator();
	}

	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {
		int populationSize = a_population.size();
		int numOfWholeCo = (int) Math.ceil(this.WHOLE_CO_RATE * populationSize);
		int numOfSimpleCo = (int) Math.ceil(this.SIMPLE_CO_RATE * populationSize);
		int numOfSingleCo = (int) Math.ceil(this.SINGLE_CO_RATE * populationSize);
		int numOfCo = numOfWholeCo + numOfSimpleCo + numOfSingleCo;
		int index1;
		IChromosome mate1;
		int index2;
		IChromosome mate2;

		for (int i=0; i < numOfCo; i++) {
			// draw random parents
			index1 = this.randomGenerator.nextInt(populationSize);
			index2 = this.randomGenerator.nextInt(populationSize);
			mate1 = (IChromosome) a_population.getChromosome(index1).clone();
			mate2 = (IChromosome) a_population.getChromosome(index2).clone();

			doBooleanCrossOver(mate1, mate2);
			doModeCrossOver(mate1, mate2);
			if (i < numOfWholeCo) {
				doDoubleWholeCrossOver(mate1, mate2);
			}
			else if (i < numOfWholeCo + numOfSimpleCo) {
				doDoubleSimpleCrossOver(mate1, mate2);
			}
			else {
				doDoubleSingleCrossOver(mate1, mate2);
			}

			a_candidateChromosome.add(mate1);
			a_candidateChromosome.add(mate2);
		}
	}

	/**
	 * Performs a uniform cross-over on the boolean valued genes.
	 */
	private final void doBooleanCrossOver(IChromosome mate1, IChromosome mate2) {
		boolean value1;
		boolean value2;
		// loop over boolean genes
		for (int i=0; i < this.N_BOOL; i++) {
			value1 = ((BooleanGene) mate1.getGene(i)).booleanValue();
			value2 = ((BooleanGene) mate2.getGene(i)).booleanValue();

			// exchange values with proba O.5
			if (this.randomGenerator.nextInt(2) == 0) {
				mate1.getGene(i).setAllele(value2);
				mate2.getGene(i).setAllele(value1);
			}
		}
	}

	/**
	 * Performs a uniform cross-over on the mode genes.
	 */
	private final void doModeCrossOver(IChromosome mate1, IChromosome mate2) {
		Object value1;
		Object value2;

		// loop over boolean genes
		for (int i=this.N_BOOL + this.N_DOUBLE;
				i < this.N_BOOL + this.N_DOUBLE + this.N_MODE;
				i++) {
			value1 = mate1.getGene(i).getAllele();
			value2 = mate2.getGene(i).getAllele();

			// exchange values with proba O.5
			if (this.randomGenerator.nextInt(2) == 0) {
				mate1.getGene(i).setAllele(value2);
				mate2.getGene(i).setAllele(value1);
			}
		}
	}


	/**
	 * Performs a "GENOCOP-like" "Whole arithmetical cross-over" on the double
	 * valued genes, with a random coefficient.
	 */
	private final void doDoubleWholeCrossOver(IChromosome mate1, IChromosome mate2) {
		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		double randomCoef = this.randomGenerator.nextDouble();

		for (int i=this.N_BOOL; i < this.N_BOOL + this.N_DOUBLE; i++) {
			gene1 = (DoubleGene) mate1.getGene(i);
			gene2 = (DoubleGene) mate2.getGene(i);
			oldValue1 = gene1.doubleValue();
			oldValue2 = gene2.doubleValue();
			
			gene1.setAllele(randomCoef*oldValue1 + (1 - randomCoef)*oldValue2);
			gene2.setAllele(randomCoef*oldValue2 + (1 - randomCoef)*oldValue1);
		}
	}

	/**
	 * Performs a "GENOCOP-like" "Simple arithmetical cross-over" on the double
	 * valued genes.
	 */
	private final void doDoubleSimpleCrossOver(IChromosome mate1, IChromosome mate2) {
		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		int crossingPoint = this.randomGenerator.nextInt(this.N_DOUBLE);
		double crossingCoef1 = SimpleCoCrossingCoef(
				mate1.getGenes(),
				mate2.getGenes(),
				crossingPoint);
		double crossingCoef2 = SimpleCoCrossingCoef(
				mate2.getGenes(),
				mate1.getGenes(),
				crossingPoint);

		for (int i=this.N_BOOL + crossingPoint; i < this.N_BOOL + this.N_DOUBLE; i++) {
			gene1 = (DoubleGene) mate1.getGene(i);
			gene2 = (DoubleGene) mate2.getGene(i);
			oldValue1 = gene1.doubleValue();
			oldValue2 = gene2.doubleValue();
			
			gene1.setAllele((1 - crossingCoef1)*oldValue1 + crossingCoef1*oldValue2);
			gene2.setAllele(crossingCoef2*oldValue1 + (1 - crossingCoef2)*oldValue2);
		}
	}

	/**
	 * computes the "crossing coefficient" for a simple arithmetic CO.
	 * This corresponds to the largest a such that the vector 
	 * (x_1, x_2, ..., x_(i-1), (1 - a)x_i + a*y_i, ..., (1 - a)x_n + a*y_n)
	 * respects the constraints. A value of one corresponds to a classical single
	 * point cross-over.
	 */
	private double SimpleCoCrossingCoef(
			Gene[] mate1Genes,
			Gene[] mate2Genes,
			int crossingPoint) {
		double mate1PlanDuration = 0d;
		double crossOverSurplus = 0d;
		double currentEpisodeDuration;
		double minIndivCoef = 0;
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		int currentNGenes=nGenesIterator.next();
		int countGenes = 1;

		// move count gene through the uncrossed part of the plan and
		// initialize the first plan duration.
		for (int i=this.N_BOOL; i < this.N_BOOL + crossingPoint; i++) {
			if (countGenes > currentNGenes) {
				countGenes = 1;
				currentNGenes = nGenesIterator.next();
				mate1PlanDuration = ((DoubleGene) mate1Genes[i]).doubleValue();
			} else {
				mate1PlanDuration += ((DoubleGene) mate1Genes[i]).doubleValue();
				countGenes++;
			}
		}
	
		for (int i=this.N_BOOL + crossingPoint; i < this.N_BOOL + this.N_DOUBLE; i++) {
			if (countGenes > currentNGenes) {
				// end of the individual plan reached.
				countGenes = 1;
				currentNGenes = nGenesIterator.next();
				minIndivCoef = Math.min(minIndivCoef,
						calculateCoef(mate1PlanDuration, crossOverSurplus));
				crossOverSurplus = 0d;
				mate1PlanDuration = 0d;
			} else {
				countGenes++;
			}

			currentEpisodeDuration = ((DoubleGene) mate1Genes[i]).doubleValue();
			mate1PlanDuration += currentEpisodeDuration;

			crossOverSurplus += ((DoubleGene) mate2Genes[i]).doubleValue() -
				currentEpisodeDuration;
		}

		// take the last individual plan into account
		minIndivCoef = Math.min(minIndivCoef,
			calculateCoef(mate1PlanDuration, crossOverSurplus));

		return Math.max(1d, minIndivCoef);
	}

	private double calculateCoef(double mate1PlanDuration, double crossOverSurplus) {
		if (Math.abs(crossOverSurplus) < EPSILON) {
			return 1d;
		} else {
			double upperLimit = (DAY_DURATION - mate1PlanDuration) / crossOverSurplus;
			return Math.max(1d, upperLimit);
		}
	}

	/**
	 * Performs a "GENOCOP-like" "Single arithmetic cross-over" on the double
	 * valued genes.
	 * each gene is crossed with probability 0.5
	 */
	private final void doDoubleSingleCrossOver(IChromosome mate1, IChromosome mate2) {
		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		double crossingCoef1;
		double crossingCoef2;

		// initialize a list of indices of the double genes, in random order.
		// the random order makes all duration genes having the same "status"
		List<Integer> indicesToCross = new ArrayList<Integer>(this.N_DOUBLE);
		for (int i = this.N_BOOL; i < this.N_BOOL + this.N_DOUBLE; i++) {
			indicesToCross.add(i);
		}
		Collections.shuffle(indicesToCross, (Random) this.randomGenerator);

		for (int crossingPoint : indicesToCross) {
			// swap with probability O.5
			if (this.randomGenerator.nextInt(2) == 1) {
				continue;
			}

			crossingCoef1 = SingleCoCrossingCoef(
					mate1.getGenes(),
					mate2.getGenes(),
					crossingPoint);
			crossingCoef2 = SingleCoCrossingCoef(
					mate2.getGenes(),
					mate1.getGenes(),
					crossingPoint);

			gene1 = (DoubleGene) mate1.getGene(crossingPoint);
			gene2 = (DoubleGene) mate2.getGene(crossingPoint);
			oldValue1 = gene1.doubleValue();
			oldValue2 = gene2.doubleValue();
			
			gene1.setAllele((1 - crossingCoef1)*oldValue1 + crossingCoef1*oldValue2);
			gene2.setAllele(crossingCoef2*oldValue1 + (1 - crossingCoef2)*oldValue2);
		}
	}

	/**
	 * computes the "crossing coefficient" for a simple arithmetic CO.
	 * This corresponds to a random a such that the vectors 
	 * (x_1, x_2, ..., x_(i-1), (1 - a)x_i + a*y_i, x_(i+1), ..., x_n)
	 * and
	 * (y_1, y_2, ..., y_(i-1), (1 - a)y_i + a*x_i, y_(i+1), ..., y_n)
	 * respect the constraints.
	 * (NOT HERE: here, it is the max such that the modified x respects the constraints.
	 */
	private double SingleCoCrossingCoef(
			Gene[] mate1Genes,
			Gene[] mate2Genes,
			int crossingPoint) {
		double mate1PlanDuration = 0d;
		double crossOverSurplus = 0d;
		double currentEpisodeDuration;
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		int currentNGenes=nGenesIterator.next();
		int countGenes = 1;
		boolean crossingPointIsPast = false;
	
		for (int i=this.N_BOOL; i < this.N_BOOL + this.N_DOUBLE; i++) {
			if (countGenes > currentNGenes) {
				if (!crossingPointIsPast) {
					// end of an individual plan reached.
					countGenes = 1;
					currentNGenes = nGenesIterator.next();
					mate1PlanDuration = 0d;
				}
				else {
					return Math.max(1d, calculateCoef(mate1PlanDuration, crossOverSurplus));
				}
			} else {
				countGenes++;
			}

			currentEpisodeDuration = ((DoubleGene) mate1Genes[i]).doubleValue();
			mate1PlanDuration += currentEpisodeDuration;

			if (i==crossingPoint) {
				crossOverSurplus = ((DoubleGene) mate2Genes[i]).doubleValue() -
					currentEpisodeDuration;
				crossingPointIsPast = true;
			}
		}

		if (crossingPointIsPast) {
			// if the crossing point was in the last plan
			return Math.max(1d, calculateCoef(mate1PlanDuration, crossOverSurplus));
		}

		throw new RuntimeException("Single cross over coefficient computation failed!");
	}
}
