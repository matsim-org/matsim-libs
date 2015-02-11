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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

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
					memory.createNewThresholds( );

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
		private final Queue<Thresholds> queue = new ArrayDeque< >();

		private Thresholds bestNetSize = null;
		private Thresholds bestClustering = null;

		private double primaryStepSizeDegree = 50;
		private double secondaryStepSizeDegree = 50;

		private double primaryStepSizeClustering = 50;
		private double secondaryStepSizeClustering = 50;

		boolean hadDegreeImprovement = false;
		boolean hadClusteringImprovement = false;

		public ThresholdMemory( final Collection<Thresholds> initial ) {
			this.queue.addAll( initial );
		}

		public void add( final Thresholds t ) {
			if ( bestNetSize == null || distDegree( t ) < distDegree( bestNetSize ) ) {
				log.info( t+" better than best value for net size "+bestNetSize );
				log.info( "replacing value" );
				bestNetSize = t;
				hadDegreeImprovement = true;
			}
			else {
				log.info( t+" not better than best value for net size "+bestNetSize+" => NOT KEPT" );
			}
			
			if (  bestClustering == null || distClustering( t ) < distClustering( bestClustering ) ) {
				log.info( t+" better than best value for clustering "+bestClustering );
				log.info( "replacing value" );
				bestClustering = t;
				hadClusteringImprovement = true;
			}
			else {
				log.info( t+" not better than best value for clustering "+bestClustering+" => NOT KEPT" );
			}
		}

		public Thresholds createNewThresholds() {
			if ( queue.isEmpty() ) fillQueue();
			return queue.remove();
		}

		private void fillQueue() {
			// no improvement means "overshooting": decrease step sizes
			if ( !hadDegreeImprovement ) {
				primaryStepSizeDegree /= 2;
				secondaryStepSizeDegree /= 2;
			}

			if ( !hadClusteringImprovement ) {
				primaryStepSizeClustering /= 2;
				secondaryStepSizeClustering /= 2;
			}

			hadDegreeImprovement = false;
			hadClusteringImprovement = false;

			log.info( "New step Sizes:" );
			log.info( "primary - degree: "+primaryStepSizeDegree );
			log.info( "secondary - degree: "+secondaryStepSizeDegree );
			log.info( "primary - Clustering: "+primaryStepSizeClustering );
			log.info( "secondary - Clustering: "+secondaryStepSizeClustering );


			fillQueueWithChildren(
					bestNetSize,
					bestNetSize.getResultingAverageDegree() > targetDegree ? primaryStepSizeDegree : -primaryStepSizeDegree,
					bestNetSize.getResultingClustering() > targetClustering ? -secondaryStepSizeDegree : secondaryStepSizeDegree );

			if ( bestNetSize != bestClustering ) {
				fillQueueWithChildren(
						bestClustering,
						bestClustering.getResultingAverageDegree() > targetDegree ? primaryStepSizeClustering : -primaryStepSizeClustering,
						bestClustering.getResultingClustering() > targetClustering ? -secondaryStepSizeClustering : secondaryStepSizeClustering );
				fillQueueWithCombinations();
			}
		}

		private void fillQueueWithCombinations() {
			// risk of re-exploring...
			// And when adding, cannot simple modify step size
			//queue.add( new Thresholds( bestNetSize.getPrimaryThreshold() , bestClustering.getSecondaryReduction() ) );
			//queue.add( new Thresholds( bestClustering.getPrimaryThreshold() , bestNetSize.getSecondaryReduction() ) );
		}

		private void fillQueueWithChildren( final Thresholds point , final double stepDegree , final double stepSecondary ) {
			queue.add( moveByStep( point , stepDegree , stepSecondary ) );
			queue.add( moveByStep( point , 0 , stepSecondary ) );
			queue.add( moveByStep( point , stepDegree , 0 ) );
		}

		private Thresholds moveByStep(
				final Thresholds point,
				final double degreeStep,
				final double clusteringStep ) {
			return new Thresholds(
					point.getPrimaryThreshold() + degreeStep,
					point.getSecondaryReduction() + clusteringStep );
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

