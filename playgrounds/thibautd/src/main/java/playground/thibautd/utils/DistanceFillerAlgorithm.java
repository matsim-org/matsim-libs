/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceFillerAlgorithm.java
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
package playground.thibautd.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm which tries to estimate the distance for legs for which
 * the distance field in the route is NaN.
 * <br>
 * The reason to do this is to get this information in the Travelled events,
 * to be able to use it at scoring (KTI scoring for instances scores walk
 * based on distance, not travel time).
 * <br>
 * Note that as it is a hack for some specific use cases anyway,
 * it assumes a strict act/leg alternance.
 * <br>
 * This should be use in a {@link BeforeMobsimListener}.
 *
 * @author thibautd
 */
public class DistanceFillerAlgorithm implements PlanAlgorithm {
	private final Map<String, DistanceEstimator> distanceEstimatorPerMode = new HashMap<String, DistanceEstimator>();

	@Override
	public void run(final Plan plan) {
		final List<PlanElement> pes = plan.getPlanElements();
		for ( int i=0; i < pes.size() - 2; i+= 2) {
			// this is super dirty... But reasonably safe.
			// If the assumptions about sequence are not verified,
			// we will get a ClassCastException immediately.
			handleLeg(
					(Activity) pes.get( i ),
					(Leg) pes.get( i + 1 ),
					(Activity) pes.get( i + 2 ) );
		}
	}

	private void handleLeg(
			final Activity origin,
			final Leg leg,
			final Activity destination) {
		if ( leg.getRoute() == null ) return;
		if ( !Double.isNaN( leg.getRoute().getDistance() ) ) return;

		final DistanceEstimator estimator = distanceEstimatorPerMode.get( leg.getMode() );
		if ( estimator == null ) return;

		leg.getRoute().setDistance(
				estimator.calcDistance(
					origin,
					leg,
					destination) );
	}

	public DistanceEstimator putEstimator(
			final String mode,
			final DistanceEstimator estimator) {
		return distanceEstimatorPerMode.put( mode , estimator );
	}

	// /////////////////////////////////////////////////////////////////////////
	// estimator: interface and default implementations
	public static interface DistanceEstimator {
		public double calcDistance( Activity origin, Leg leg, Activity destination );
	}

	public static final class CrowFlyEstimator implements DistanceEstimator {
		final Network network;
		final double factor;

		public CrowFlyEstimator(
				final double factor,
				final Network network) {
			this.factor = factor;
			this.network = network;
		}

		@Override
		public double calcDistance(
				final Activity origin,
				final Leg leg,
				final Activity destination ) {
			final Coord startCoord =
				origin.getCoord() != null ?
					origin.getCoord() :
					network.getLinks().get( leg.getRoute().getStartLinkId() ).getCoord();
			final Coord endCoord =
				destination.getCoord() != null ?
					destination.getCoord() :
					network.getLinks().get( leg.getRoute().getEndLinkId() ).getCoord();

			assert startCoord != null;
			assert endCoord != null;

			return factor * CoordUtils.calcEuclideanDistance( startCoord , endCoord );
		}
	}
}

