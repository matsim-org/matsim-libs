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
package tutorial.programming.example13MultiStageTripRouting;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Provider;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;

/**
 * {@link RoutingModule} for a mode consisting in going to a teleportation
 * station by public transport, and being instantly teleported to destination
 * 
 * @author thibautd
 */
public class MyRoutingModule implements RoutingModule {
	public static final String STAGE = "teleportationInteraction";
	public static final String TELEPORTATION_LEG_MODE = "teleportationLeg";

	public static final String TELEPORTATION_MAIN_MODE = "myTeleportationMainMode";

	private final Provider<RoutingModule> routingDelegate;
	private final PopulationFactory populationFactory;
	private final RouteFactoryImpl modeRouteFactory;
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
		this.modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getRouteFactory();
		this.station = station;
	}

	@Override
	public List<PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final List<PlanElement> trip = new ArrayList<PlanElement>();

		// route the access trip
		trip.addAll(
				routingDelegate.get().calcRoute(
//						TransportMode.pt,
						fromFacility,
						station,
						departureTime,
						person) );

		// create a dummy activity at the teleportation origin
		final Activity interaction =
			populationFactory.createActivityFromLinkId(
					STAGE,
					station.getLinkId());
		interaction.setMaximumDuration( 0 );
		trip.add( interaction );

		// create the teleportation leg
		final Leg teleportationLeg =
			populationFactory.createLeg( TELEPORTATION_LEG_MODE );
		teleportationLeg.setTravelTime( 0 );
		final Route teleportationRoute =
			modeRouteFactory.createRoute(
					Route.class,
					station.getLinkId(),
					toFacility.getLinkId());
		teleportationRoute.setTravelTime( 0 );
		teleportationLeg.setRoute( teleportationRoute );
		trip.add( teleportationLeg );

		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		final CompositeStageActivityTypes stageTypes = new CompositeStageActivityTypes();

		// trips for this mode contain the ones we create, plus the ones of the
		// pt router we use.
		stageTypes.addActivityTypes( routingDelegate.get().getStageActivityTypes() );
		stageTypes.addActivityTypes( new StageActivityTypesImpl( STAGE ) );

		return stageTypes;
	}
}

