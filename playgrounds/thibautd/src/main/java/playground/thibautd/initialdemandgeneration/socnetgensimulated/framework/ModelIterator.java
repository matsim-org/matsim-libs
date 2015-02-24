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

	private final int stagnationLimit;
	private final int maxIterations;

	// TODO: make adaptive (the closer to the target value,
	// the more precise is should get)
	private double samplingRateClustering = 1;
	private final List<EvolutionListener> listeners = new ArrayList< >();

	private final double exponent = 5;
	private final double contractionFactor = 2;
	private final double expansionFactor = 1.5;

	public ModelIterator( final SocialNetworkGenerationConfigGroup config ) {
		this.targetClustering = config.getTargetClustering();
		this.targetDegree = config.getTargetDegree();

		listeners.add( new EvolutionLogger() );

		setSamplingRateClustering( config.getSamplingRateForClusteringEstimation() );
		this.precisionClustering = config.getPrecisionClustering();
		this.precisionDegree = config.getPrecisionDegree();

		this.initialPrimaryStep = config.getInitialPrimaryStep();
		this.initialSecondaryStep = config.getInitialSecondaryStep();

		this.stagnationLimit = config.getStagnationLimit();
		this.maxIterations = config.getMaxIterations();
	}

	private SocialNetwork generate( final ModelRunner runner , final Thresholds thresholds ) {
		log.info( "generate network for "+thresholds );
		final long start = System.currentTimeMillis();
		final SocialNetwork sn = runner.runModel( thresholds );

		thresholds.setResultingAverageDegree( SnaUtils.calcAveragePersonalNetworkSize( sn ) );
		thresholds.setResultingClustering( estimateClustering( sn ) );

		log.info( "generation took "+(System.currentTimeMillis() - start)+" ms" );
		return sn;
	}


	public SocialNetwork iterateModelToTarget(
			final ModelRunner runner,
			final Thresholds initialThresholds ) {
		generate( runner , initialThresholds );
		Thresholds currentParent = initialThresholds;

		double stepPrimary = initialPrimaryStep;
		double stepSecondary = initialSecondaryStep;

		final Set<Thresholds> tabu = new HashSet< >();
		for ( int iter=1, stagnationCount=0;
				iter < maxIterations && stagnationCount < stagnationLimit;
				iter++, stagnationCount++ ) {
			log.info( "Iteration # "+iter );

			final List<Move> moves = new ArrayList<Move>( 4 );

			log.info( "Parent: "+currentParent );
			log.info( "primary : "+stepPrimary );
			log.info( "secondary : "+stepSecondary );

			moves.add( new Move( currentParent , 0 , stepSecondary ) );
			moves.add( new Move( currentParent , stepPrimary , 0 ) );
			moves.add( new Move( currentParent , 0 , -stepSecondary ) );
			moves.add( new Move( currentParent , -stepPrimary , 0 ) );

			Move bestChild = null;
			SocialNetwork bestChildResultingNetwork = null;

			for ( Move move : moves ) {
				final Thresholds thresholds = move.getChild();
				if ( !tabu.add( thresholds ) ) continue;
				final SocialNetwork sn = generate( runner , thresholds );

				if ( bestChild == null || function( thresholds ) < function( bestChild.getChild() ) ) {
					bestChild = move;
					bestChildResultingNetwork = sn;
				}

				for ( EvolutionListener l : listeners ) l.handleMove( move , false );
			}

			if ( isAcceptable( bestChild.getChild() ) ) {
				log.info( "END - "+bestChild+" fulfills the precision criteria!" );
				return bestChildResultingNetwork;
			}

			if ( function( bestChild.getChild() ) < function( bestChild.getParent() ) ) {
				stagnationCount = 0;
				log.info( "improvement with "+bestChild.getChild()+" compared to "+bestChild.getParent() );
				log.info( "new value "+function( bestChild.getChild() ) );
				currentParent = bestChild.getChild();
				stepPrimary *= expansionFactor;
				stepSecondary *= expansionFactor;
			}
			else {
				log.info( "no improvement with "+bestChild.getChild()+" compared to "+bestChild.getParent() );
				log.info( "new value "+function( bestChild.getChild() ) );
				stepPrimary /= contractionFactor;
				stepSecondary /= contractionFactor;
			}
		}

		log.warn( "stop iterations before reaching the required precision level!" );
		log.info( "END - Maximum number of iterations or stagnation reached. Best so far: "+currentParent );
		return runner.runModel( currentParent );
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

	private double function( Thresholds t ) {
		return Math.pow( distDegree( t ) , exponent ) +
			Math.pow( (precisionDegree / precisionClustering) * distClustering( t ) , exponent );
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

