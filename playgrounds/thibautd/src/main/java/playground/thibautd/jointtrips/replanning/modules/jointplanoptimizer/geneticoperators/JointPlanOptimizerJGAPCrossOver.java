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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators;

import java.util.List;

import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.Population;
import org.jgap.RandomGenerator;
import org.jgap.impl.DoubleGene;

import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.configuration.JointPlanOptimizerJGAPConfiguration;

/**
 * Cross breeds joint plans.
 * It does the following:
 * <ul>
 * <li> on discrete variables: uniform cross-over.
 * <li> on continuous variables: GENOCOP-like "arithmetical" cross overs.
 * </ul>
 *
 * @author thibautd
 */
public class JointPlanOptimizerJGAPCrossOver implements GeneticOperator {
	private static final long serialVersionUID = 1L;

	private static final double EPSILON = 1e-10;
	private static final double WHOLE_COEF = 0.25;

	private final double wholeCoRate;
	private final double simpleCoRate;
	private final double singleCoRate;
	private final double discreteCoRate;

	private final ConstraintsManager constraintsManager;

	private final RandomGenerator randomGenerator;

	/**
	 * Constructor for use with static rates
	 */
	public JointPlanOptimizerJGAPCrossOver(
			final JointPlanOptimizerJGAPConfiguration config,
			final JointReplanningConfigGroup configGroup,
			final ConstraintsManager constraintsManager) {
		this.wholeCoRate = configGroup.getWholeCrossOverProbability();
		this.simpleCoRate = configGroup.getSimpleCrossOverProbability();
		this.singleCoRate = configGroup.getSingleCrossOverProbability();
		this.discreteCoRate = configGroup.getDiscreteCrossOverProbability();
		this.randomGenerator = config.getRandomGenerator();
		this.constraintsManager = constraintsManager;
	}

	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome) {
		int populationSize = a_population.size();
		int numOfWholeCo;
		int numOfSimpleCo;
		int numOfSingleCo;
		int numOfDiscreteCo;

		numOfWholeCo = getNumberOfOperations(this.wholeCoRate, populationSize);
		numOfSimpleCo = getNumberOfOperations(this.simpleCoRate, populationSize);
		numOfSingleCo = getNumberOfOperations(this.singleCoRate, populationSize);
		numOfDiscreteCo = getNumberOfOperations(this.discreteCoRate, populationSize);

		int numOfCo = numOfWholeCo + numOfSimpleCo + numOfSingleCo + numOfDiscreteCo;
		int chromLength = a_population.getChromosome( 0 ).size();
		int index1;
		IChromosome parent1;
		IChromosome mate1;
		int index2;
		IChromosome parent2;
		IChromosome mate2;
		int crossingPoint;

		for (int i=0; i < numOfCo; i++) {
			// draw random parents
			index1 = this.randomGenerator.nextInt(populationSize);
			index2 = this.randomGenerator.nextInt(populationSize);
			parent1 = a_population.getChromosome(index1);
			parent2 = a_population.getChromosome(index2);
			mate1 = (IChromosome) parent1.clone();
			mate2 = (IChromosome) parent2.clone();

			crossingPoint = randomGenerator.nextInt( chromLength );

			boolean somethingWasDoneDouble = false;
			boolean somethingWasDoneDiscrete =
				doNonDoubleCrossOver(mate1, mate2, crossingPoint);
			if (i < numOfWholeCo) {
				somethingWasDoneDouble = doDoubleWholeCrossOver(mate1, mate2);
			}
			else if (i < numOfWholeCo + numOfSimpleCo) {
				somethingWasDoneDouble = doDoubleSimpleCrossOver(mate1, mate2, crossingPoint);
			}
			else if (i < numOfWholeCo + numOfSimpleCo + numOfSingleCo) {
				somethingWasDoneDouble = doDoubleSingleCrossOver(mate1, mate2, crossingPoint);
			}
			// else: discrete only CO: nothing to do.

			if (somethingWasDoneDiscrete || somethingWasDoneDouble) {
				a_candidateChromosome.add(mate1);
				a_candidateChromosome.add(mate2);
			}
		}
	}

	private int getNumberOfOperations(final double rate, final double populationSize) {
		// always perform at least one operation of each CO,
		// except if explicitly requested that no operation is performed
		return rate > EPSILON ? (int) Math.ceil(rate * populationSize) : 0;
	}

	/**
	 * performs standard CO on non-double genes
	 */
	private static boolean doNonDoubleCrossOver(
			final IChromosome mate1,
			final IChromosome mate2,
			final int crossingPoint) {
		boolean somethingWasDone = false;
		Object value1;
		Object value2;

		Gene[] mate1Genes = mate1.getGenes();
		Gene[] mate2Genes = mate2.getGenes();

		for (int i= crossingPoint; i < mate1Genes.length; i++) {
			if ( !(mate1Genes[i] instanceof DoubleGene) ) {
				somethingWasDone = true;
				value1 = mate1Genes[i].getAllele();
				value2 = mate2Genes[i].getAllele();

				mate1Genes[i].setAllele(value2);
				mate2Genes[i].setAllele(value1);
			}
		}

		return somethingWasDone;
	}

	/**
	 * Performs a "GENOCOP-like" "Whole arithmetical cross-over" on the double
	 * valued genes, with a random coefficient.
	 */
	private static boolean doDoubleWholeCrossOver(
			final IChromosome mate1,
			final IChromosome mate2) {
		boolean somethingWasDone = false;

		Gene[] mate1Genes = mate1.getGenes();
		Gene[] mate2Genes = mate2.getGenes();

		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		//double randomCoef = this.randomGenerator.nextDouble();
		double randomCoef = WHOLE_COEF;

		for (int i=0; i < mate1Genes.length; i++) {
			if (mate1Genes[i] instanceof DoubleGene) {
				somethingWasDone = true;
				gene1 = (DoubleGene) mate1Genes[i];
				gene2 = (DoubleGene) mate2Genes[i];
				oldValue1 = gene1.doubleValue();
				oldValue2 = gene2.doubleValue();
				
				gene1.setAllele(randomCoef*oldValue1 + (1 - randomCoef)*oldValue2);
				gene2.setAllele(randomCoef*oldValue2 + (1 - randomCoef)*oldValue1);
			}
		}

		return somethingWasDone;
	}

	/**
	 * Performs a "GENOCOP-like" "Simple arithmetical cross-over" on the double
	 * valued genes.
	 */
	private boolean doDoubleSimpleCrossOver(
			final IChromosome mate1,
			final IChromosome mate2,
			int crossingPoint) {
		boolean somethingWasDone = false;

		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		double crossingCoef1 =
			constraintsManager.getSimpleCrossOverCoef(
				mate1,
				mate2,
				crossingPoint);
		double crossingCoef2 =
			constraintsManager.getSimpleCrossOverCoef(
				mate2,
				mate1,
				crossingPoint);

		//only compute "useful" values: more efficient and avoids cumulating
		//rouding errors
		boolean cross1 = (crossingCoef1 > EPSILON);
		boolean cross2 = (crossingCoef2 > EPSILON);

		if ( cross1 || cross2 ) {
			Gene[] mate1Genes = mate1.getGenes();
			Gene[] mate2Genes = mate2.getGenes();

			for (int i= crossingPoint; i < mate1Genes.length; i++) {
				if ( mate1Genes[i] instanceof DoubleGene ) {
					somethingWasDone = true;
					gene1 = (DoubleGene) mate1Genes[i];
					gene2 = (DoubleGene) mate2Genes[i];

					oldValue1 = gene1.doubleValue();
					oldValue2 = gene2.doubleValue();
					
					if (cross1) {
						gene1.setAllele(
								(1d - crossingCoef1)*oldValue1 +
								crossingCoef1*oldValue2);
					}
					if (cross2) {
						gene2.setAllele(
								crossingCoef2*oldValue1 +
								(1d - crossingCoef2)*oldValue2);
					}
				}
			}
		}

		return somethingWasDone;
	}

	/**
	 * Performs a "GENOCOP-like" "Single arithmetic cross-over" on the double
	 * valued genes.
	 */
	private boolean doDoubleSingleCrossOver(
			final IChromosome mate1,
			final IChromosome mate2,
			final int crossingPoint) {
		if ( !(mate1.getGene( crossingPoint ) instanceof DoubleGene) ) {
			return false;
		}

		boolean somethingWasDone = false;

		Tuple<Double, Double> tuple1 = constraintsManager.getSingleCrossOverCoefInterval(
				mate1,
				mate2,
				crossingPoint);
		Tuple<Double, Double> tuple2 = constraintsManager.getSingleCrossOverCoefInterval(
				mate2,
				mate1,
				crossingPoint);
		double random = randomGenerator.nextDouble();
		double crossingCoef = Math.min(
				tuple1.getFirst() + random * tuple1.getSecond(),
				tuple2.getFirst() + random * tuple2.getSecond() );

		if (crossingCoef > EPSILON) {
			somethingWasDone = true;

			DoubleGene gene1 = (DoubleGene) mate1.getGene(crossingPoint);
			DoubleGene gene2 = (DoubleGene) mate2.getGene(crossingPoint);
			double oldValue1 = gene1.doubleValue();
			double oldValue2 = gene2.doubleValue();
		
			gene1.setAllele(
					(1 - crossingCoef) * oldValue1 +
					crossingCoef * oldValue2);

			gene2.setAllele(
					crossingCoef * oldValue1 +
					(1 - crossingCoef) * oldValue2);
		}

		return somethingWasDone;
	}
}
