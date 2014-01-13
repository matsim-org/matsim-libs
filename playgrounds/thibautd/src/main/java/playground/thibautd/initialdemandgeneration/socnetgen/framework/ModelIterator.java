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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.ModelRunner.SecondaryTieLimitExceededException;

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
	private static final double PRECISION_SECONDARY = 1E-3;
	private static final double SEARCH_STEP = 5;

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

	public <T extends Agent> SocialNetwork iterateModelToTarget(
			final ModelRunner<T> runner,
			final SocialPopulation<T> population,
			final double targetAvgPersonalNetSize,
			final double targetClusteringCoeficient,
			final int iterations) {
		SocialNetwork network = runner.run( population );

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

	private <T extends Agent> SocialNetwork convergePrimary(
			final SocialNetwork initialNetwork,
			final ModelRunner<T> runner,
			final SocialPopulation<T> population,
			final double target) {
		SocialNetwork currentbest = initialNetwork;
		double bestNetSize = SnaUtils.calcAveragePersonalNetworkSize( initialNetwork );
		double bestThreshold = runner.getThresholds().getPrimaryTieThreshold();

		// assumes that net size decreases with threshold increase
		// and use binary search until randomness makes it invalid
		final AdaptiveThreshold adaptiveThreshold = new AdaptiveThreshold( target );

		adaptiveThreshold.notifyNewValue(
				bestThreshold,
				bestNetSize );

		while ( adaptiveThreshold.continueSearch() &&
				Math.abs( target - bestNetSize ) > PRECISION_PRIMARY ) {
			final double newThreshold = adaptiveThreshold.newThreshold();
			runner.getThresholds().setPrimaryTieThreshold( newThreshold );

			final SocialNetwork newNetPrimary = runner.runPrimary( population );
			final double newNetPrimarySize = SnaUtils.calcAveragePersonalNetworkSize( newNetPrimary );
			if ( newNetPrimarySize > target &&
				Math.abs( target - bestNetSize ) <= Math.abs( target - newNetPrimarySize ) ) {
				// already worst than current best, and can only get farther
				adaptiveThreshold.updateLowerBoundWithoutResult( newThreshold );
				log.info( "primary tie generation aborted" );
				continue;
			}

			try {
				final double nTies = newNetPrimarySize * population.getAgents().size();
				final double targetTies = target * population.getAgents().size();
				final long maxNSecondaryTies = (long) (targetTies - nTies);

				final SocialNetwork newNet = runner.runSecondary( newNetPrimary , population , maxNSecondaryTies );
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
			catch (SecondaryTieLimitExceededException e) {
				// this skips the whole "best" updating
				// this looks almost as ugly as good ol' goto,
				// but could not find a better way to do it right.
				adaptiveThreshold.updateLowerBoundWithoutResult( newThreshold );
				log.info( "secondary tie generation aborted" );
			}
		}

		// make the thresholds match the ones used to generate the returned network
		assert adaptiveThreshold.inRange( bestThreshold );
		runner.getThresholds().setPrimaryTieThreshold( bestThreshold );
		return currentbest;
	}

	private void notifyNewState(
			final ThresholdFunction thresholds,
			final SocialNetwork newNet,
			final double newNetSize,
			final double newClusteringCoef) {
		if ( listeners.isEmpty() ) return;
		double netSize = newNetSize >= 0 ? newNetSize : SnaUtils.calcAveragePersonalNetworkSize( newNet );
		double clustering = newClusteringCoef >= 0 ? newClusteringCoef : SnaUtils.calcClusteringCoefficient( newNet );

		for ( IterationListener l : listeners ) {
			l.notifyStats( thresholds , netSize , clustering );
		}
	}

	private <T extends Agent> SocialNetwork convergeSecondary(
			final SocialNetwork initialNetwork,
			final ModelRunner<T> runner,
			final SocialPopulation<T> population,
			final double target) {
		SocialNetwork currentbest = initialNetwork;
		double bestClustering = SnaUtils.calcClusteringCoefficient( initialNetwork );
		double bestThreshold = runner.getThresholds().getSecondaryReduction();

		// assumes that clustering index decreases with threshold increase,
		// and use binary search until randomness makes it invalid
		final AdaptiveThreshold adaptiveThreshold = new AdaptiveThreshold( target );


		adaptiveThreshold.notifyNewValue(
			bestThreshold,
			bestClustering );

		while ( adaptiveThreshold.continueSearch() &&
				Math.abs( target - bestClustering ) > PRECISION_SECONDARY ) {
			final double newThreshold = adaptiveThreshold.newThreshold();
			runner.getThresholds().setSecondaryReduction( newThreshold );

			// TODO: early abort
			final SocialNetwork newNet = runner.runSecondary( initialNetwork , population );
			double newClustering = SnaUtils.calcClusteringCoefficient( newNet );
			notifyNewState( runner.getThresholds() , newNet , -1 , newClustering );
			if ( Math.abs( target - bestClustering ) > Math.abs( target - newClustering ) ) {
				bestClustering = newClustering;
				currentbest = newNet;
				bestThreshold = newThreshold;
			}

			adaptiveThreshold.notifyNewValue(
				newThreshold,
				newClustering );
		}

		// make the thresholds match the ones used to generate the returned network
		runner.getThresholds().setSecondaryReduction( bestThreshold );
		assert adaptiveThreshold.inRange( bestThreshold );
		return currentbest;
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

		public AdaptiveThreshold(final double targetStat) {
			this.targetStat = targetStat;
		}

		public boolean inRange(final double bestThreshold) {
			return bestThreshold >= lowerBoundThreshold && bestThreshold <= upperBoundThreshold;
		}

		public void notifyNewValue(
				final double usedThreshold,
				final double resultStat) {
			if ( resultStat < targetStat ) {
				if ( !Double.isNaN( upperBoundThreshold ) &&
						Double.isNaN( controlThreshold ) ) {
					this.controlThreshold = upperBoundThreshold;
					this.controlValue = valueAtUpperBound;
				}
				this.upperBoundThreshold = usedThreshold;
				this.valueAtUpperBound = resultStat;
			}
			else {
				if ( !Double.isNaN( lowerBoundThreshold ) &&
						Double.isNaN( controlThreshold ) ) {
					this.controlThreshold = lowerBoundThreshold;
					this.controlValue = valueAtLowerBound;
				}
				this.lowerBoundThreshold = usedThreshold;
				this.valueAtLowerBound = resultStat;
			}
		}

		public double newThreshold() {
			final double newThreshold = calcThreshold();
			log.info( "new threshold "+newThreshold+" in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[" );
			assert Double.isNaN( lowerBoundThreshold ) || newThreshold > lowerBoundThreshold : newThreshold+" not in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[";
			assert Double.isNaN( upperBoundThreshold ) || newThreshold < upperBoundThreshold : newThreshold+" not in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[";
			return newThreshold;
		}

		private double calcThreshold() {
			assert !Double.isNaN( lowerBoundThreshold ) || !Double.isNaN( upperBoundThreshold );

			if ( Double.isNaN( upperBoundThreshold ) ) {
				if ( Double.isNaN( controlThreshold ) || Double.isNaN( valueAtLowerBound ) ) {
					return lowerBoundThreshold + step;
				}

				return interpolate(
						lowerBoundThreshold,
						valueAtLowerBound,
						controlThreshold,
						controlValue);
			}

			if ( Double.isNaN( lowerBoundThreshold ) ) {
				if ( Double.isNaN( controlThreshold ) || Double.isNaN( valueAtUpperBound ) ) {
					return upperBoundThreshold - step;
				}

				return interpolate(
						upperBoundThreshold,
						valueAtUpperBound,
						controlThreshold,
						controlValue);
			}
			
			if ( Double.isNaN( valueAtLowerBound ) ||
					Double.isNaN( valueAtUpperBound ) ) {
				// cannot interpolate
				return (lowerBoundThreshold + upperBoundThreshold) / 2d;
			}

			return interpolate(
					lowerBoundThreshold,
					valueAtLowerBound,
					upperBoundThreshold,
					valueAtUpperBound);
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
			return Double.isNaN( lowerBoundThreshold ) ||
				Double.isNaN( upperBoundThreshold ) ||
				lowerBoundThreshold >= upperBoundThreshold;
		}
	}
}

