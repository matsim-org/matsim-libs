/* *********************************************************************** *
 * project: org.matsim.*
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


import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.facilities.Facility;

/**
 * @author thibautd
 */
public class MyTripRouterFactory implements TripRouterFactory {
	public static final String TELEPORTATION_MAIN_MODE = "myTeleportationMainMode";

	private final Facility teleport;

	private TripRouterFactory delegate;

	private Scenario scenario;

	public MyTripRouterFactory(
			final Scenario scenario,
			final Facility teleport ) {
		this.teleport = teleport;
		// this factory initializes a TripRouter with default modules,
		// taking into account what is asked for in the config
		// (for instance, detailled or teleported pt).
		// This allows us to just add our module and go.

		this.delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(scenario);
		this.scenario = scenario;
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext iterationContext) {


		final TripRouter router = delegate.instantiateAndConfigureTripRouter(iterationContext);

		// add our module to the instance
		router.setRoutingModule(
			TELEPORTATION_MAIN_MODE,
			new MyRoutingModule(
				// the module uses the trip router for the PT part.
				// This allows to automatically adapt to user settings,
				// including if they are specified at a later stage
				// in the initialisation process.
				router,
				scenario.getPopulation().getFactory(),
				teleport));

		// we still need to provide a way to identify our trips
		// as being teleportation trips.
		// This is for instance used at re-routing.
		router.setMainModeIdentifier(
				new MyMainModeIdentifier(
					router.getMainModeIdentifier() ) );

		// we're done!
		return router;
	}
}
