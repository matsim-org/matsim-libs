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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.MultiModalConfigGroup;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.MultiModalLegRouter;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapper;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author thibautd
 */
public class MultimodalSimulationTripRouterFactory implements TripRouterFactory {
	private final Network network;
	private final LeastCostPathCalculatorFactory leastCostAlgoFactory;
	private final MultiModalTravelTimeWrapper travelTime;
	private final TravelDisutility travelDisutility;
	private final MultiModalConfigGroup configGroup;
	private final PopulationFactory populationFactory;

	public MultimodalSimulationTripRouterFactory(
			final Network network,
			final PopulationFactory populationFactory,
			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final TravelDisutility travelDisutility,
			final MultiModalTravelTimeWrapper travelTime,
			final MultiModalConfigGroup configGroup) {
		this.network = network;
		this.populationFactory = populationFactory;
		this.leastCostAlgoFactory = leastCostAlgoFactory;
		this.travelTime = travelTime;
		this.configGroup = configGroup;
		this.travelDisutility = travelDisutility;
	}

	@Override
	public TripRouter createTripRouter() {
		TripRouter instance = new TripRouter();

		/*
		 * A MultiModalTravelTime calculator is used. Before creating a route for a given
		 * leg, the leg's mode has to be set in the travel time and travel cost objects.
		 * This is not done by the LegHandler used by default in PlansCalcRoute. Therefore,
		 * we have to use a multiModalLegHandler.
		 */
		LegRouter legHandler =
			new MultiModalLegRouter(
					network,
					travelTime,
					(IntermodalLeastCostPathCalculator)
						leastCostAlgoFactory.createPathCalculator(
							network,
							travelDisutility,
							travelTime ));

		instance.setRoutingModule(
				TransportMode.car,
				new LegRouterWrapper(
					TransportMode.car,
					populationFactory,
					legHandler));

		for ( String mode : CollectionUtils.stringToArray( configGroup.getSimulatedModes() ) ) {
			instance.setRoutingModule(
					mode,
					new LegRouterWrapper(
						mode,
						populationFactory,
						legHandler));
		}

		return instance;
	}
}

