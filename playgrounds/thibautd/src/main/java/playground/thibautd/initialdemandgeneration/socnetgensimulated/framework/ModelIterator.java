/* *********************************************************************** *
 * project: org.matsim.*
 * ModelIterator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.SnaUtils;
import playground.thibautd.socnetsim.population.SocialNetwork;

/**
 * @author thibautd
 */
public class ModelIterator {
	private static final Logger log =
		Logger.getLogger(ModelIterator.class);


	private final double targetClustering;
	private final double targetDegree;

	private static final double PRECISION_CLUSTERING = 1E-2;
	private static final double PRECISION_DEGREE = 1E-1;

	private static final double SEARCH_STEP = 10;

	// TODO: make adaptive (the closer to the target value,
	// the more precise is should get)
	private double samplingRateClustering = 1;
	private final List<EvolutionListener> listeners = new ArrayList< >();

	public ModelIterator( final SocialNetworkGenerationConfigGroup config ) {
		this( config.getTargetClustering() , config.getTargetDegree() );
		setSamplingRateClustering( config.getSamplingRateForClusteringEstimation() );
	}

	public ModelIterator( double targetClustering , double targetDegree ) {
		this.targetClustering = targetClustering;
		this.targetDegree = targetDegree;
		listeners.add( new EvolutionLogger() );
	}

	public SocialNetwork iterateModelToTarget(
			final ModelRunner runner,
			final Collection<Thresholds> initialThresholds ) {
		final ThresholdMemory memory = new ThresholdMemory( initialThresholds );

		for ( int iter=1; true; iter++ ) {
			log.info( "Iteration # "+iter );
			final Thresholds thresholds =
					memory.createNewThresholds( iter );

			log.info( "generate network for "+thresholds );
			final SocialNetwork sn = runner.runModel( thresholds );

			thresholds.setResultingAverageDegree( SnaUtils.calcAveragePersonalNetworkSize( sn ) );
			thresholds.setResultingClustering( SnaUtils.estimateClusteringCoefficient( samplingRateClustering , sn ) );

			for ( EvolutionListener l : listeners ) l.handleNewResult( thresholds );

			memory.add( thresholds );

			if ( isAcceptable( thresholds ) ) return sn;
		}
	}

	public void addListener( final EvolutionListener l ) {
		listeners.add( l );
	}

	public void setSamplingRateClustering( final double rate ) {
		if ( rate < 0 || rate > 1 ) throw new IllegalArgumentException( rate+" is not in [0;1]" );
		this.samplingRateClustering = rate;
	}

	private boolean isAcceptable(
			final Thresholds thresholds ) {
		return distClustering( thresholds ) < PRECISION_CLUSTERING &&
			distDegree( thresholds ) < PRECISION_DEGREE;
	}

	private double distClustering( final Thresholds thresholds ) {
		return Math.abs( targetClustering -  thresholds.getResultingClustering() );
	}

	private double distDegree( final Thresholds thresholds ) {
		return Math.abs( targetDegree -  thresholds.getResultingAverageDegree() );
	}

	private class ThresholdMemory {
		private final Iterator<Thresholds> initialThresholds;

		private final ThresholdsReference bestSouthWest = new ThresholdsReference( "southWest" );
		private final ThresholdsReference bestSouthEast = new ThresholdsReference( "southEast" );
		private final ThresholdsReference bestNorthEast = new ThresholdsReference( "northEast" );
		private final ThresholdsReference bestNorthWest = new ThresholdsReference( "northWest" );

		public ThresholdMemory( final Iterable<Thresholds> initial ) {
			this.initialThresholds = initial.iterator();
		}

		public void add( final Thresholds t ) {
			final ThresholdsReference ref = getQuadrant( t );

			if ( ref.thresholds == null ) {
				log.info("not yet a value for quadrant "+ref.name+", putting "+t );
				ref.thresholds = t;
			}
			else if ( distDegree( t ) <= distDegree( ref.thresholds ) &&
					distClustering( t ) <= distClustering( ref.thresholds ) ) {
				log.info( t+" strictly better than "+ref.thresholds+" for quadrant "+ref.name );
				log.info( "replacing value" );
				ref.thresholds = t;
			}
			else if ( distDegree( t ) < distDegree( ref.thresholds ) ||
					distClustering( t ) < distClustering( ref.thresholds ) ) {
				log.info( t+" partly better than "+ref.thresholds+" for quadrant "+ref.name );
				log.info( "replacing value" );
				ref.thresholds = t;
			}
			else {
				log.warn( "DROPPING VALUE "+t+" for quadrant "+ref.name+" with value "+ref.thresholds+"!" );
			}

		}

		private final ThresholdsReference getQuadrant( final Thresholds t ) {
			final boolean isSouth = t.getResultingAverageDegree() < targetDegree;
			final boolean isWest = t.getResultingClustering() < targetClustering;
			return getQuadrant( isSouth , isWest );
		}

		private final Thresholds getAttemptToUnnullify( final int iteration ) {
			if ( null == bestSouthWest.thresholds && null != bestNorthWest.thresholds ) {
				return moveByStep( bestNorthWest , -1 , 0 , iteration );
			}
			if ( null == bestSouthEast.thresholds && null != bestNorthEast.thresholds ) {
				return moveByStep( bestNorthEast , -1 , 0 , iteration );
			}
			if ( null == bestNorthEast.thresholds && null != bestSouthEast.thresholds ) {
				return moveByStep( bestSouthEast , 1 , 0 , iteration );
			}
			if ( null == bestNorthWest.thresholds && null != bestSouthWest.thresholds ) {
				return moveByStep( bestSouthWest , 1 , 0 , iteration );
			}
			return null;
		}

		private final ThresholdsReference getQuadrant( final boolean isSouth , final boolean isWest ) {
			if ( isSouth && isWest ) return bestSouthWest;
			if ( isSouth && !isWest ) return bestSouthEast;
			if ( !isSouth && !isWest ) return bestNorthEast;
			if ( !isSouth && isWest ) return bestNorthWest;

			throw new RuntimeException( "impossible to get there!" );
		}

		public Thresholds createNewThresholds( final int iteration ) {
			if ( initialThresholds.hasNext() ) return initialThresholds.next();

			final Thresholds unnullifier = getAttemptToUnnullify( iteration );

			return unnullifier != null ?
				// step-based movement
				unnullifier :
				// no null quadrant
				combineAll();
		}

		private Thresholds combineAll() {
			return new Thresholds(
					(bestSouthWest.thresholds.getPrimaryThreshold() +
						 bestSouthEast.thresholds.getPrimaryThreshold() +
						 bestNorthEast.thresholds.getPrimaryThreshold() +
						 bestNorthWest.thresholds.getPrimaryThreshold() ) / 4d,
					(bestSouthWest.thresholds.getSecondaryReduction() +
						 bestSouthEast.thresholds.getSecondaryReduction() +
						 bestNorthEast.thresholds.getSecondaryReduction() +
						 bestNorthWest.thresholds.getSecondaryReduction() ) / 4d );
		}

		private Thresholds moveByStep(
				final ThresholdsReference start,
				final int degreeSign,
				final int clusteringSign,
				final int iteration ) {
			final double step = Math.pow( 2 , iteration ) * SEARCH_STEP;

			return new Thresholds(
					start.thresholds.getPrimaryThreshold() + degreeSign * step,
					// move secondary reduction in opposite direction as clustering
					Math.max( 0 , start.thresholds.getSecondaryReduction() - clusteringSign * step ) );
		}
	}

	private static class ThresholdsReference {
		public final String name;
		public Thresholds thresholds = null;

		public ThresholdsReference(
				final String name ) {
			this.name = name;
		}
	}

	public static interface EvolutionListener {
		public void handleNewResult( Thresholds t );
	}

	private static class EvolutionLogger implements EvolutionListener {
		@Override
		public void handleNewResult( final Thresholds t ) {
			log.info( "generated network for "+t );
		}
	}
}

