/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatorTripRouterFactory.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser.traveltimeestimation;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Provider;


/**
 * @author thibautd
 */
public class EstimatorTripRouterFactory implements Provider<TripRouter> {
	private final Provider<TripRouter> delegate;
	private final PlansCalcRouteConfigGroup config;
	private final Plan plan;
	private final PopulationFactory populationFactory;
	private final Network network;
	private final TravelTime travelTime;
	private final LeastCostPathCalculatorFactory leastCostPathAlgoFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final DepartureDelayAverageCalculator delay;
	private final TransitSchedule transitSchedule;

	public EstimatorTripRouterFactory(
			final Plan plan,
			final PopulationFactory populationFactory,
			final Network network,
			final TravelTime travelTime,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final ModeRouteFactory modeRouteFactory,
			final TransitSchedule transitSchedule,
			final PlansCalcRouteConfigGroup config,
			final DepartureDelayAverageCalculator delay,
			final Provider<TripRouter> delegate) {
		this.plan = plan;
		this.config = config;
		this.delay = delay;
		this.delegate = delegate;
		this.populationFactory = populationFactory;
		this.network = network;
		this.travelTime = travelTime;
		this.leastCostPathAlgoFactory = leastCostPathAlgoFactory;
		this.modeRouteFactory = modeRouteFactory;
		this.transitSchedule = transitSchedule;
	}

	@Override
	public TripRouter get() {
		TripRouter instance = delegate.get();

		for ( String mode : config.getNetworkModes() ) {
			instance.setRoutingModule(
					mode,
					new FixedRouteNetworkRoutingModule(
						mode,
						plan,
						populationFactory,
						network,
						travelTime,
						leastCostPathAlgoFactory,
						modeRouteFactory,
						delay,
						// TODO: import from somewhere, or detect from the mobsim
						false,
						true ));
		}

		RoutingModule pt = instance.getRoutingModule( TransportMode.pt );
		if (pt instanceof TransitRouterWrapper) {
			instance.setRoutingModule(
					TransportMode.pt,
					new FixedTransitRouteRoutingModule(
						plan,
						transitSchedule,
						(TransitRouterWrapper) pt ));
		}



		return instance;
	}

}

