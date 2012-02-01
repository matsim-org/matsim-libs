/* *********************************************************************** *
 * project: org.matsim.*
 * UpperBoundsContraintsManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import java.util.Iterator;
import java.util.List;

import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;
import org.jgap.RandomGenerator;
import org.matsim.core.utils.collections.Tuple;

/**
 * {@link ConstraintsManager} where he constraints are upper bounds
 * on the sum of consecutive positive double gene values.
 * It can be used to enforce plan duration, or to keep optimised plans
 * synchronised with unoptimised plans.
 * <br>
 * This is useful for encodings based on activity duration.
 *
 * <br>
 * Note that the positiveness of the gene values is not checked.
 *
 * @author thibautd
 */
public class UpperBoundsConstraintsManager implements ConstraintsManager {
	private static final double EPSILON = 1E-7;
	private final Iterable<Integer> nDurationGenes;
	private final Iterable<Double> upperBounds;


	// TODO: extract some info about structure
	// TODO: check positiveness and that number of genes is consistent
	// those both operation would require a sample chrom.
	/**
	 * Initialised an instance.
	 * @param nDurationGenes a list containing the number of genes to consider for
	 * each sum, in the order they appear in the chromosome.
	 * @param upperBounds the upper bounds on the sums whose terms are defined by the
	 * previous list
	 */
	public UpperBoundsConstraintsManager(
			final List<Integer> nDurationGenes,
			final List<Double> upperBounds) {
		if (nDurationGenes.size() != upperBounds.size()) {
			throw new IllegalArgumentException( "incompatible sizes "+nDurationGenes.size()+" and "+upperBounds.size() );
		}

		this.nDurationGenes = nDurationGenes;
		this.upperBounds = upperBounds;
	}

	@Override
	public Tuple<Double, Double> getAllowedRange(
			final IChromosome chromosome,
			final int i) {
		if (i < 0 || i >= chromosome.size()) {
			throw new IllegalArgumentException( "illegal index "+i+"for chromosome of size "+chromosome.size() );
		}
		return new Tuple<Double, Double>(
				0d,
				getFreeSpace( chromosome , i ) );
	}

	/**
	 * @param indexToMute index of the DoubleGene to mute in the Chromosome.
	 * @param chromosome the chromosome to mute.
	 * @return the upper bound on the gene value.
	 */
	private final double getFreeSpace(
			final IChromosome chromosome,
			final int indexToMute) {
		int geneCount = 0;
		Iterator<Integer> nGenesIterator = nDurationGenes.iterator();
		int currentNGenes = nGenesIterator.next();
		Iterator<Double> boundsIterator = upperBounds.iterator();
		double freeSpace = boundsIterator.next();
		boolean inGoodPlan = false;

		Gene[] genes = chromosome.getGenes();
		Gene gene;
		for (int i=0; i < genes.length; i++) {
			gene = genes[i];
			if ( gene instanceof DoubleGene ) {
				if (geneCount == currentNGenes) {
					// end of an individual plan reached
					if (inGoodPlan) {
						// we were in the plan of the mutated chromosome:
						// we are done.
						return freeSpace;
					}
					// else, we begin a new initial plan
					freeSpace = boundsIterator.next();
					geneCount = 0;
					currentNGenes = nGenesIterator.next();
				}

				if ((i != indexToMute)) {
					freeSpace -= ((DoubleGene) gene).doubleValue();
				}
				else {
					inGoodPlan = true;
				}

				geneCount++;
			}
			else if (i == indexToMute) {
				throw new IllegalArgumentException( "gene at index "+i
						+" is an instance of "+gene.getClass()
						+", not a double gene" );
			}
		}

		return freeSpace;
	}


	@Override
	public double getSimpleCrossOverCoef(
			final IChromosome firstMate,
			final IChromosome secondMate,
			final int crossingPoint) {
		Gene[] mate1Genes = firstMate.getGenes();
		Gene[] mate2Genes = secondMate.getGenes();

		if (crossingPoint >= mate1Genes.length) {
			throw new IllegalArgumentException( "crossing point "+crossingPoint+" out of chromosome of size "+mate1Genes.length );
		}

		double mate1PlanDuration = 0d;
		double crossOverSurplus = 0d;
		double currentSurplus;
		double currentEpisodeDuration1;
		double currentEpisodeDuration2;
		double minIndivCoef = Double.POSITIVE_INFINITY;
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		Iterator<Double> boundsIterator = this.upperBounds.iterator();
		int currentNGenes = nGenesIterator.next();
		double currentBound = boundsIterator.next();
		int countGenes = 0;

		// move count gene through the uncrossed part of the plan and
		// initialize the first plan duration.
		for (int i=0; i <  crossingPoint; i++) {
			if ( mate1Genes[i] instanceof DoubleGene ) {
				if (countGenes == currentNGenes) {
					countGenes = 1;
					currentNGenes = nGenesIterator.next();
					currentBound = boundsIterator.next();
					mate1PlanDuration = ((DoubleGene) mate1Genes[i]).doubleValue();
				} else {
					mate1PlanDuration += ((DoubleGene) mate1Genes[i]).doubleValue();
					countGenes++;
				}
			}
		}
	
		for (int i=crossingPoint; i < mate1Genes.length; i++) {
			if ( mate1Genes[i] instanceof DoubleGene ) {
				if (countGenes == currentNGenes) {
					// end of the individual plan reached.
					countGenes = 1;
					currentNGenes = nGenesIterator.next();
					currentBound = boundsIterator.next();
					minIndivCoef =
						Math.min(
							minIndivCoef,
							calculatePlanDurCoef(
								mate1PlanDuration,
								currentBound,
								crossOverSurplus));
					crossOverSurplus = 0d;
					mate1PlanDuration = 0d;
				} else {
					countGenes++;
				}

				currentEpisodeDuration1 = ((DoubleGene) mate1Genes[i]).doubleValue();
				currentEpisodeDuration2 = ((DoubleGene) mate2Genes[i]).doubleValue();
				mate1PlanDuration += currentEpisodeDuration1;

				currentSurplus = currentEpisodeDuration2 - currentEpisodeDuration1;

				crossOverSurplus += currentSurplus;
			}
		}

		// take the last individual plan into account
		minIndivCoef =
			Math.min(
				minIndivCoef,
				calculatePlanDurCoef(
					mate1PlanDuration,
					currentBound,
					crossOverSurplus));

		return Math.min(1d, minIndivCoef);
	}


	/**
	 * Calculates the simple cross-over coef for one individual inequality.
	 * @param mat1Sum the sum of all terms entering the inequality at hand in the first mate
	 * @param upperBound the maximal value the sum can take
	 * @param crossOverSurplus the sum of the differences between the first mate and
	 * the second mate values, for all genes entering the cross-over
	 */
	private static double calculatePlanDurCoef(
			final double mate1Sum,
			final double upperBound,
			final double crossOverSurplus) {
		if (Math.abs( crossOverSurplus ) < EPSILON) {
			return 1d;
		} else {
			double upperLimit = (upperBound - mate1Sum) / crossOverSurplus;
			return Math.max(0d, Math.min(1d, upperLimit));
		}
	}

	@Override
	public Tuple<Double, Double> getSingleCrossOverCoefInterval(
			final IChromosome firstMate,
			final IChromosome secondMate,
			final int crossingPoint) {
		return new Tuple<Double, Double>(
				0d,
				getMaxSingleCrossOverCoef(
					firstMate,
					secondMate,
					crossingPoint ));
	}

	private double getMaxSingleCrossOverCoef(
			final IChromosome firstMate,
			final IChromosome secondMate,
			final int crossingPoint) {
		Gene[] mate1Genes = firstMate.getGenes();
		Gene[] mate2Genes = secondMate.getGenes();
		double mate1PlanDuration = 0d;
		double crossOverSurplus = 0d;
		double currentEpisodeDuration;
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		Iterator<Double> boundsIterator = this.upperBounds.iterator();
		int currentNGenes = nGenesIterator.next();
		double currentBound = boundsIterator.next();
		int countGenes = 0;
		boolean crossingPointIsPast = false;
		double coef;
	
		for (int i=0; i < mate1Genes.length; i++) {
			if ( mate1Genes[i] instanceof DoubleGene ) {
				if (countGenes == currentNGenes) {
					if (!crossingPointIsPast) {
						// end of an individual plan reached.
						countGenes = 0;
						currentNGenes = nGenesIterator.next();
						currentBound = boundsIterator.next();
						mate1PlanDuration = 0d;
					}
					else {
						coef = calculatePlanDurCoef(
								mate1PlanDuration,
								currentBound,
								crossOverSurplus);
						coef = Math.min(1d, coef);
						coef = Math.max(0d, coef);
						return coef;
					}
				}

				currentEpisodeDuration = ((DoubleGene) mate1Genes[i]).doubleValue();
				mate1PlanDuration += currentEpisodeDuration;

				if ( i >= crossingPoint ) {
					crossOverSurplus = ((DoubleGene) mate2Genes[i]).doubleValue() -
						currentEpisodeDuration;
				}
				countGenes++;
			}
			if ( i >= crossingPoint ) {
				crossingPointIsPast = true;
			}
		}

		if (crossingPointIsPast) {
			// if the crossing point was in the last plan
			coef = calculatePlanDurCoef(
					mate1PlanDuration,
					currentBound,
					crossOverSurplus);
			coef = Math.min(1d, coef);
			coef = Math.max(0d, coef);
			return coef;
		}

		throw new RuntimeException("Single cross over coefficient computation failed! "
				+"Crossing point: "+crossingPoint+
				", chromosome size "+mate1Genes.length+
				", crossingPointIsPast: "+crossingPointIsPast);
	}

	@Override
	public boolean respectsConstraints(final IChromosome chromosome) {
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		Iterator<Double> boundsIterator = this.upperBounds.iterator();
		int remainingGenes = nGenesIterator.next();
		double remainingSpace = boundsIterator.next();

		for (Gene gene : chromosome.getGenes()) {
			if (gene instanceof DoubleGene) {
				if (remainingGenes == 0) {
					remainingGenes = nGenesIterator.next();
					remainingSpace = boundsIterator.next();
				}

				remainingGenes--;
				remainingSpace -= ((DoubleGene) gene).doubleValue();

				if (remainingSpace < -EPSILON) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * "Fills" a chromosome. This means that all double genes will be affected
	 * random values, so that the sums are equal to their upper bound.
	 * This is meant for debugging/unit-testing.
	 *
	 * @param chromosome the chromosome to fill
	 */
	public void fillChromosome(
			final IChromosome chromosome) {
		RandomGenerator random = chromosome.getConfiguration().getRandomGenerator();
		Gene[] genes = chromosome.getGenes();
		List<Double> randomDurations = new ArrayList<Double>();
		List<DoubleGene> genesToFill = new ArrayList<DoubleGene>();
		double scalingFactor = 0;
		int countDoubleGenes = 0;

		Iterator<Double> boundsIterator = upperBounds.iterator(); 
		Iterator<Integer> nGenesIterator = nDurationGenes.iterator();
		int sumLength = nGenesIterator.next();
		double bound = boundsIterator.next();
		for (Gene gene : genes) {
			if (gene instanceof DoubleGene) {
				if (countDoubleGenes == sumLength) {
					Iterator<Double> durations = randomDurations.iterator();
					scalingFactor = bound / scalingFactor;

					for (DoubleGene geneToFill : genesToFill) {
						geneToFill.setAllele(scalingFactor * durations.next());
					}

					scalingFactor = 0;
					sumLength = nGenesIterator.next();
					bound = boundsIterator.next();
					randomDurations.clear();
					genesToFill.clear();
					countDoubleGenes = 0;
				}

				double rand = random.nextDouble();
				randomDurations.add( rand );
				scalingFactor += rand;
				genesToFill.add( (DoubleGene) gene );

				countDoubleGenes++;
			}
		}
	
		if (countDoubleGenes != sumLength) {
			throw new RuntimeException( "unexpected! Counting the double genes does not sum to the expected number of genes!" );
		}

		Iterator<Double> durations = randomDurations.iterator();
		scalingFactor = bound / scalingFactor;

		for (DoubleGene geneToFill : genesToFill) {
			geneToFill.setAllele(scalingFactor * durations.next());
		}
	}

	@Override
	public void randomiseChromosome(final IChromosome chromosome) {
		RandomGenerator random = chromosome.getConfiguration().getRandomGenerator();
		Gene[] genes = chromosome.getGenes();
		List<Double> randomDurations = new ArrayList<Double>();
		List<DoubleGene> genesToFill = new ArrayList<DoubleGene>();
		// initialise to a non-0 random value to let some "space"
		double scalingFactor = random.nextDouble();
		int countDoubleGenes = 0;

		Iterator<Double> boundsIterator = upperBounds.iterator(); 
		Iterator<Integer> nGenesIterator = nDurationGenes.iterator();
		int sumLength = nGenesIterator.next();
		double bound = boundsIterator.next();

		for (Gene gene : genes) {
			if (gene instanceof DoubleGene) {
				if (countDoubleGenes == sumLength) {
					Iterator<Double> durations = randomDurations.iterator();
					scalingFactor = bound / scalingFactor;

					for (DoubleGene geneToFill : genesToFill) {
						geneToFill.setAllele(scalingFactor * durations.next());
					}

					scalingFactor = random.nextDouble();
					sumLength = nGenesIterator.next();
					bound = boundsIterator.next();
					randomDurations.clear();
					genesToFill.clear();
					countDoubleGenes = 0;
				}

				double rand = random.nextDouble();
				randomDurations.add( rand );
				scalingFactor += rand;
				genesToFill.add( (DoubleGene) gene );

				countDoubleGenes++;
			}
		}

		if (countDoubleGenes != sumLength) {
			throw new RuntimeException( "unexpected! Counting the double genes does not sum to the expected number of genes!" );
		}

		Iterator<Double> durations = randomDurations.iterator();
		scalingFactor = bound / scalingFactor;

		for (DoubleGene geneToFill : genesToFill) {
			geneToFill.setAllele(scalingFactor * durations.next());
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// test methods
	// /////////////////////////////////////////////////////////////////////////
	/*package*/ double getRemainingFreeSpace(final IChromosome chromosome) {
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		Iterator<Double> boundsIterator = this.upperBounds.iterator();
		int remainingGenes = nGenesIterator.next();
		double remainingSpace = boundsIterator.next();

		for (Gene gene : chromosome.getGenes()) {
			if (gene instanceof DoubleGene) {
				if (remainingGenes == 0) {
					remainingGenes = nGenesIterator.next();
					remainingSpace += boundsIterator.next();
				}

				remainingGenes--;
				remainingSpace -= ((DoubleGene) gene).doubleValue();

			}
		}

		return remainingSpace;
	}
}

