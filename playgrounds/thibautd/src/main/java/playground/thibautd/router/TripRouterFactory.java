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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;

import playground.thibautd.router.TripRouterFactory;

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
	private final TravelCostCalculatorFactory travelCostCalculatorFactory;
	private final PersonalizableTravelTimeFactory travelTimeFactory;
	private final LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final TripRouterBuilder builder;

	/**
	 * Default constructor: uses a {@link TripRouterBuilderImpl} to configure
	 * the {@link TripRouter}.
	 *
	 * @param routeConfigGroup
	 * @param scoreConfigGroup
	 * @param network
	 * @param travelCostCalculatorFactory
	 * @param travelTimeFactory
	 * @param leastCostPathAlgoFactory
	 * @param modeRouteFactory
	 */
	public TripRouterFactory(
			final PlansCalcRouteConfigGroup routeConfigGroup,
			final PlanCalcScoreConfigGroup scoreConfigGroup,
			final Network network,
			final TravelCostCalculatorFactory travelCostCalculatorFactory,
			final PersonalizableTravelTimeFactory travelTimeFactory,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final ModeRouteFactory modeRouteFactory) {
		this( network,
				travelCostCalculatorFactory,
				travelTimeFactory,
				leastCostPathAlgoFactory,
				modeRouteFactory,
				new TripRouterBuilderImpl( routeConfigGroup , scoreConfigGroup ));
	}


	/**
	 * Configurable constructor: the {@link TripRouterBuilder} can be specified.
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
			final TravelCostCalculatorFactory travelCostCalculatorFactory,
			final PersonalizableTravelTimeFactory travelTimeFactory,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final ModeRouteFactory modeRouteFactory,
			final TripRouterBuilder builder) {
		this.network = network;
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
		this.travelTimeFactory = travelTimeFactory;
		this.leastCostPathAlgorithmFactory = leastCostPathAlgoFactory;
		this.modeRouteFactory = modeRouteFactory;
		this.builder = builder;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	public Network getNetwork() {
		return network;
	}

	public TravelCostCalculatorFactory getTravelCostCalculatorFactory() {
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

	// /////////////////////////////////////////////////////////////////////////
	// factory methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Creates a new {@link TripRouter} instance.
	 * @return a fully initialised {@link TripRouter}.
	 */
	public TripRouter createTripRouter() {
		TripRouter tripRouter = new TripRouter();

		builder.setModeHandlers( this , tripRouter );

		return tripRouter;
	}
}

