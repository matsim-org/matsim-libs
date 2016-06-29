/* *********************************************************************** *
 * project: org.matsim.*
 * AllocateVehicleToSubtourAlgorithm.java
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
package org.matsim.contrib.socnetsim.sharedvehicles.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;

/**
 * Allocates a random vehicle to a random tour ( "root subtour" ) containing a given
 * <u>main</u> mode. The vehicle is allocated to all NetworkRoutes in the trips with
 * the given main mode in the choosen tour.
 * @author thibautd
 */
public class AllocateVehicleToSubtourAlgorithm implements PlanAlgorithm {
	private final String mode;
	private final VehicleRessources vehicleRessources;
	private final TripRouter tripRouter;
	private final Random random;

	public AllocateVehicleToSubtourAlgorithm(
			final Random random,
			final String mode,
			final TripRouter router,
			final VehicleRessources vehicleRessources) {
		this.random = random;
		this.mode = mode;
		this.tripRouter = router;
		this.vehicleRessources = vehicleRessources;
	}

	@Override
	public void run(final Plan plan) {
		final List<Id> vehs = new ArrayList<Id>(
			vehicleRessources.identifyVehiclesUsableForAgent(
					plan.getPerson().getId() ) );
		// make sure order is deterministic
		Collections.sort( vehs );

		final List<Subtour> rootSubtours = getRootSubtoursWithMode( plan );
		if ( rootSubtours.isEmpty() ) return;

		final Subtour toHandle = rootSubtours.get( random.nextInt( rootSubtours.size() ) );
		final Id veh = vehs.get( random.nextInt( vehs.size() ) );

		for ( Trip t : toHandle.getTrips() ) {
			if ( !identifyMainMode( t ).equals( mode ) ) continue;
			for ( Leg l : t.getLegsOnly() ) {
				if ( !(l.getRoute() instanceof NetworkRoute) ) continue;
				final NetworkRoute route = (NetworkRoute) l.getRoute();
				route.setVehicleId( veh );
			}
		}
	}

	private String identifyMainMode(final Trip t) {
		return tripRouter.getMainModeIdentifier().identifyMainMode( t.getTripElements() );
	}

	private List<Subtour> getRootSubtoursWithMode(final Plan plan) {
		final Collection<Subtour> allSubtours = TripStructureUtils.getSubtours( plan , tripRouter.getStageActivityTypes() );
		final List<Subtour> roots = new ArrayList<Subtour>();

		for ( Subtour s : allSubtours ) {
			if ( s.getParent() != null ) continue;
			if ( !containsMode( s ) ) continue;
			roots.add( s );
		}

		return roots;
	}

	private boolean containsMode( final Subtour s ) {
		for ( Trip t : s.getTrips() ) {
			if ( tripRouter.getMainModeIdentifier().identifyMainMode(
						t.getTripElements() ).equals( mode ) ) {
				return true;
			}
		}
		return false;
	}
}

