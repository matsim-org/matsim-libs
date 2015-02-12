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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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
			final Thresholds initialThresholds ) {
		final ThresholdMemory memory = new ThresholdMemory( initialThresholds );

		for ( int iter=1; true; iter++ ) {
			log.info( "Iteration # "+iter );
			final long start = System.currentTimeMillis();
			final Thresholds thresholds =
					memory.createNewThresholds( );

			log.info( "generate network for "+thresholds );
			final SocialNetwork sn = runner.runModel( thresholds );

			thresholds.setResultingAverageDegree( SnaUtils.calcAveragePersonalNetworkSize( sn ) );
			thresholds.setResultingClustering( estimateClustering( sn ) );

			memory.add( thresholds );

			log.info( "Iteration # "+iter+" took "+(System.currentTimeMillis() - start)+" ms" );
			if ( isAcceptable( thresholds ) ) {
				log.info( thresholds+" fulfills the precision criteria!" );
				return sn;
			}
		}
	}

	private double estimateClustering( final SocialNetwork sn ) {
		final double estimate = SnaUtils.estimateClusteringCoefficient( samplingRateClustering , sn );

		return Math.abs( targetClustering - estimate ) > 10 * precisionClustering ? estimate : SnaUtils.calcClusteringCoefficient( sn );
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

	// TODO: write moves to file, to be able to plot tree
	private class ThresholdMemory {
		private final Set<Thresholds> tabu = new HashSet<Thresholds>();
		private final Queue<Move> queue =
			new PriorityQueue<Move>(
					10,
					 new Comparator<Move>() {
							@Override
							public int compare( Move o1 , Move o2 ) {
								return Double.compare( function( o2.getParent() ) , function( o1.getParent() ) );
							}
						} );

		// we want an improvement of one precision unit to result in the same effect
		// on mono-objective version.
		private final double factorClustering = precisionDegree / precisionClustering;

		private final double exponent = 4;
		private final double contractionFactor = 2;
		private final double expansionFactor = 1;

		private Move lastMove = null;

		private Thresholds initial;

		public ThresholdMemory( final Thresholds initial ) {
			this.initial = initial;
		}

		public boolean add( final Thresholds t ) {
			if ( lastMove != null && t != lastMove.getChild() ) throw new IllegalArgumentException();

			//final boolean hadDegreeImprovement = lastMove == null || distDegree( t ) < distDegree( lastMove.parent );
			//final boolean hadClusteringImprovement = lastMove == null || distClustering( t ) < distClustering( lastMove.parent );

			// no improvement means "overshooting": decrease step sizes
			final double primaryStepSize = lastMove == null ? initialPrimaryStep : lastMove.getStepPrimary();
			final double secondaryStepSize = lastMove == null ? initialSecondaryStep : lastMove.getStepSecondary();

			log.info( "New step Sizes:" );
			log.info( "primary : "+primaryStepSize );
			log.info( "secondary : "+secondaryStepSize );

			if ( lastMove == null || isBetter( t ) ) {

				log.info( "improvement with "+t );
				log.info( "new value "+function( t ) );
				fillQueueWithChildren(
						t,
						primaryStepSize * expansionFactor,
						secondaryStepSize * expansionFactor );

				if (lastMove != null) for ( EvolutionListener l : listeners ) l.handleMove( lastMove , true );

				return true;
			}

			log.info( "no improvement with "+t );
			log.info( "new value "+function( t ) );
			fillQueueWithChildren(
					lastMove.getParent(),
					primaryStepSize / contractionFactor,
					secondaryStepSize / contractionFactor );

			for ( EvolutionListener l : listeners ) l.handleMove( lastMove , false );

			return false;
		}

		private boolean isBetter( Thresholds t ) {
			return function( t ) < function( lastMove.getParent() );
		}

		private double function( Thresholds t ) {
			return Math.pow( distDegree( t ) , exponent ) +
				Math.pow( factorClustering * distClustering( t ) , exponent );
		}

		public Thresholds createNewThresholds() {
			if ( initial != null ) {
				final Thresholds v = initial;
				initial = null;
				return v;
			}

			lastMove = queue.remove();
			return lastMove.getChild();
		}

		private void fillQueueWithChildren( final Thresholds point , final double stepDegree , final double stepSecondary ) {
			//addToStack( moveByStep( point , stepDegree , stepSecondary ) );
			addToStack( new Move( point , 0 , stepSecondary ) );
			addToStack( new Move( point , stepDegree , 0 ) );
			addToStack( new Move( point , 0 , -stepSecondary ) );
			addToStack( new Move( point , -stepDegree , 0 ) );
		}
		
		private void addToStack( final Move move  ) {
			if ( !tabu.add( move.getChild() ) ) return;
			queue.add( move );
		}
	}

	public static class Move {
		private final Thresholds parent;
		private final double stepPrimary;
		private final double stepSecondary;
		private final Thresholds child;

		private Move(
				final Thresholds parent,
				final double stepPrimary,
				final double stepSecondary ) {
			this.parent = parent;
			this.stepPrimary = stepPrimary;
			this.stepSecondary = stepSecondary;
			this.child = new Thresholds(
					parent.getPrimaryThreshold() + stepPrimary,
					parent.getSecondaryReduction() + stepSecondary );
		}

		public Thresholds getParent() {
			return parent;
		}

		public double getStepPrimary() {
			return stepPrimary;
		}

		public double getStepSecondary() {
			return stepSecondary;
		}

		public Thresholds getChild() {
			return child;
		}
	}

	public static interface EvolutionListener {
		public void handleMove( Move m , boolean improved );
	}

	private static class EvolutionLogger implements EvolutionListener {
		@Override
		public void handleMove( final Move m , final boolean improved ) {
			log.info( "generated network for "+m.getChild() );
		}
	}
}

