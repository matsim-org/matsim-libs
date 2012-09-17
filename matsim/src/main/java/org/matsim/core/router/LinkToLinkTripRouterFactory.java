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
package org.matsim.core.router;

import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.InvertedNetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

/**
 * @author thibautd
 */
public class LinkToLinkTripRouterFactory implements TripRouterFactory {
	private final TripRouterFactory delegate;
	private final Scenario scenario;
	private final LeastCostPathCalculatorFactory leastCostAlgoFactory;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTimes;
	private final PopulationFactory populationFactory;

	public LinkToLinkTripRouterFactory(
			final Scenario scenario,
			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final TravelDisutilityFactory travelDisutilityFactory,
			final TravelTime travelTimes,
			final PopulationFactory populationFactory,
			final TripRouterFactory delegate) {
		this.scenario = scenario;
		this.leastCostAlgoFactory = leastCostAlgoFactory;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.travelTimes = travelTimes;
		this.populationFactory = populationFactory;
		this.delegate = delegate;
	}

	@Override
	public TripRouter createTripRouter() {
		TripRouter instance = delegate.createTripRouter();

		//Note that the inverted network is created once per thread
		InvertedNetworkLegRouter invertedNetLegRouter =
			new InvertedNetworkLegRouter(
					scenario,
					leastCostAlgoFactory,
					travelDisutilityFactory,
					travelTimes);
		instance.setRoutingModule(
				TransportMode.car,
				new LegRouterWrapper(
					TransportMode.car,
					populationFactory,
					invertedNetLegRouter));
		log.warn("Link to link routing only affects car legs, which is correct if turning move costs only affect rerouting of car legs.");

		return instance;
	}
}

