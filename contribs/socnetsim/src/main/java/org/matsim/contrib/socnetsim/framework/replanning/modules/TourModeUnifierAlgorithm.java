/* *********************************************************************** *
 * project: org.matsim.*
 * TourModeUnifierAlgorithm.java
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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import java.util.Collections;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * sets the mode of all trips in full tours to the same mode.
 * Useful after operations which may change the tour structure
 * (for instance location choice or sequence mutation)
 * @author thibautd
 */
public class TourModeUnifierAlgorithm implements PlanAlgorithm {
	private final StageActivityTypes stages;
	private final SubtourModeIdentifier modeIdentifier;

	public TourModeUnifierAlgorithm(
			final StageActivityTypes stages,
			final MainModeIdentifier modeIdentifier) {
		this( stages,
				new SubtourFirstModeIdentifier(
					modeIdentifier ) );
	}

	public TourModeUnifierAlgorithm(
			final StageActivityTypes stages,
			final SubtourModeIdentifier modeIdentifier) {
		this.stages = stages;
		this.modeIdentifier = modeIdentifier;
	}

	@Override
	public void run(final Plan plan) {
		for ( Subtour subtour : TripStructureUtils.getSubtours( plan , stages ) ) {
			// not clear what we should do with open tours
			if ( !subtour.isClosed() ) continue;
			// only consider "root" tours: tours without (closed) parent
			if ( subtour.getParent() != null && subtour.getParent().isClosed() ) continue;

			final String mode = modeIdentifier.identifyMode( subtour );

			for ( Trip trip : subtour.getTrips() ) {
				TripRouter.insertTrip(
						plan,
						trip.getOriginActivity(),
						Collections.singletonList( new LegImpl( mode ) ),
						trip.getDestinationActivity() );
			}
		}
	}

	public static interface SubtourModeIdentifier {
		public String identifyMode( final Subtour subtour );
	}

	public static final class SubtourFirstModeIdentifier implements SubtourModeIdentifier {
		private final MainModeIdentifier modeIdentifier;

		public SubtourFirstModeIdentifier(
				final MainModeIdentifier modeIdentifier) {
			this.modeIdentifier = modeIdentifier;
		}

		@Override
		public String identifyMode(final Subtour subtour) {
			return modeIdentifier.identifyMainMode(
					subtour.getTrips().get( 0 ).getTripElements() );
		}
	}
}

