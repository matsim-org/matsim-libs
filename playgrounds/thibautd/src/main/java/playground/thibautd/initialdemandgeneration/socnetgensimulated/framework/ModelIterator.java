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
import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;
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

	private final int maxIterations;

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
		final MultivariateOptimizer optimizer =
			new PowellOptimizer(
					1E-5,1E-5,
					new Convergence() );

		final double x = initialThresholds.getPrimaryThreshold();
		final double y = initialThresholds.getSecondaryReduction();

		final PointValuePair result =
			optimizer.optimize(
					GoalType.MINIMIZE,
					new MaxEval( maxIterations ),
					new InitialGuess( new double[]{ x , y } ),
					new ObjectiveFunction( new Function( runner ) )
					);

		return runner.runModel( new Thresholds( result.getPoint()[ 0 ] , result.getPoint()[ 1 ] ) );
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

	private double distClustering( final Thresholds thresholds ) {
		return Math.abs( targetClustering -  thresholds.getResultingClustering() );
	}

	private double distDegree( final Thresholds thresholds ) {
		return Math.abs( targetDegree -  thresholds.getResultingAverageDegree() );
	}

	public static interface EvolutionListener {
		public void handleMove( Thresholds m , double fitness );
	}

	private static class EvolutionLogger implements EvolutionListener {
		@Override
		public void handleMove( final Thresholds m , final double fitness ) {
			log.info( "generated network for "+m+" -> fitness="+fitness );
		}
	}

	private class Function implements MultivariateFunction {
		private final ModelRunner runner;

		public Function( ModelRunner runner ) {
			this.runner = runner;
		}

		@Override
		public double value( final double[] args ) {
			final Thresholds thr = new Thresholds( args[ 0 ] , args[ 1 ] );
	 		generate( runner , thr );

			final double fitness = distDegree( thr ) / precisionDegree +
				distClustering( thr ) / precisionClustering;

			for ( EvolutionListener l : listeners ) l.handleMove( thr , fitness );

			return fitness;
		}
	}

	private class Convergence implements ConvergenceChecker<PointValuePair> {
		@Override
		public boolean converged( final int i , final PointValuePair prev , final PointValuePair curr ) {
			// not really satisfying...
			return curr.getValue().doubleValue() < 1;
		}
	}
}

