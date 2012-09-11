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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.TransitRouterWrapper;
import playground.thibautd.router.TripRouter;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class EstimatorTripRouterFactory implements TripRouterFactory {
	private final TripRouterFactory delegate;
	private final PlansCalcRouteConfigGroup config;
	private final PlanCalcScoreConfigGroup configScore;
	private final Plan plan;
	private final PopulationFactory populationFactory;
	private final Network network;
	private final TravelTime travelTime;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final LeastCostPathCalculatorFactory leastCostPathAlgoFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final DepartureDelayAverageCalculator delay;
	private final TransitSchedule transitSchedule;

	public EstimatorTripRouterFactory(
			final Plan plan,
			final PopulationFactory populationFactory,
			final Network network,
			final TravelTime travelTime,
			final TravelDisutilityFactory travelDisutilityFactory,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final ModeRouteFactory modeRouteFactory,
			final TransitSchedule transitSchedule,
			final PlansCalcRouteConfigGroup config,
			final PlanCalcScoreConfigGroup configScore,
			final DepartureDelayAverageCalculator delay,
			final TripRouterFactory delegate) {
		this.plan = plan;
		this.config = config;
		this.configScore = configScore;
		this.delay = delay;
		this.delegate = delegate;
		this.populationFactory = populationFactory;
		this.network = network;
		this.travelTime = travelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.leastCostPathAlgoFactory = leastCostPathAlgoFactory;
		this.modeRouteFactory = modeRouteFactory;
		this.transitSchedule = transitSchedule;
	}

	@Override
	public TripRouter createTripRouter() {
		TripRouter instance = delegate.createTripRouter();

		for ( String mode : config.getNetworkModes() ) {
			instance.setRoutingModule(
					mode,
					new FixedRouteNetworkRoutingModule(
						mode,
						plan,
						populationFactory,
						network,
						travelTime,
						travelDisutilityFactory,
						leastCostPathAlgoFactory,
						modeRouteFactory,
						configScore,
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

