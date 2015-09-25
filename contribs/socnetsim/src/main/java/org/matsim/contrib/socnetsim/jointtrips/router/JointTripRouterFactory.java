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
package org.matsim.contrib.socnetsim.jointtrips.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterProviderImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;
import com.google.inject.Inject;

import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;

import javax.inject.Provider;
import java.util.Map;

/**
 * @author thibautd
 */
public class JointTripRouterFactory implements Provider<TripRouter> {
	private static final Logger log = Logger.getLogger(JointTripRouterFactory.class);
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
			final Map<String, TravelDisutilityFactory> disutilityFactory,
			final Map<String, TravelTime> travelTime,
			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final Provider<TransitRouter> transitRouterFactory) {
		this(
			new TripRouterProviderImpl(
					scenario,
					disutilityFactory.get( TransportMode.car ),
					travelTime.get( TransportMode.car ),
					leastCostAlgoFactory,
					transitRouterFactory),
			scenario.getPopulation().getFactory() );
		log.warn( getClass().getName()+" uses "+TripRouterProviderImpl.class.getName()+" with travel time for car. This is not anymore the default." );
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

