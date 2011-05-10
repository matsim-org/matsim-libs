/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPEnhancedSpx.java
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPModeGene;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Multi-parent "Simplex cross-over", with boolean CO.
 * modified JointPlanOptimizerJGAPSpx: refactoring to do.
 * @author thibautd
 */
public class JointPlanOptimizerJGAPEnhancedSpx implements GeneticOperator {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPSpx.class);

	public static final long serialVersionUID = 1L;

	private static final double EPSILON = 1E-6;

	private final double expansionRate;
	private final int numberOfParents;
	private final int numberOfOffsprings;
	private final int numberOfDoubleGenes;
	private final int numberOfBooleanGenes;
	private final double coRate;
	private final List<Integer> nDurationGenesPerIndiv;
	private final double dayDuration = 24*3600;
	private final int numberOfHillClimbingSteps = 1;

	private final RandomGenerator generator;
	private final Comparator<IChromosome> comparator = new FitnessValueComparator();

	private double actualExpansionRate;
	private final double[] currentCoordinates;

	public JointPlanOptimizerJGAPEnhancedSpx(
			final JointPlanOptimizerJGAPConfiguration config,
			final JointReplanningConfigGroup configGroup,
			final int numBooleanGenes,
			final int numDoubleGenes,
			final List<Integer> nDurationGenesPerIndiv) {
		this.numberOfParents = numDoubleGenes + 1;
		this.expansionRate = Math.sqrt(this.numberOfParents + 1);
		this.numberOfOffsprings = configGroup.getSPXOffspringRate() * 
			this.numberOfParents;
		this.coRate = configGroup.getSPXProbability();

		this.numberOfDoubleGenes = numDoubleGenes;
		this.numberOfBooleanGenes = numBooleanGenes;
		this.nDurationGenesPerIndiv = nDurationGenesPerIndiv;

		this.currentCoordinates = new double[this.numberOfParents];

		this.generator = config.getRandomGenerator();
	}

	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome) {
		int populationSize = a_population.size();

		List<IChromosome> fathers;
		double[][] fatherValues;
		double[] centerOfMass;
		double[][] edges;
		double[] variablePart;
		double[] offspring;
		List<IChromosome> workingList = new ArrayList<IChromosome>(
				this.numberOfParents + this.numberOfOffsprings);

		int generatedOffsprings;
		for (int i=0;
				i < getNumberOfOperations(this.coRate, populationSize); 
				i += this.numberOfOffsprings) {
			// get "initial" fathers randomly from the population
			fathers = getFathers(a_population);

			generatedOffsprings = 0;
			for (int j=0; j < numberOfHillClimbingSteps; j++) {
				// generate the simplex
				fatherValues = getFatherValues(fathers);
				centerOfMass = getCenterOfMass(fatherValues);
				edges = getSimplexEdges(centerOfMass, fatherValues);

				// sample from the simplex
				while (generatedOffsprings < this.numberOfOffsprings) {
					variablePart = getVariablePart(edges);
					offspring = getOffspring(edges, variablePart);
					
					if (isAcceptable(offspring)) {
						retainValue(offspring, fathers, workingList);
						generatedOffsprings++;
					}
					else {
						// should be moved to a test case: there is no point in checking
						// something that mustn't occur during the runs.
						throw new RuntimeException("unacceptable moves cannot be "+
								"generated!");
					}
				}

				//prepare next step
				if (j < numberOfHillClimbingSteps - 1) {
					generatedOffsprings = 0;
					workingList.addAll(fathers);
					Collections.sort(workingList, this.comparator);
					fathers.clear();
					fathers.addAll(workingList.subList(0,this.numberOfParents));
					workingList.clear();
				}
			}
		}

		a_candidateChromosome.addAll(workingList);
	}

	/**
	 * get the required number of father chromosomes from the population.
	 * The chromosomes are drawn without replacement, to ensure that the fathers
	 * are different.
	 */
	private List<IChromosome> getFathers(final Population a_population) {
		if (a_population.size() < this.numberOfParents) {
			throw new IllegalArgumentException("the population size must be "+
					"greater to the number of double genes to work with SPX");
		}

		List<IChromosome> output = new ArrayList<IChromosome>(this.numberOfParents);
		int popSize = a_population.size();
		List<Integer> alreadyChoosen = new ArrayList<Integer>(this.numberOfParents);
		int randomIndex;

		for (int i=0; i < this.numberOfParents;) {
			randomIndex = this.generator.nextInt(popSize);

			if (!alreadyChoosen.contains(randomIndex)) {
				output.add(a_population.getChromosome(randomIndex));

				alreadyChoosen.add(randomIndex);
				i++;
			}
		}

		return output;
	}

	/**
	 * Get the numerical values of the father real-coded genes.
	 * 
	 * @return a bi dimensionnal array X: X_ij is the value of the jth double gene
	 * of father i.
	 */
	private double[][] getFatherValues(final List<IChromosome> fathers) {
		Gene[] currentGenes;
		double[][] output = new double[fathers.size()][this.numberOfDoubleGenes];

		for (int i=0; i < fathers.size(); i++) {
			currentGenes = fathers.get(i).getGenes();
			for (int j=0; j < this.numberOfDoubleGenes; j++) {
				output[i][j] = 
					((DoubleGene) currentGenes[this.numberOfBooleanGenes + j])
					.doubleValue();
			}
		}

		return output;
	}

	/**
	 * Get the center of mass of the simplex defined by the father values.
	 */
	private double[] getCenterOfMass(final double[][] fatherValues) {
		double[] sum = fatherValues[0];

		for (int i=1; i < this.numberOfParents; i++) {
			sum = addVector(sum, fatherValues[i]);
		}

		return scalarVectorProduct(1d / this.numberOfParents, sum);
	}

	/**
	 * get the edges of the simplex (named Y in tsutsui and goldberg 2002)
	 */
	private double[][] getSimplexEdges(
			final double[] centerOfMass,
			final double[][] fatherValues) {
		double[][] output = new double[this.numberOfParents][this.numberOfDoubleGenes];
		double[] vectorInConstruction;

		this.actualExpansionRate = getActualExpansionRate(
				centerOfMass,
				fatherValues);

		for (int i=0; i < this.numberOfParents; i++) {
			vectorInConstruction = substractVector(
					fatherValues[i],
					centerOfMass);
			vectorInConstruction = scalarVectorProduct(
					this.actualExpansionRate,
					vectorInConstruction);
			output[i] = addVector(centerOfMass, vectorInConstruction);
		}

		return output;
	}

	/**
	 * Returns the expansion rate to use.
	 */
	private double getActualExpansionRate(
			final double[] centerOfMass,
			final double[][] fatherValues) {
		double output = Double.POSITIVE_INFINITY;

		for (int i=0; i < this.numberOfParents; i++) {
			output = Math.min(
					getConstrainedExpansionRate(
						centerOfMass,
						fatherValues[i]),
					output);
		}

		return output;

	}

	/**
	 * Returns the expansion rate for the edge corresponding to the given parent,
	 * such that this edge lies in the feasible space.
	 */
	private double getConstrainedExpansionRate(
			final double[] centerOfMass,
			final double[] parent) {
		double upperBound = Double.POSITIVE_INFINITY;
		double currentValue;

		// positivity of durations:
		for (int i=0; i < this.numberOfDoubleGenes; i++) {
			currentValue = parent[i] - centerOfMass[i];

			if (currentValue < -EPSILON) {
				currentValue = -centerOfMass[i] / currentValue;
				upperBound = Math.min(upperBound, currentValue);
			}
		}

		if (upperBound < 1d) {
			log.error("expansion < 1 for act duration: "+upperBound);
		}

		// plan duration
		upperBound = Math.min(
				upperBound,
				getUpperBoundPlanDurations(centerOfMass, parent));

		return Math.min(upperBound, this.expansionRate);
	}

	private double getUpperBoundPlanDurations(
			final double[] centerOfMass,
			final double[] father) {
		double output = Double.POSITIVE_INFINITY;
		double centerOfMassDur = 0d;
		double currentDuration = 0d;
		double denom;
		Iterator<Integer> nIndivGenes = this.nDurationGenesPerIndiv.iterator();
		int nGenes = nIndivGenes.next();
		
		int count=0;
		for (int i=0; i < this.numberOfDoubleGenes; i++) {
			if (count == nGenes) {
				// compute upper bound
				denom = currentDuration - centerOfMassDur;
				if ((dayDuration - currentDuration) < -EPSILON) {
					log.error("1 -> invalid father duration "+(dayDuration - currentDuration));
				}

				if (denom > EPSILON) {
					output = Math.min(
							output,
							(dayDuration - centerOfMassDur) / denom);
				}

				// reinitialize plan data
				count=0;
				currentDuration = 0d;
				centerOfMassDur = 0d;
				nGenes = nIndivGenes.next();
			}
			currentDuration += father[i];
			centerOfMassDur += centerOfMass[i];
			count++;
		}

		// treat the last plan
		denom = currentDuration - centerOfMassDur;
		if ((dayDuration - currentDuration) < -EPSILON) {
			log.error("2 -> invalid father duration, exces: "+(currentDuration - dayDuration));
		}
		if (denom > EPSILON) {
			output = Math.min(
					output,
					(dayDuration - centerOfMassDur) / denom);
		}

		if (output < 1d) {
			if (output < 1d - EPSILON) {
				// significant error
				log.error("expansion < 1 for plan duration: "+output);
			}
			else {
				// rounding error: don't worry
				output = 1d;
			}
		}
		return output;
	}

	/**
	 * get the "variable" part of the new offspring (named C in tsutsui and goldberg 2002)
	 */
	private double[] getVariablePart(
			final double[][] simplexEdges) {
		double[] output = new double[this.numberOfDoubleGenes];
		double[] randomCoefs = new double[this.numberOfParents];
		double[] vectorInConstruction;

		//initialize the output to the null vector
		for (int i=0; i < this.numberOfDoubleGenes; i++) {
			output[i] = 0d;
		}

		//randomCoefs[0] = 0d;

		//construct the output such that it corresponds to a random point in the
		//simplex
		for (int i=1; i < this.numberOfParents; i++) {
			randomCoefs[i-1] = this.generator.nextDouble();
			randomCoefs[i-1] = Math.pow(randomCoefs[i-1], 1d / i);

			vectorInConstruction = substractVector(
					simplexEdges[i-1],
					simplexEdges[i]);
			vectorInConstruction = addVector(
					vectorInConstruction,
					output);

			output = scalarVectorProduct(randomCoefs[i], vectorInConstruction);
		}

		randomCoefs[this.numberOfParents - 1] = 1d;

		retainCoordinates(randomCoefs);

		return output;
	}

	/**
	 * remember barycentric coordinates, to use with discrete values afterwards.
	 */
	private void retainCoordinates(double[] randomCoefs) {
		double accumulation = this.actualExpansionRate;
		double toAdd = (1d / this.numberOfParents) * (1 - this.actualExpansionRate);

		for (int i = this.numberOfParents - 1; i > 0; i--) {
			accumulation *= randomCoefs[i];
			this.currentCoordinates[i] = 
				(accumulation * (1 - randomCoefs[i-1])) + toAdd;
		}

		this.currentCoordinates[0] = accumulation * randomCoefs[0] + toAdd;
	}

	private double[] getOffspring(
			final double[][] simplexEdges, 
			final double[] variablePart) {
		return addVector(simplexEdges[this.numberOfParents - 1], variablePart);
	}

	/**
	 * checks of the offspring satisfies the constraints
	 */
	private boolean isAcceptable(final double[] offspring) {
		double currentDuration = 0d;
		Iterator<Integer> nIndivGenes = this.nDurationGenesPerIndiv.iterator();
		int nGenes = nIndivGenes.next();
		
		int count=0;
		for (int i=0; i < this.numberOfDoubleGenes; i++) {
			if (count == nGenes) {
				count=0;
				currentDuration = 0d;
				nGenes = nIndivGenes.next();
			}
			currentDuration += offspring[i];
			count++;
			if (currentDuration > dayDuration + EPSILON) {
				log.error("plan duration excedent: "+(currentDuration - dayDuration));
				return false;
			}
		}
		return true;
	}

	/**
	 * add the chromosome to the candidates for selection.
	 * discrete values are taken randomly from one of the parents.
	 */
	private void retainValue(
			final double[] offspring,
			final List<IChromosome> fathers,
			final List a_candidateChromosome) {
		//get a random father to include the generated value in
		IChromosome newChrom = (IChromosome)
			fathers.get(this.generator.nextInt(this.numberOfParents)).clone();
		Gene[] genes = newChrom.getGenes();

		for (int i=0; i < this.numberOfBooleanGenes; i++) {
			genes[i].setAllele(getBooleanAllele(i, fathers));
		}

		// set the double values to the "crossed" ones
		for (int i=0; i < this.numberOfDoubleGenes; i++) {
			((DoubleGene) genes[this.numberOfBooleanGenes + i]).setAllele(
					offspring[i]);
		}

		for (int i=this.numberOfBooleanGenes + this.numberOfDoubleGenes; i < genes.length; i++) {
			genes[i] = getModeGene(i, fathers);
		}

		//add to candidates
		a_candidateChromosome.add(newChrom);
	}

	private boolean getBooleanAllele(
			final int geneIndex,
			final List<IChromosome> fathers) {
		double prob = 0d;
		IChromosome father;

		//double sum = 0d;
		for (int i = 0; i < this.numberOfParents; i++) {
			father = fathers.get(i);
			if (((BooleanGene) father.getGene(geneIndex)).booleanValue()) {
				prob += this.currentCoordinates[i];
			}
			//sum +=  this.currentCoordinates[i];
		}

		//System.out.println(sum);

		return this.generator.nextDouble() < prob;
	}

	/**
	 * Draws a parental mode value according to the position in the simplex,
	 * in a "Roulette Wheel" fashion.
	 */
	private Gene getModeGene(
			final int geneIndex,
			final List<IChromosome> fathers) {
		int index = 0;
		double choice = this.generator.nextDouble();
		double accumulation = this.currentCoordinates[0];

		for (int i=0; i < this.numberOfParents; i++) {
			if (choice < accumulation) {
				index = i;
				break;
			}
			try {
				accumulation += this.currentCoordinates[i];
			} catch (ArrayIndexOutOfBoundsException e) {
				// can occur if choice near to one and sum of coordinates
				// slightly under one due to rounding error
				if (choice - accumulation < EPSILON) {
					log.debug("Mode gene choice had to be forced. Rounding error"+
							"can may this happen, but too many of this message "+
							"may indicate some bug.");
					index = i;
				}
				else {
					throw new RuntimeException("Mode gene could not be set properly.");
				}
			}
		}

		try {
			return new JointPlanOptimizerJGAPModeGene(
					(JointPlanOptimizerJGAPModeGene)
					fathers.get(index).getGene(geneIndex));
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * ========================================================================
	 * various helpers
	 * ========================================================================
	 */
	private int getNumberOfOperations(final double rate, final double populationSize) {
		// always perform at least one operation of each CO
		return Math.max(1, (int) Math.ceil(rate * populationSize));
	}

	private double[] addVector(final double[] v1, final double[] v2) {
		int size = v1.length;
		if (v2.length != size) {
			throw new IllegalArgumentException("cannot sum vectors of different lengths");
		}
		double[] output = new double[size];

		for (int i=0; i < size; i++) {
			output[i] = v1[i] + v2[i];
		}

		return output;
	}

	private double[] substractVector(final double[] v1, final double[] v2) {
		int size = v1.length;
		if (v2.length != size) {
			throw new IllegalArgumentException("cannot substract vectors of different lengths");
		}
		double[] output = new double[size];

		for (int i=0; i < size; i++) {
			output[i] = v1[i] - v2[i];
		}

		return output;
	}

	private double[] scalarVectorProduct(final double scal, final double[] v) {
		double[] output = new double[v.length];

		for (int i=0; i < v.length; i++) {
			output[i] = scal * v[i];
		}

		return output;
	}

	private double sumArrayElements(final double[] array) {
		double output = 0d;

		for (int i=0; i < array.length; i++) {
			output += array[i];
		}

		return output;
	}

	/**
	 * comparator aimed at classing chromosomes in ascending order: if f(C1) > f(C2),
	 * compare(C1, C2) = -1.
	 */
	private class FitnessValueComparator implements Comparator<IChromosome> {
		public FitnessValueComparator() {}

		public int compare(final IChromosome chrom1, final IChromosome chrom2) {
			double fit1 = chrom1.getFitnessValue();
			double fit2 = chrom2.getFitnessValue();
			if (fit1 > fit2) {
				return -1;
			}
			else if (fit1 < fit2) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}
}

