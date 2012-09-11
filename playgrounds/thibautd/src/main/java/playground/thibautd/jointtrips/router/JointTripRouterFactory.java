/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripRouterFactory.java
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
package playground.thibautd.jointtrips.router;

import org.matsim.api.core.v01.TransportMode;

import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.router.RoutingElements;
import playground.thibautd.router.TripRouter;
import playground.thibautd.router.TripRouterFactory;
import playground.thibautd.router.TripRouterFactoryImpl;

/**
 * @author thibautd
 */
public class JointTripRouterFactory implements TripRouterFactory {
	private final TripRouterFactory defaultFactory;
	private final RoutingElements data;

	public JointTripRouterFactory(final RoutingElements data) {
		this.data = data;
		defaultFactory = new TripRouterFactoryImpl( data );
	}

	@Override
	public TripRouter createTripRouter() {
		TripRouter instance = defaultFactory.createTripRouter();

		instance.setRoutingModule(
				JointActingTypes.DRIVER,
				new DriverRoutingModule(
					JointActingTypes.DRIVER,
					data.getPopulationFactory(),
					instance.getRoutingModule( TransportMode.car )));

		instance.setRoutingModule(
				JointActingTypes.PASSENGER,
				new PassengerRoutingModule(
					JointActingTypes.PASSENGER,
					data.getPopulationFactory(),
					data.getModeRouteFactory()));

		return instance;
	}
}

