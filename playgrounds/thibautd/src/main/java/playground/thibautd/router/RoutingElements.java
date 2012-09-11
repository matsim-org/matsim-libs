/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterFactory.java
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
package playground.thibautd.router;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Container class, providing access to the elements useful to
 * create routing modules.
 *
 * @author thibautd
 */
public class RoutingElements {
	private final Config config;
	private final Network network;
	private final TravelDisutilityFactory travelCostCalculatorFactory;
	private final TravelTimeFactory travelTimeFactory;
	private final LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final PopulationFactory populationFactory;
	private final TransitRouterFactory transitRouterFactory;
	private final TransitSchedule transitSchedule;

	public RoutingElements(final Controler controler) {
		this(
				controler.getConfig(),
				controler.getScenario().getNetwork(),
				controler.getTravelDisutilityFactory(),
				// we need the same instance over and over
				new TravelTimeFactory() {
					@Override
					public TravelTime createTravelTime() {
						return controler.getTravelTimeCalculator();
					}
				},
				controler.getLeastCostPathCalculatorFactory(),
				controler.getPopulation().getFactory(),
				((PopulationFactoryImpl) (controler.getPopulation().getFactory())).getModeRouteFactory(),
				controler.getTransitRouterFactory(),
				controler.getScenario().getTransitSchedule());
	}

	public RoutingElements(
			final Config config,
			final Network network,
			final TravelDisutilityFactory travelCostCalculatorFactory,
			final TravelTimeFactory travelTimeFactory,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final PopulationFactory populationFactory,
			final ModeRouteFactory modeRouteFactory,
			final TransitRouterFactory transitRouterFactory,
			final TransitSchedule transitSchedule) {
		this.config = config;
		this.network = network;
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
		this.travelTimeFactory = travelTimeFactory;
		this.leastCostPathAlgorithmFactory = leastCostPathAlgoFactory;
		this.modeRouteFactory = modeRouteFactory;
		this.populationFactory = populationFactory;
		this.transitRouterFactory = transitRouterFactory;
		this.transitSchedule = transitSchedule;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	public TransitRouterFactory getTransitRouterFactory() {
		return transitRouterFactory;
	}

	public TransitSchedule getTransitSchedule() {
		return transitSchedule;
	}

	public Config getConfig() {
		return config;
	}

	/**
	 * @return the registered network
	 */
	public Network getNetwork() {
		return network;
	}

	/**
	 * @return the registered travel disutility calculator
	 */
	public TravelDisutilityFactory getTravelDisutilityFactory() {
		return travelCostCalculatorFactory;
	}

	/**
	 * @return the registered travel time estimator factory
	 */
	public TravelTimeFactory getTravelTimeFactory() {
		return travelTimeFactory;
	}

	/**
	 * @return the registered mode route factory
	 */
	public ModeRouteFactory getModeRouteFactory() {
		return modeRouteFactory;
	}

	/**
	 * @return the registered {@link PopulationFactory} instance.
	 */
	public PopulationFactory getPopulationFactory() {
		return populationFactory;
	}

	/**
	 * @return the registered least cost path factory
	 */
	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return leastCostPathAlgorithmFactory;
	}
}

