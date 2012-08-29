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
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTimeFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

/**
 * Allows to create {@link TripRouter} instances, and gives access to factories
 * for the basic elements used for routing.
 * <br>
 * The actual result of the {@link #createTripRouter()} method can be customized
 * by changing the registered factories (see the <tt>setSomething<tt> methods below).
 * <br>
 * Moreover, this class provides access to its member fields, in case they
 * would be needed to initialise a routing module.
 *
 * @author thibautd
 */
public class TripRouterFactory implements MatsimFactory {
	private final Network network;
	private final TravelDisutilityFactory travelCostCalculatorFactory;
	private final TravelTimeFactory travelTimeFactory;
	private final LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final PopulationFactory populationFactory;

	private final Map<String, RoutingModuleFactory> routingModulesFactories =
		new HashMap<String, RoutingModuleFactory>();

	/**
	 * Initialises an instance, with an empty list of routing modules.
	 *
	 * @param network the network to route on.
	 * @param travelCostCalculatorFactory the factory for the travel disutility
	 * calculator used in netwrok-based routing.
	 * @param travelTimeFactory the factory for the travel time estimator used in
	 * network-based routing. Typically, it will return always the same instance
	 * of a {@link TravelTimeCalculator}.
	 * @param leastCostPathAlgoFactory the factory to use to get least-cost path
	 * algorithms
	 * @param modeRouteFactory the {@link ModeRouteFactory} to use to create routes.
	 * Note that it is not guaranteed that all routing modules will use it...
	 */
	public TripRouterFactory(
			final Network network,
			final TravelDisutilityFactory travelCostCalculatorFactory,
			final TravelTimeFactory travelTimeFactory,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final PopulationFactory populationFactory,
			final ModeRouteFactory modeRouteFactory) {
		this.network = network;
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
		this.travelTimeFactory = travelTimeFactory;
		this.leastCostPathAlgorithmFactory = leastCostPathAlgoFactory;
		this.modeRouteFactory = modeRouteFactory;
		this.populationFactory = populationFactory;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
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

	/**
	 * Gives access to a map view of the registered {@link RoutingModuleFactory}s.
	 * @return an immutable map linking main modes to {@link RoutingModuleFactory}
	 */
	public Map<String, RoutingModuleFactory> getRoutingModuleFactories() {
		return Collections.unmodifiableMap( routingModulesFactories );
	}

	// /////////////////////////////////////////////////////////////////////////
	// factory methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Sets a factory for creating {@link RoutingModule}s for a given mode.
	 * @param mainMode the main mode
	 * @param moduleFactory the factory to associate to this mode
	 * @return the previously registered factory, if any
	 */
	public RoutingModuleFactory setRoutingModuleFactory(
			final String mainMode,
			final RoutingModuleFactory moduleFactory) {
		return routingModulesFactories.put( mainMode , moduleFactory );
	}

	/**
	 * Creates a new {@link TripRouter} instance, using the registered
	 * {@link RoutingModuleFactory}es.
	 * @return a fully initialised {@link TripRouter}.
	 */
	public TripRouter createTripRouter() {
		TripRouter tripRouter = initRouter();

		for (Map.Entry<String, RoutingModuleFactory> entry : routingModulesFactories.entrySet()) {
			tripRouter.setRoutingModule(
					entry.getKey(),
					entry.getValue().createModule(
						entry.getKey(),
						this ));
		}

		return tripRouter;
	}

	/**
	 * This method is provided so that custom TripRouter
	 * implementations can be used.
	 * This should only be used to change the way the "main mode"
	 * is detected.
	 * <br>
	 * This method is called from {@link #createTripRouter()}, to
	 * obtain the instance to configure and than return.
	 *
	 * @return a new {@link TripRouter} instance, with no routing modules
	 * set.
	 */
	protected TripRouter initRouter() {
		return new TripRouter();
	}
}

