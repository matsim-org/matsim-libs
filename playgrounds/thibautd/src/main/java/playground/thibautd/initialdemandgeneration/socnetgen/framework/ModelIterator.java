/* *********************************************************************** *
 * project: org.matsim.*
 * ModelRunnerUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Iteratively runs a ModelRunner, modifying the thresholds at each time step,
 * in order to obtain average personal network size and clustering coefficients
 * as close to pre-defined targets as possible.
 * <br>
 * This corresponds to the "manual calibration" of Arentze's paper, except that
 * I do not like to perform repetitive task when I have a computer available.
 *
 * @author thibautd
 */
public class ModelIterator {
	private static final Logger log =
		Logger.getLogger(ModelIterator.class);
	private static final double PRECISION_PRIMARY = 1E-1;
	private static final double PRECISION_SECONDARY = 1E-2;
	private static final double SEARCH_STEP = 5;
	private static final int MAX_NO_PROGRESS = 5;

	public static interface IterationListener {
		public void notifyStats(
				ThresholdFunction thresholds,
				double avgPersonalNetworkSize,
				double clusteringCoefficient);
	}

	private final List<IterationListener> listeners = new ArrayList<IterationListener>();

	public void addListener(final IterationListener l) {
		listeners.add( l );
	}

	public <T extends Agent> LockedSocialNetwork iterateModelToTarget(
			final ModelRunner<T> runner,
			final SocialPopulation<T> population,
			final double targetAvgPersonalNetSize,
			final double targetClusteringCoeficient,
			final int iterations) {
		LockedSocialNetwork network = runner.run( population );

		notifyNewState( runner.getThresholds() , network , -1 , -1 );
		for ( int i = 0; i < iterations; i++ ) {
			network = convergePrimary(
					network,
					runner,
					population,
					targetAvgPersonalNetSize );
			network = convergeSecondary(
					network,
					runner,
					population,
					targetClusteringCoeficient );
		}

		return network;
	}

	private <T extends Agent> LockedSocialNetwork convergePrimary(
			final LockedSocialNetwork initialNetwork,
			final ModelRunner<T> runner,
			final SocialPopulation<T> population,
			final double target) {
		LockedSocialNetwork currentbest = initialNetwork;
		double bestNetSize = SnaUtils.calcAveragePersonalNetworkSize( initialNetwork );
		double bestThreshold = runner.getThresholds().getPrimaryTieThreshold();

		// assumes that net size decreases with threshold increase
		// and use binary search until randomness makes it invalid
		final AdaptiveThreshold adaptiveThreshold = new AdaptiveThreshold( target );

		adaptiveThreshold.notifyNewValue(
				bestThreshold,
				bestNetSize );

		while ( adaptiveThreshold.continueSearch() &&
				continueBasedOnStat( target , bestNetSize , PRECISION_PRIMARY ) ) {
			final double newThreshold = adaptiveThreshold.newThreshold();
			runner.getThresholds().setPrimaryTieThreshold( newThreshold );

			final LockedSocialNetwork newNetPrimary = runner.runPrimary( population );
			final double newNetPrimarySize = SnaUtils.calcAveragePersonalNetworkSize( newNetPrimary );
			if ( newNetPrimarySize > target &&
				Math.abs( target - bestNetSize ) <= Math.abs( target - newNetPrimarySize ) ) {
				// already worst than current best, and can only get farther
				adaptiveThreshold.updateLowerBoundWithoutResult( newThreshold );
				log.info( "primary tie generation aborted" );
				continue;
			}

			final LockedSocialNetwork newNet = runner.runSecondary( newNetPrimary , population );
			final double newNetSize = SnaUtils.calcAveragePersonalNetworkSize( newNet );
			notifyNewState( runner.getThresholds() , newNet , newNetSize , -1 );
			if ( Math.abs( target - bestNetSize ) > Math.abs( target - newNetSize ) ) {
				bestNetSize = newNetSize;
				currentbest = newNet;
				bestThreshold = newThreshold;
			}

			adaptiveThreshold.notifyNewValue(
				newThreshold,
				newNetSize );
		}

		// make the thresholds match the ones used to generate the returned network
		runner.getThresholds().setPrimaryTieThreshold( bestThreshold );
		return currentbest;
	}

	private void notifyNewState(
			final ThresholdFunction thresholds,
			final LockedSocialNetwork newNet,
			final double newNetSize,
			final double newClusteringCoef) {
		if ( listeners.isEmpty() ) return;
		double netSize = newNetSize >= 0 ? newNetSize : SnaUtils.calcAveragePersonalNetworkSize( newNet );
		double clustering = newClusteringCoef >= 0 ? newClusteringCoef : SnaUtils.calcClusteringCoefficient( newNet );

		for ( IterationListener l : listeners ) {
			l.notifyStats( thresholds , netSize , clustering );
		}
	}

	private <T extends Agent> LockedSocialNetwork convergeSecondary(
			final LockedSocialNetwork initialNetwork,
			final ModelRunner<T> runner,
			final SocialPopulation<T> population,
			final double target) {
		LockedSocialNetwork currentbest = initialNetwork;
		double bestClustering = SnaUtils.calcClusteringCoefficient( initialNetwork );
		// adaptive threshold assumes decreasing stat with threshold
		double bestThreshold = -runner.getThresholds().getSecondaryReduction();

		// assumes that clustering index decreases with threshold increase,
		// and use binary search until randomness makes it invalid
		final AdaptiveThreshold adaptiveThreshold = new AdaptiveThreshold( target );

		adaptiveThreshold.notifyNewValue(
			bestThreshold,
			bestClustering );

		// this is stable: only compute once
		final LockedSocialNetwork primaryNetwork = runner.runPrimary( population );
		while ( adaptiveThreshold.continueSearch() &&
				continueBasedOnStat( target , bestClustering , PRECISION_SECONDARY ) ) {
			// the iteration process does not consider the fact that the reduction
			// must be negative.
			final double newThreshold = Math.min( 0 , adaptiveThreshold.newThreshold() );
			runner.getThresholds().setSecondaryReduction( -newThreshold );

			final LockedSocialNetwork newNet = runner.runSecondary( primaryNetwork , population );
			double newClustering = SnaUtils.calcClusteringCoefficient( newNet );
			notifyNewState( runner.getThresholds() , newNet , -1 , newClustering );
			if ( Math.abs( target - bestClustering ) >= Math.abs( target - newClustering ) ) {
				bestClustering = newClustering;
				currentbest = newNet;
				bestThreshold = newThreshold;
			}

			adaptiveThreshold.notifyNewValue(
				newThreshold,
				newClustering );
		}

		// make the thresholds match the ones used to generate the returned network
		runner.getThresholds().setSecondaryReduction( -bestThreshold );
		return currentbest;
	}

	private static boolean continueBasedOnStat(
			final double target,
			final double stat,
			final double precision) {
		final double dist = Math.abs( target - stat );
		final boolean v = dist > precision;

		log.info( "stat "+stat+" at distance "+dist+" of target "+target );
		log.info( (v ? "CONTINUE" : "STOP" )+" at precision "+precision );

		return v;
	}

	// adapts threshold assuming stat monotonically decreases with threshold
	private static class AdaptiveThreshold {
		private final double step = SEARCH_STEP;
		private final double targetStat;
		private double lowerBoundThreshold = Double.NaN;
		private double upperBoundThreshold = Double.NaN;
		private double valueAtLowerBound = Double.NaN;
		private double valueAtUpperBound = Double.NaN;

		// to allow interpolation before having a proper interval
		private double controlThreshold = Double.NaN;
		private double controlValue = Double.NaN;

		// each time stepping is needed, increase step size
		// allows to adapt worng step size: the aim is to find bounds,
		// search afterwards becomes fast
		private int multiplicator = 1;

		private int nSuccessiveLackOfSuccess = 0;

		public AdaptiveThreshold(final double targetStat) {
			this.targetStat = targetStat;
		}

		public void notifyNewValue(
				final double usedThreshold,
				final double resultStat) {
			if ( resultStat < targetStat ) {
				log.info( "new stat "+resultStat+" < "+targetStat );
				if ( !Double.isNaN( upperBoundThreshold ) &&
						Double.isNaN( controlThreshold ) ) {
					log.info( "store former upper bound as control value" );
					this.controlThreshold = upperBoundThreshold;
					this.controlValue = valueAtUpperBound;
				}

				if ( Double.isNaN( lowerBoundThreshold ) &&
						!Double.isNaN( valueAtUpperBound ) &&
						Math.abs( resultStat - valueAtUpperBound ) < 1E-5 ) {
					nSuccessiveLackOfSuccess++;
				}
				else nSuccessiveLackOfSuccess = 0;

				this.upperBoundThreshold = usedThreshold;
				this.valueAtUpperBound = resultStat;
			}
			else {
				log.info( "new stat "+resultStat+" >= "+targetStat );
				if ( !Double.isNaN( lowerBoundThreshold ) &&
						Double.isNaN( controlThreshold ) ) {
					log.info( "store former lower bound as control value" );
					this.controlThreshold = lowerBoundThreshold;
					this.controlValue = valueAtLowerBound;
				}

				if ( Double.isNaN( upperBoundThreshold ) &&
						!Double.isNaN( valueAtLowerBound ) &&
						Math.abs( resultStat - valueAtLowerBound ) < 1E-5 ) {
					nSuccessiveLackOfSuccess++;
				}
				else nSuccessiveLackOfSuccess = 0;

				this.lowerBoundThreshold = usedThreshold;
				this.valueAtLowerBound = resultStat;
			}
		}

		public double newThreshold() {
			final double interpolatedThreshold = calcThreshold( true );
			final double newThreshold =
				!acceptInterpolatedThreshold( interpolatedThreshold ) ?
					calcThreshold( false ) :
					interpolatedThreshold;
			log.info( "new threshold "+newThreshold+" in ["+lowerBoundThreshold+" ("+valueAtLowerBound+") ; "+
					upperBoundThreshold+" ("+valueAtUpperBound+")] with control "+
					controlThreshold+" ("+controlValue+")" );
			assert Double.isNaN( lowerBoundThreshold ) || newThreshold > lowerBoundThreshold : newThreshold+" not in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[";
			assert Double.isNaN( upperBoundThreshold ) || newThreshold < upperBoundThreshold : newThreshold+" not in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[";
			return newThreshold;
		}

		private boolean acceptInterpolatedThreshold(
				final double t) {
			if ( Double.isInfinite( t ) ) {
				log.info( "reject infinite interpolated threshold "+t );
				return false;
			}

			if ( !Double.isNaN( lowerBoundThreshold ) && t < lowerBoundThreshold ) {
				log.info( "reject interpolated threshold "+t+" lower than lower bound "+lowerBoundThreshold );
				log.info( "this can happen because of randomness if values at bounds/control are too close." );
				return false;
			}

			if ( !Double.isNaN( upperBoundThreshold ) && t > upperBoundThreshold ) {
				log.info( "reject interpolated threshold "+t+" upper than upper bound "+upperBoundThreshold );
				log.info( "this can happen because of randomness if values at bounds/control are too close." );
				return false;
			}

			return true;
		}

		private double calcThreshold( final boolean interpolate ) {
			assert !Double.isNaN( lowerBoundThreshold ) || !Double.isNaN( upperBoundThreshold );

			if ( Double.isNaN( upperBoundThreshold ) ) {
				if ( !interpolate ||
						Double.isNaN( controlThreshold ) ||
						Double.isNaN( valueAtLowerBound ) ) {
					log.info( "new threshold: step-augmented lower bound" );
					return lowerBoundThreshold + (multiplicator++) * step;
				}

				log.info( "new threshold: interpolated from lower bound and control" );
				return interpolate(
						lowerBoundThreshold,
						valueAtLowerBound,
						controlThreshold,
						controlValue);
			}

			if ( Double.isNaN( lowerBoundThreshold ) ) {
				if ( !interpolate ||
						Double.isNaN( controlThreshold ) ||
						Double.isNaN( valueAtUpperBound ) ) {
					log.info( "new threshold: step-diminished upper bound" );
					return upperBoundThreshold - (multiplicator++) * step;
				}

				log.info( "new threshold: interpolated from upper bound and control" );
				return interpolate(
						upperBoundThreshold,
						valueAtUpperBound,
						controlThreshold,
						controlValue);
			}

			if ( interpolate && !Double.isNaN( controlValue ) &&
					(Double.isNaN( valueAtLowerBound ) || Double.isNaN( valueAtUpperBound )) ) {
				if ( !Double.isNaN( valueAtLowerBound ) ) {
					log.info( "new threshold: interpolated from lower bound and control" );
					return interpolate(
							lowerBoundThreshold,
							valueAtLowerBound,
							controlThreshold,
							controlValue);
				}

				if ( !Double.isNaN( valueAtUpperBound ) ) {
					log.info( "new threshold: interpolated from lower bound and control" );
					return interpolate(
							upperBoundThreshold,
							valueAtUpperBound,
							controlThreshold,
							controlValue);
				}
			}
			
			// do not interpolate if bounds are known:
			// the relationship between the parameters and the statistics
			// not being linear, interpolation was found not to give a clear advantage
			// over "pure" binary search. It is however better than the step-based approach
			// to find initial bounds.
			log.info( "new threshold: half interval" );
			return (lowerBoundThreshold + upperBoundThreshold) / 2d;
		}

		private double interpolate(
				final double x1,
				final double y1,
				final double x2,
				final double y2 ) {
			final double slope = (y2 - y1) /
				(x2 - x1);

			final double intercept = y1 - slope * x1;

			final double t = (targetStat - intercept) / slope;

			assert !Double.isNaN( t );

			return t;
		}

		public void updateLowerBoundWithoutResult(
				final double newBound) {
			assert newBound < upperBoundThreshold;
			this.lowerBoundThreshold = newBound;
			this.valueAtLowerBound = Double.NaN;
		}

		public boolean continueSearch( ) {
			if ( nSuccessiveLackOfSuccess > MAX_NO_PROGRESS ) {
				log.info( "stop due to lack of progression" );
				return false;
			}

			// stop if lower bound greater than upper bound
			final boolean basedOnBounds = Double.isNaN( lowerBoundThreshold ) ||
				Double.isNaN( upperBoundThreshold ) ||
				lowerBoundThreshold < upperBoundThreshold;

			log.info( "interval ["+lowerBoundThreshold+" ; "+upperBoundThreshold+"]: "+
					(basedOnBounds ? "CONTINUE" : "STOP" ) );

			// or if values at the two bounds are undistinguishable (no progress possible)
			final boolean basedOnValues =
				Double.isNaN( valueAtLowerBound ) ||
				Double.isNaN( valueAtUpperBound ) ||
				valueAtLowerBound > valueAtUpperBound + 1E-5;

			log.info( "interval ["+lowerBoundThreshold+" ("+valueAtLowerBound+") ; "+
					upperBoundThreshold+" ("+valueAtUpperBound+")]: "+
					(basedOnValues ? "CONTINUE" : "STOP" )+
					" based on values" );

			return basedOnBounds && basedOnValues;
		}
	}
}

