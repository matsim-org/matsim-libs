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


import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryImpl;

/**
 * @author thibautd
 */
public class MyTripRouterFactory implements TripRouterFactory {
	public static final String TELEPORTATION_MAIN_MODE = "myTeleportationMainMode";

	private final Controler controler;
	private final Facility teleport;

	public MyTripRouterFactory(
			final Controler controler,
			final Facility teleport ) {
		this.controler = controler;
		this.teleport = teleport;
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter() {
		// this factory initializes a TripRouter with default modules,
		// taking into account what is asked for in the config
		// (for instance, detailled or teleported pt).
		// This allows us to just add our module and go.
		final TripRouterFactory delegate =
			new TripRouterFactoryImpl(
				controler.getScenario(),
				controler.getTravelDisutilityFactory(),
				controler.getLinkTravelTimes(),
				controler.getLeastCostPathCalculatorFactory(),
				controler.getTransitRouterFactory() );

		final TripRouter router = delegate.instantiateAndConfigureTripRouter();

		// add our module to the instance
		router.setRoutingModule(
			TELEPORTATION_MAIN_MODE,
			new MyRoutingModule(
				// use the default routing module for the
				// public transport sub-part.
				// It will adapt to the configuration (teleported,
				// simulated, user implementation...)
				router.getRoutingModule( TransportMode.pt ),
				controler.getScenario().getPopulation().getFactory(),
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
