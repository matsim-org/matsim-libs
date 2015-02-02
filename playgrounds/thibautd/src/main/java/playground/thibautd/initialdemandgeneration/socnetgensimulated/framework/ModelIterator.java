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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

	private final List<EvolutionListener> listeners = new ArrayList< >();

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
			thresholds.setResultingClustering( SnaUtils.calcClusteringCoefficient( sn ) );

			for ( EvolutionListener l : listeners ) l.handleNewResult( thresholds );

			memory.add( thresholds );

			if ( isAcceptable( thresholds ) ) return sn;
		}
	}

	public void addListener( final EvolutionListener l ) {
		listeners.add( l );
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

		private final ThresholdsReference getOppositeQuadrant( final ThresholdsReference t ) {
			if ( t == null ) throw new NullPointerException();
			if ( t == bestSouthWest ) return bestNorthEast;
			if ( t == bestSouthEast ) return bestNorthWest;
			if ( t == bestNorthEast ) return bestSouthWest;
			return bestSouthEast;
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

			final boolean accordingToDegree = iteration % 2 == 0;

			final ThresholdsReference best = getBestQuadrant( accordingToDegree );
			final ThresholdsReference opposite = getOppositeQuadrant( best );

			assert best != null;
			assert best.thresholds != null;
			return opposite.thresholds == null ?
				// step-based movement
				moveByStep( best , iteration ) :
				// best cannot be null, because if there is one non-null,
				// it is the best
				new Thresholds(
					(best.thresholds.getPrimaryThreshold() + opposite.thresholds.getPrimaryThreshold()) / 2d,
					(best.thresholds.getSecondaryReduction() + opposite.thresholds.getSecondaryReduction()) / 2d );
		}

		private Thresholds moveByStep(
				final ThresholdsReference best,
				final int iteration ) {
			final double step = iteration * SEARCH_STEP;
			final double primarySign =
				best.thresholds.getResultingAverageDegree() < targetDegree ?
				-1 : 1; // if degree lower, need to decrease threshold
			final double secondarySign =
				best.thresholds.getResultingClustering() < targetClustering ?
				-1 : 1; // if degree lower, need to decrease threshold

			return new Thresholds(
					best.thresholds.getPrimaryThreshold() + primarySign * step,
					Math.max( 0 , best.thresholds.getSecondaryReduction() + secondarySign * step ) );
		}

		private ThresholdsReference getBestQuadrant( final boolean accordingToDegree ) {
			return Collections.min(
					Arrays.asList(
						bestSouthWest,
						bestSouthEast,
						bestNorthEast,
						bestNorthWest ),
					 new Comparator<ThresholdsReference>() {
							@Override
							public int compare( ThresholdsReference o1 , ThresholdsReference o2 ) {
								if ( o1.thresholds == null ) {
									// null > whatever
									return o2.thresholds == null ? 0 : 1;
								}
								if ( o2.thresholds == null ) {
									// o1 is not null
									return -1;
								}

								return accordingToDegree ?
									Double.compare(
										distDegree( o1.thresholds ),
										distDegree( o2.thresholds ) ) :
									Double.compare(
										distClustering( o1.thresholds ),
										distClustering( o2.thresholds ) );
							}
					} );
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

