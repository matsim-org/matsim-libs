/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEndsEncodingConstraintsManager.java
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;
import org.jgap.RandomGenerator;
import org.matsim.core.utils.collections.Tuple;

/**
 * ConstraintManager aimed at being used with plans encoded by activity ends rather
 * than duration (the duration encoding has bad convergence).
 * It requires the double genes considered in this constraints to be consecutive
 * in the chromosome. See the constructor doc for details.
 *
 * @author thibautd
 */
public class ActivityEndsEncodingConstraintsManager implements ConstraintsManager {
	private static final double EPSILON = 1E-7;

	private final double minDuration;
	private final double scenarioDuration;
	private final int firstIndex;
	private final int lastIndex;
	private final List<Integer> lastGenePerPlan;

	private static enum GeneType{ FIRST , LAST , UNIQUE , MIDDLE }

	/**
	 * Initialises an instance.
	 *
	 * @param minDuration the minimum duration of an activity (the minimal difference
	 * between the end of an activity and the end of the next one)
	 * @param scenarioDuration the duration of the scenario. The first activity is
	 * required to end at maximum this duration after midnight; the last one
	 * must start at last this duration after the end of the last act, minus the
	 * minimum duration.
	 * @param firstIndex the index of the first double gene part of the sequence.
	 * No double gene with an index inferior to this one will be considered here.
	 * @param nGenesPerPlan the number of double genes for each plan,
	 * in the order they appear in the chromosome (thus, a collection with a
	 * known iteration order MUST be used). They must be consecutive.
	 * All subsequent double genes (if any) will not be considered.
	 */
	public ActivityEndsEncodingConstraintsManager(
			final double minDuration,
			final double scenarioDuration,
			final int firstIndex,
			final Collection<Integer> nGenesPerPlan) {
		this.minDuration = minDuration;
		this.scenarioDuration = scenarioDuration;
		this.firstIndex = firstIndex;

		List<Integer> lastGenePerPlan = new ArrayList<Integer>( nGenesPerPlan.size() );

		int curr = firstIndex - 1;
		for (int n : nGenesPerPlan) {
			curr += n;
			lastGenePerPlan.add( curr );
		}
		lastIndex = curr;
		this.lastGenePerPlan = Collections.unmodifiableList( lastGenePerPlan );
	}

	@Override
	public Tuple<Double, Double> getAllowedRange(
			final IChromosome chromosome,
			final int index) {
		double low, high;

		Gene[] genes = chromosome.getGenes();

		int[] planBounds = planBounds( index );
		GeneType geneType = getGeneType( index , planBounds );

		switch ( geneType ) {
			case MIDDLE:
				low = geneValue( genes , index - 1 ) + minDuration;
				high = geneValue( genes , index + 1 ) - minDuration;
				break;
			case FIRST:
				low = Math.max( minDuration , geneValue( genes , planBounds[1] ) +minDuration - scenarioDuration );
				high = Math.min( geneValue( genes , index + 1 ) - minDuration , scenarioDuration );
				break;
			case LAST:
				low = geneValue( genes , index - 1 ) + minDuration;
				high = geneValue( genes , planBounds[0] ) + scenarioDuration - minDuration;
				assert low < high : low+" > "+high+"!";
				//assert low > high : "youhou!";
				break;
			case UNIQUE:
				low = minDuration;
				high = scenarioDuration;
				break;
			default:
				throw new RuntimeException( "unexpected value "+geneType );
		}


		return new Tuple<Double, Double>( low , high - low );
	}

	@Override
	public double getSimpleCrossOverCoef(
			final IChromosome firstMate,
			final IChromosome secondMate,
			final int crossingPoint) {
		if ( !isInConstrainedSequence( crossingPoint ) ) {
			// this can happen, and this corresponds to crossing
			// all double values: the constraints remain respected
			return 1d;
		}

		int[] planBounds = planBounds( crossingPoint );

		Gene[] firstMateGenes = firstMate.getGenes();
		Gene[] secondMateGenes = secondMate.getGenes();

		double firstValueCrossingPoint = geneValue( firstMateGenes , crossingPoint );
		double secondValueCrossingPoint = geneValue( secondMateGenes , crossingPoint );

		double firstPlanFirstEndTime =  geneValue( firstMateGenes , planBounds[ 0 ] );
		double firstPlanLastEndTime =  geneValue( firstMateGenes , planBounds[ 1 ] );
		double secondPlanLastEndTime =  geneValue( secondMateGenes , planBounds[ 1 ] );

		double currentMax = 1d;
		// the last act must end before the end of the first the next day:
		if (crossingPoint != planBounds[ 0 ] && firstPlanLastEndTime < secondPlanLastEndTime) {
			currentMax = Math.min( currentMax,
						(firstPlanFirstEndTime - firstPlanLastEndTime + scenarioDuration) /
						(secondPlanLastEndTime - firstPlanLastEndTime));
		}

		currentMax = firstValueCrossingPoint <= secondValueCrossingPoint ?
			currentMax :
			Math.min(
					currentMax,
					// end before the end of the next act
					(minDuration - firstValueCrossingPoint +
					 ( crossingPoint != planBounds[ 0 ] ?
						  geneValue( firstMateGenes , crossingPoint - 1 ) :
						  Math.max(
							  0d,
							  firstPlanLastEndTime - scenarioDuration ) ) ) /
					(secondValueCrossingPoint - firstValueCrossingPoint) );

		return currentMax;

	}

	@Override
	public Tuple<Double, Double> getSingleCrossOverCoefInterval(
			final IChromosome firstMate,
			final IChromosome secondMate,
			final int crossingPoint) {
		int[] planBounds = planBounds( crossingPoint );

		Gene[] firstMateGenes = firstMate.getGenes();
		Gene[] secondMateGenes = secondMate.getGenes();

		double firstValueCrossingPoint = geneValue( firstMateGenes , crossingPoint );
		double secondValueCrossingPoint = geneValue( secondMateGenes , crossingPoint );
		double matesDifference = secondValueCrossingPoint - firstValueCrossingPoint;

		double high = 1d;

		if (matesDifference > EPSILON) {
			double numerator;

			if (crossingPoint == planBounds[ 1 ]) {
				numerator = scenarioDuration + geneValue( firstMateGenes , planBounds[ 0 ] ) - firstValueCrossingPoint;
			}
			else if (crossingPoint == planBounds[ 0 ]) {
				numerator = geneValue( firstMateGenes , crossingPoint + 1 ) - minDuration - firstValueCrossingPoint;
				numerator = Math.min(
						scenarioDuration - firstValueCrossingPoint,
						numerator);
			}
			else {
				numerator = geneValue( firstMateGenes , crossingPoint + 1 ) - minDuration - firstValueCrossingPoint;
			}

			high = numerator / matesDifference;
		}
		else if (matesDifference < -EPSILON) {
			double prevVal = crossingPoint == planBounds[ 0 ] ?
				Math.max( 0d , geneValue( firstMateGenes , planBounds[ 1 ] ) - scenarioDuration ) :
				geneValue( firstMateGenes , crossingPoint - 1 );
			high = (minDuration - firstValueCrossingPoint + prevVal) / matesDifference;
		}

		high = high > 1d ? 1d : high;

		return new Tuple<Double , Double>( 0d , high );
	}

	@Override
	public boolean respectsConstraints(
			final IChromosome chromosome) {
		Gene[] genes = chromosome.getGenes();

		Iterator<Integer> lastIndexIterator = lastGenePerPlan.iterator();
		int currentFirst = firstIndex;

		while (lastIndexIterator.hasNext()) {
			final int currentLast = lastIndexIterator.next();

			final double firstGeneValue = geneValue( genes , currentFirst );
			
			if (firstGeneValue > scenarioDuration + EPSILON) {
				throw new RuntimeException( "first" );
				//return false;
			}

			double lastGeneValue = firstGeneValue;

			for (int i = currentFirst + 1; i <= currentLast; i++) {
				double currentGeneValue = geneValue( genes , i );
				if (currentGeneValue - lastGeneValue < minDuration - EPSILON) {
					throw new RuntimeException( "in" );
					//return false;
				}
				lastGeneValue = currentGeneValue;
			}

			if (lastGeneValue > firstGeneValue + scenarioDuration - minDuration + EPSILON) {
				throw new RuntimeException( "last: "+lastGeneValue+" > "+(firstGeneValue + scenarioDuration - minDuration + EPSILON) );
				//return false;
			}

			currentFirst = currentLast + 1;
		}

		return true;
	}

	@Override
	public void randomiseChromosome(
			final IChromosome chromosome) {
		RandomGenerator random = chromosome.getConfiguration().getRandomGenerator();
		Gene[] genes = chromosome.getGenes();

		Iterator<Integer> lastIndexIterator = lastGenePerPlan.iterator();
		int currentFirst = firstIndex;

		while (lastIndexIterator.hasNext()) {
			int currentLast = lastIndexIterator.next();

			int nGenes = currentLast - currentFirst;
			double[] values = new double[ nGenes ];
			double coef = random.nextDouble();

			for (int i = 0; i < nGenes; i++) {
				values[ i ] = random.nextDouble();
				coef += values[ i ];
			}

			coef = (scenarioDuration - ((nGenes + 1) * minDuration)) / coef;

			double lastEnd = random.nextDouble() * (scenarioDuration - minDuration) + minDuration;
			genes[ currentFirst ].setAllele( lastEnd );

			for (int i = 0; i < nGenes; i++) {
				lastEnd +=  minDuration + coef * values[ i ];
				genes[ currentFirst + 1 + i ].setAllele( lastEnd );
			}

			currentFirst = currentLast + 1;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static GeneType getGeneType(
			final int index,
			final int[] planBounds) {
		if (index == planBounds[ 0 ]) {
			if (index == planBounds[ 1 ]) {
				return GeneType.UNIQUE;
			}
			return GeneType.FIRST;
		}
		else {
			return index == planBounds[ 1 ] ? GeneType.LAST : GeneType.MIDDLE;
		}
	}


	/**
	 * returns an array of two values, the first being the index of the first gene of the plan
	 * (inclusive), the second being the index of the last gene of the plan (inclusive)
	 */
	private int[] planBounds(
			final int index) {
		if (index < firstIndex || index > lastIndex) throw new RuntimeException( index+" is not the index of a double gene" );

		int currentFirstIndex = firstIndex;
		for (int currentLastIndex : lastGenePerPlan) {
			if (index <= currentLastIndex) {
				return new int[]{currentFirstIndex , currentLastIndex};
			}

			currentFirstIndex = currentLastIndex + 1;
		}

		// impossible to get here without bug
		throw new RuntimeException( "uh?" );
	}

	private static double geneValue(
			final Gene[] genes,
			final int index) {
		return ((DoubleGene) genes[ index ]).doubleValue();
	}

	private boolean isInConstrainedSequence(final int index) {
		return index >= firstIndex && index <= lastIndex;
	}
}

