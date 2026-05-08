/* *********************************************************************** *
 * project: org.matsim.*
 * MyRoutingModule.java
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
package org.matsim.codeexamples.router.example13MultiStageTripRouting;

import com.google.inject.Provider;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RoutingModule} for a mode consisting in going to a teleportation
 * station by public transport, and being instantly teleported to destination
 * 
 * @author thibautd
 */
public class MyRoutingModule implements RoutingModule {
	public static final String STAGE = "teleportation interaction";
	public static final String TELEPORTATION_LEG_MODE = "teleportationLeg";

	public static final String TELEPORTATION_MAIN_MODE = "myTeleportationMainMode";

	private final Provider<RoutingModule> routingDelegate;
	private final PopulationFactory populationFactory;
	private final RouteFactories modeRouteFactory;
	private final Facility station;

	/**
	 * Creates a new instance.
	 * @param routingDelegate the {@link TripRouter} to use to compute the PT subtrips
	 * @param populationFactory used to create legs, activities and routes
	 * @param station {@link Facility} representing the teleport station
	 */
	public MyRoutingModule(
			// I do not know what is best here: RoutingModule or TripRouter.
			// RoutingModule is the level we actually need, but
			// getting the TripRouter allows to be consistent with modifications
			// of the TripRouter done later in the initialization process (delegation).
			// Using TripRouter may also lead to infinite loops, if two  modes
			// calling each other (though I cannot think in any actual mode with this risk).
			final Provider<RoutingModule> routingDelegate,
			final PopulationFactory populationFactory,
			final Facility station) {
		this.routingDelegate = routingDelegate;
		this.populationFactory = populationFactory;
		this.modeRouteFactory = populationFactory.getRouteFactories();
		this.station = station;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {

		final List<PlanElement> trip = new ArrayList<>(routingDelegate.get().calcRoute(request));

		// create a dummy activity at the teleportation origin
		final Activity interaction = populationFactory.createActivityFromLinkId(
						STAGE, station.getLinkId());

		interaction.setMaximumDuration( 0 );
		trip.add( interaction );

		// create the teleportation leg
		final Leg teleportationLeg = populationFactory.createLeg( TELEPORTATION_LEG_MODE );
		teleportationLeg.setTravelTime( 0 );
		final Route teleportationRoute = modeRouteFactory.createRoute(
						Route.class, station.getLinkId(), request.getToFacility().getLinkId());

		teleportationRoute.setTravelTime( 0 );
		teleportationLeg.setRoute( teleportationRoute );
		trip.add( teleportationLeg );

		return trip;
	}
}

