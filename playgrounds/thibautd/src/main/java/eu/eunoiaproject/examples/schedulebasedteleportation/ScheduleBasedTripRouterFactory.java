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
import org.matsim.core.router.*;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.pt.router.TransitRouter;

import javax.inject.Provider;

/**
 * @author thibautd
 */
public class ScheduleBasedTripRouterFactory implements Provider<TripRouter> {
	final Scenario scenario;
	final Provider<TransitRouter> transitRouterFactory;
	final Provider<TripRouter> defaultFactory;

	public ScheduleBasedTripRouterFactory(
			final Scenario scenario) {
		this(
			null,
			scenario );
	}

	public ScheduleBasedTripRouterFactory(
			final Provider<TransitRouter> transitRouterFactory,
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
	public TripRouter get() {
		final TripRouter tripRouter = defaultFactory.get();

		final TransitRouterWrapper routingModule =
			 new TransitRouterWrapper(
					transitRouterFactory.get(),
					scenario.getTransitSchedule(),
					scenario.getNetwork(), // use a walk router in case no PT path is found
					DefaultRoutingModules.createTeleportationRouter( TransportMode.transit_walk, scenario.getPopulation().getFactory(), 
							scenario.getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ) ));

		tripRouter.setRoutingModule(
				TransportMode.pt,
				routingModule );

		return tripRouter;
	}
}

