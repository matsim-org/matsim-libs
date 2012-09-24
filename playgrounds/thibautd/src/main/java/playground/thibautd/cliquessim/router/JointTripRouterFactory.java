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
package playground.thibautd.cliquessim.router;

import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryImpl;

import playground.thibautd.cliquessim.population.JointActingTypes;

/**
 * @author thibautd
 */
public class JointTripRouterFactory implements TripRouterFactory {
	private final TripRouterFactory defaultFactory;
	private final PopulationFactory populationFactory;

	public JointTripRouterFactory(final Controler controler) {
		defaultFactory = new TripRouterFactoryImpl( controler );
		populationFactory = ((PopulationImpl) controler.getPopulation()).getFactory();
	}

	@Override
	public TripRouter createTripRouter() {
		TripRouter instance = defaultFactory.createTripRouter();

		instance.setRoutingModule(
				JointActingTypes.DRIVER,
				new DriverRoutingModule(
					JointActingTypes.DRIVER,
					populationFactory,
					instance.getRoutingModule( TransportMode.car )));

		instance.setRoutingModule(
				JointActingTypes.PASSENGER,
				new PassengerRoutingModule(
					JointActingTypes.PASSENGER,
					populationFactory,
					((PopulationFactoryImpl) populationFactory).getModeRouteFactory()));

		return instance;
	}
}

