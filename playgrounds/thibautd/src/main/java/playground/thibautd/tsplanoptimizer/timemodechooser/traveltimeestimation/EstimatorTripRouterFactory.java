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

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;

import playground.thibautd.router.RoutingElements;
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
	private final RoutingElements data;
	private final DepartureDelayAverageCalculator delay;

	public EstimatorTripRouterFactory(
			final Plan plan,
			final RoutingElements data,
			final PlansCalcRouteConfigGroup config,
			final PlanCalcScoreConfigGroup configScore,
			final DepartureDelayAverageCalculator delay,
			final TripRouterFactory delegate) {
		this.data = data;
		this.plan = plan;
		this.config = config;
		this.configScore = configScore;
		this.delay = delay;
		this.delegate = delegate;
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
						data,
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
						data.getTransitSchedule(),
						(TransitRouterWrapper) pt ));
		}



		return instance;
	}

}

