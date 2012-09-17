/* *********************************************************************** *
 * project: org.matsim.*
 * MultimodalSimulationTripRouterFactory.java
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

import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.MultiModalConfigGroup;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author thibautd
 */
public class MultimodalSimulationTripRouterFactory implements TripRouterFactory {
	private final Network network;
	private final LeastCostPathCalculatorFactory leastCostAlgoFactory;
	private final TravelDisutility travelDisutility;
	private final MultiModalConfigGroup configGroup;
	private final PopulationFactory populationFactory;
	private final Map<String, TravelTime> multimodalTravelTimes;

	public MultimodalSimulationTripRouterFactory(
			final Network network,
			final PopulationFactory populationFactory,
			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final TravelDisutility travelDisutility,
			final Map<String, TravelTime> multimodalTravelTimes,
			final MultiModalConfigGroup configGroup) {
		this.network = network;
		this.populationFactory = populationFactory;
		this.leastCostAlgoFactory = leastCostAlgoFactory;
		this.configGroup = configGroup;
		this.travelDisutility = travelDisutility;
		this.multimodalTravelTimes = multimodalTravelTimes;
	}

	@Override
	public TripRouter createTripRouter() {
		TripRouter instance = new TripRouter();

		LeastCostPathCalculator routeAlgo =
			leastCostAlgoFactory.createPathCalculator(
					network,
					travelDisutility,
					multimodalTravelTimes.get( TransportMode.car ) );
		instance.setRoutingModule(
				TransportMode.car,
				new LegRouterWrapper(
					TransportMode.car,
					populationFactory,
					new NetworkLegRouter(
						network,
						routeAlgo,
						((PopulationFactoryImpl) populationFactory).getModeRouteFactory())));

		for ( String mode : CollectionUtils.stringToArray( configGroup.getSimulatedModes() ) ) {
			routeAlgo =
				leastCostAlgoFactory.createPathCalculator(
						network,
						travelDisutility,
						multimodalTravelTimes.get( mode ) );
			instance.setRoutingModule(
					mode,
					new LegRouterWrapper(
						mode,
						populationFactory,
						new NetworkLegRouter(
							network,
							routeAlgo,
							((PopulationFactoryImpl) populationFactory).getModeRouteFactory())));
		}

		return instance;
	}
}

