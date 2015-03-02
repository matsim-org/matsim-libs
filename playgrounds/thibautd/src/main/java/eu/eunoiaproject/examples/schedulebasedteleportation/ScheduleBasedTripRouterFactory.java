/* *********************************************************************** *
 * project: org.matsim.*
 * ScheduleBasedTripRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.examples.schedulebasedteleportation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.old.LegRouterWrapper;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.pt.router.TransitRouterFactory;

/**
 * @author thibautd
 */
public class ScheduleBasedTripRouterFactory implements TripRouterFactory {
	final Scenario scenario;
	final TransitRouterFactory transitRouterFactory;
	final TripRouterFactory defaultFactory;

	public ScheduleBasedTripRouterFactory(
			final Scenario scenario) {
		this(
			null,
			scenario );
	}

	public ScheduleBasedTripRouterFactory(
			final TransitRouterFactory transitRouterFactory,
			final Scenario scenario) {
		this.scenario = scenario;
		final TripRouterFactoryBuilderWithDefaults builder =
			new TripRouterFactoryBuilderWithDefaults();

		this.transitRouterFactory =
			transitRouterFactory != null ?
				transitRouterFactory :
				builder.createDefaultTransitRouter( scenario );

		builder.setTransitRouterFactory( this.transitRouterFactory );
		this.defaultFactory = builder.build( scenario );
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(
			final RoutingContext routingContext) {
		final TripRouter tripRouter = defaultFactory.instantiateAndConfigureTripRouter( routingContext );

		final TransitRouterWrapper routingModule =
			 new TransitRouterWrapper(
					transitRouterFactory.createTransitRouter(),
					scenario.getTransitSchedule(),
					scenario.getNetwork(), // use a walk router in case no PT path is found
					LegRouterWrapper.createLegRouterWrapper(TransportMode.transit_walk, scenario.getPopulation().getFactory(), new TeleportationLegRouter(
							((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory(),
							scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get( TransportMode.walk ),
							scenario.getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor())));

		tripRouter.setRoutingModule(
				TransportMode.pt,
				routingModule );

		return tripRouter;
	}
}

