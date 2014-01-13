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
import java.util.Random;

import org.apache.log4j.Logger;

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
	private static final double PRECISION_PRIMARY = 1E-4;
	private static final double PRECISION_SECONDARY = 1E-4;
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
		final Random random = new Random( 1943 );
		for ( int i = 0; i < iterations; i++ ) {
			network = convergePrimary(
					network,
					runner,
					population,
					targetAvgPersonalNetSize );
			network = convergeSecondary(
					random,
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
		double lowerBoundThreshold = Double.NaN;
		double upperBoundThreshold = Double.NaN;

		if ( bestNetSize < target ) {
			upperBoundThreshold = runner.getThresholds().getPrimaryTieThreshold();
		}
		else {
			lowerBoundThreshold = runner.getThresholds().getPrimaryTieThreshold();
		}

		while ( Double.isNaN( lowerBoundThreshold ) ||
				Double.isNaN( upperBoundThreshold ) ||
				lowerBoundThreshold < upperBoundThreshold - PRECISION_PRIMARY ) {
			final double newThreshold = newThreshold( lowerBoundThreshold , upperBoundThreshold , SEARCH_STEP );
			log.info( "new primary threshold "+newThreshold+" in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[" );
			assert Double.isNaN( lowerBoundThreshold ) || newThreshold > lowerBoundThreshold : newThreshold+" not in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[";
			assert Double.isNaN( upperBoundThreshold ) || newThreshold < upperBoundThreshold : newThreshold+" not in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[";
			runner.getThresholds().setPrimaryTieThreshold( newThreshold );

			final SocialNetwork newNet = runner.run( population );
			double newNetSize = SnaUtils.calcAveragePersonalNetworkSize( newNet );
			notifyNewState( runner.getThresholds() , newNet , newNetSize , -1 );
			if ( Math.abs( target - bestNetSize ) > Math.abs( target - newNetSize ) ) {
				bestNetSize = newNetSize;
				currentbest = newNet;
				bestThreshold = newThreshold;
			}

			if ( newNetSize < target ) {
				upperBoundThreshold = newThreshold;
			}
			else {
				lowerBoundThreshold = newThreshold;
			}
		}

		// make the thresholds match the ones used to generate the returned network
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
			final Random random,
			final SocialNetwork initialNetwork,
			final ModelRunner<T> runner,
			final SocialPopulation<T> population,
			final double target) {
		SocialNetwork currentbest = initialNetwork;
		double bestClustering = SnaUtils.calcClusteringCoefficient( initialNetwork );
		double bestThreshold = runner.getThresholds().getSecondaryReduction();

		// assumes that clustering index decreases with threshold increase,
		// and use binary search until randomness makes it invalid
		double lowerBoundThreshold = Double.NaN;
		double upperBoundThreshold = Double.NaN;

		if ( bestClustering < target ) {
			upperBoundThreshold = runner.getThresholds().getSecondaryTieThreshold();
		}
		else {
			lowerBoundThreshold = runner.getThresholds().getSecondaryTieThreshold();
		}

		while ( Double.isNaN( lowerBoundThreshold ) ||
				Double.isNaN( upperBoundThreshold ) ||
				lowerBoundThreshold < upperBoundThreshold - PRECISION_SECONDARY) {
			final double newThreshold = newThreshold( lowerBoundThreshold , upperBoundThreshold , SEARCH_STEP );
			log.info( "new secondary threshold "+newThreshold+" in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[" );
			assert Double.isNaN( lowerBoundThreshold ) || newThreshold > lowerBoundThreshold : newThreshold+" not in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[";
			assert Double.isNaN( upperBoundThreshold ) || newThreshold < upperBoundThreshold : newThreshold+" not in ]"+lowerBoundThreshold+" ; "+upperBoundThreshold+"[";
			runner.getThresholds().setSecondaryReduction( newThreshold );

			final SocialNetwork newNet = new SocialNetwork( initialNetwork );
			runner.fillInSecondaryTies( random , newNet , population );
			double newClustering = SnaUtils.calcClusteringCoefficient( newNet );
			notifyNewState( runner.getThresholds() , newNet , -1 , newClustering );
			if ( Math.abs( target - bestClustering ) > Math.abs( target - newClustering ) ) {
				bestClustering = newClustering;
				currentbest = newNet;
				bestThreshold = newThreshold;
			}

			if ( newClustering < target ) {
				upperBoundThreshold = newThreshold;
			}
			else {
				lowerBoundThreshold = newThreshold;
			}
		}

		// make the thresholds match the ones used to generate the returned network
		runner.getThresholds().setSecondaryReduction( bestThreshold );
		return currentbest;
	}

	private static double newThreshold(
			final double lowerBoundThreshold,
			final double upperBoundThreshold,
			final double step) {
		assert !Double.isNaN( lowerBoundThreshold ) || !Double.isNaN( upperBoundThreshold );
		return Double.isNaN( upperBoundThreshold ) ? lowerBoundThreshold + step :
			 Double.isNaN( lowerBoundThreshold ) ? upperBoundThreshold - step :
			 (lowerBoundThreshold + upperBoundThreshold) / 2d;
	}
}

