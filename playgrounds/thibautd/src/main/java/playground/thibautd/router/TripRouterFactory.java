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
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;

/**
 * Allows to create {@link TripRouter} instances, and gives access to factories
 * for the basic elements used for routing.
 * <br>
 * The actual result of the {@link #createTripRouter()} method can be customized
 * by providing a {@link TripRouterBuilder} to the constructor.
 *
 * @author thibautd
 */
public class TripRouterFactory implements MatsimFactory {
	private final Network network;
	private final TravelDisutilityFactory travelCostCalculatorFactory;
	private final PersonalizableTravelTimeFactory travelTimeFactory;
	private final LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory;
	private final ModeRouteFactory modeRouteFactory;

	private final Map<String, RoutingModuleFactory> routingModulesFactories =
		new HashMap<String, RoutingModuleFactory>();

	/**
	 *
	 * @param network
	 * @param travelCostCalculatorFactory
	 * @param travelTimeFactory
	 * @param leastCostPathAlgoFactory
	 * @param modeRouteFactory
	 * @param builder
	 */
	public TripRouterFactory(
			final Network network,
			final TravelDisutilityFactory travelCostCalculatorFactory,
			final PersonalizableTravelTimeFactory travelTimeFactory,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final ModeRouteFactory modeRouteFactory) {
		this.network = network;
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
		this.travelTimeFactory = travelTimeFactory;
		this.leastCostPathAlgorithmFactory = leastCostPathAlgoFactory;
		this.modeRouteFactory = modeRouteFactory;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	public Network getNetwork() {
		return network;
	}

	public TravelDisutilityFactory getTravelCostCalculatorFactory() {
		return travelCostCalculatorFactory;
	}

	public PersonalizableTravelTimeFactory getTravelTimeCalculatorFactory() {
		return travelTimeFactory;
	}

	public ModeRouteFactory getModeRouteFactory() {
		return modeRouteFactory;
	}

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
	 * @param moduleFactory the factory
	 * @return the previously registered factory, if any
	 */
	public RoutingModuleFactory setRoutingModuleFactory(
			final String mainMode,
			final RoutingModuleFactory moduleFactory) {
		return routingModulesFactories.put( mainMode , moduleFactory );
	}

	/**
	 * Creates a new {@link TripRouter} instance.
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
	 */
	protected TripRouter initRouter() {
		return new TripRouter();
	}
}

