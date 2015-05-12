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
package playground.thibautd.socnetsim.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterProviderImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;
import com.google.inject.Inject;

import playground.thibautd.socnetsim.population.JointActingTypes;

import javax.inject.Provider;

/**
 * @author thibautd
 */
public class JointTripRouterFactory implements Provider<TripRouter> {
	private final Provider<TripRouter> defaultFactory;
	private final PopulationFactory populationFactory;

	public JointTripRouterFactory(
			final Provider<TripRouter> defaultFactory,
			final PopulationFactory populationFactory) {
		this.defaultFactory = defaultFactory;
		this.populationFactory = populationFactory;
	}

	@Inject
	public JointTripRouterFactory(
			final Scenario scenario,
			final TravelDisutilityFactory disutilityFactory,
			final TravelTime travelTime,
			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final Provider<TransitRouter> transitRouterFactory) {
		this(
			new TripRouterProviderImpl(
					scenario,
					disutilityFactory,
					travelTime,
					leastCostAlgoFactory,
					transitRouterFactory),
			scenario.getPopulation().getFactory() );
	}

	@Override
	public TripRouter get() {
		TripRouter instance = defaultFactory.get();

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
					populationFactory));

		return instance;
	}
}

