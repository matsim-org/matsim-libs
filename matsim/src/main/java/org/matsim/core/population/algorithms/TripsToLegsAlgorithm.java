/* *********************************************************************** *
 * project: org.matsim.*
 * TripsToLegAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.core.population.algorithms;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
 * Simplifies a plan to its structure, so that plan algorithms
 * working with strict leg/act alternance can work on it.
 * Routing will be necessary before execution.
 *
 * @author thibautd
 */
public final class TripsToLegsAlgorithm implements PlanAlgorithm {
	private final MainModeIdentifier mainModeIdentifier;

	public TripsToLegsAlgorithm(final TripRouter router) {
		this( TripStructureUtils.getRoutingModeIdentifier() );
	}


	public TripsToLegsAlgorithm(
			final MainModeIdentifier mainModeIdentifier) {
		this.mainModeIdentifier = mainModeIdentifier;
	}

	@Override
	public void run(final Plan plan) {
		final List<PlanElement> planElements = plan.getPlanElements();
		final List<Trip> trips = TripStructureUtils.getTrips( plan );

		for ( Trip trip : trips ) {
			final List<PlanElement> fullTrip =
				planElements.subList(
						planElements.indexOf( trip.getOriginActivity() ) + 1,
						planElements.indexOf( trip.getDestinationActivity() ));
			final String mode = mainModeIdentifier.identifyMainMode( fullTrip );
			fullTrip.clear();
			Leg leg = PopulationUtils.createLeg(mode);
			TripStructureUtils.setRoutingMode(leg, mode);
			fullTrip.add( leg );
			if ( fullTrip.size() != 1 ) throw new RuntimeException( fullTrip.toString() );
		}
	}
}

