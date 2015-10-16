/* *********************************************************************** *
 * project: org.matsim.*
 * LinkToLinkTripRouterFactory.java
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
package org.matsim.contrib.signals.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.pt.router.TransitRouter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

/**
 * @author thibautd
 */
public class LinkToLinkTripRouterFactory implements TripRouterFactory {
	private static final Logger log =
		Logger.getLogger(LinkToLinkTripRouterFactory.class);

	private final TripRouterFactory delegate;
	private final Scenario scenario;
	private final LeastCostPathCalculatorFactory leastCostAlgoFactory;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final LinkToLinkTravelTime travelTimes;
	private final PopulationFactory populationFactory;

    @Inject
	LinkToLinkTripRouterFactory(
            Scenario scenario,
            LeastCostPathCalculatorFactory leastCostAlgoFactory,
            Map<String, TravelDisutilityFactory> travelDisutilityFactory,
            LinkToLinkTravelTime travelTimes,
            Provider<TransitRouter> transitRouterFactory) {
		this.scenario = scenario;
		this.travelDisutilityFactory = travelDisutilityFactory.get(TransportMode.car);
		this.travelTimes = travelTimes;
		this.populationFactory = scenario.getPopulation().getFactory();
		this.delegate = new DefaultTripRouterFactoryImpl(scenario, leastCostAlgoFactory, transitRouterFactory);
        this.leastCostAlgoFactory = leastCostAlgoFactory;
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext iterationContext) {
		TripRouter instance = delegate.instantiateAndConfigureTripRouter(iterationContext);

		instance.setRoutingModule(
				TransportMode.car,
				InvertedNetworkRoutingModule.createInvertedNetworkRouter(TransportMode.car, populationFactory,
						scenario,
						leastCostAlgoFactory,
						travelDisutilityFactory,
						travelTimes));
		log.warn("Link to link routing only affects car legs, which is correct if turning move costs only affect rerouting of car legs.");

		return instance;
	}
}

