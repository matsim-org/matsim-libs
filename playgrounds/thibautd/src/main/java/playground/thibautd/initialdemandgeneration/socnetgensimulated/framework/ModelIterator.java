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
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	private final double precisionClustering;
	private final double precisionDegree;

	private final double initialPrimaryStep;
	private final double initialSecondaryStep;

	// TODO: make adaptive (the closer to the target value,
	// the more precise is should get)
	private double samplingRateClustering = 1;
	private final List<EvolutionListener> listeners = new ArrayList< >();

	public ModelIterator( final SocialNetworkGenerationConfigGroup config ) {
		this.targetClustering = config.getTargetClustering();
		this.targetDegree = config.getTargetDegree();

		listeners.add( new EvolutionLogger() );

		setSamplingRateClustering( config.getSamplingRateForClusteringEstimation() );
		this.precisionClustering = config.getPrecisionClustering();
		this.precisionDegree = config.getPrecisionDegree();

		this.initialPrimaryStep = config.getInitialPrimaryStep();
		this.initialSecondaryStep = config.getInitialSecondaryStep();
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

			final boolean added = memory.add( thresholds );

			for ( EvolutionListener l : listeners ) l.handleNewResult( thresholds , added );

			if ( isAcceptable( thresholds ) ) {
				log.info( thresholds+" fulfills the precision criteria!" );
				return sn;
			}
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
		return distClustering( thresholds ) < precisionClustering &&
			distDegree( thresholds ) < precisionDegree;
	}

	private double distClustering( final Thresholds thresholds ) {
		return Math.abs( targetClustering -  thresholds.getResultingClustering() );
	}

	private double distDegree( final Thresholds thresholds ) {
		return Math.abs( targetDegree -  thresholds.getResultingAverageDegree() );
	}

	private class ThresholdMemory {
		private final Set<Thresholds> tabu = new HashSet<Thresholds>();
		private final Deque<Thresholds> stack = new ArrayDeque< >();

		// we want an improvement of one precision unit to result in the same effect
		// on mono-objective version.
		private final double factorClustering = precisionDegree / precisionClustering;
		private final double exponent = 5;

		private Thresholds best = null;

		private double primaryStepSize = initialPrimaryStep;
		private double secondaryStepSize = initialSecondaryStep;

		boolean hadDegreeImprovement = false;
		boolean hadClusteringImprovement = false;

		public ThresholdMemory( final Collection<Thresholds> initial ) {
			this.stack.addAll( initial );
		}

		public boolean add( final Thresholds t ) {

			if ( best == null || isBetter( t ) ) {
				hadDegreeImprovement = best != null && distDegree( t ) < distDegree( best );
				hadClusteringImprovement = best != null && distClustering( t ) < distClustering( best );

				log.info( t + " better than best value " + best );
				log.info( "replacing value" );
				best = t;

				fillQueue();

				return true;
			}

			log.info( t + " not better than best value " + best + " => NOT KEPT" );
			return false;
		}

		private boolean isBetter( Thresholds t ) {
			return function( t ) < function( best );
		}

		private double function( Thresholds t ) {
			return Math.pow( distDegree( t ) , exponent ) +
				Math.pow( factorClustering * distClustering( t ) , exponent );
		}

		public Thresholds createNewThresholds() {
			if ( stack.isEmpty() ) fillQueue();
			return stack.removeFirst();
		}

		private void fillQueue() {
			// no improvement means "overshooting": decrease step sizes
			if ( !hadDegreeImprovement ) {
				primaryStepSize /= 2;
			}
			// if improvement, continue harder in the same direction
			//else primaryStepSize *= 1.5;

			if ( !hadClusteringImprovement ) {
				secondaryStepSize /= 2;
			}
			//else secondaryStepSize *= 1.5;

			hadClusteringImprovement = false;
			hadDegreeImprovement = false;

			log.info( "New step Sizes:" );
			log.info( "primary : "+primaryStepSize );
			log.info( "secondary : "+secondaryStepSize );

			fillQueueWithChildren(
					best,
					primaryStepSize,
					secondaryStepSize );
		}

		private void fillQueueWithChildren( final Thresholds point , final double stepDegree , final double stepSecondary ) {
			//addToStack( moveByStep( point , stepDegree , stepSecondary ) );
			addToStack( moveByStep( point , 0 , stepSecondary ) );
			addToStack( moveByStep( point , stepDegree , 0 ) );
			addToStack( moveByStep( point , 0 , -stepSecondary ) );
			addToStack( moveByStep( point , -stepDegree , 0 ) );
		}
		
		private void addToStack( final Thresholds t ) {
			if ( !tabu.add( t ) ) return;
			stack.addFirst( t );
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
		public void handleNewResult( Thresholds t , boolean keptInMemory );
	}

	private static class EvolutionLogger implements EvolutionListener {
		@Override
		public void handleNewResult( final Thresholds t , final boolean keptInMemory ) {
			log.info( "generated network for "+t );
		}
	}
}

