/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlansCalcRouteWithTollOrNot.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * *********************************************************************** 
 */

package org.matsim.roadpricing;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Inject;

public class PlansCalcRouteWithTollOrNot implements PlanAlgorithm {

	private Config config;
	private RoadPricingScheme roadPricingScheme;
	private TripRouterFactory tripRouterFactory;
	private TravelDisutilityFactory travelDisutilityFactory;
	private TravelTime travelTime;

	@Inject
	PlansCalcRouteWithTollOrNot(Config config, RoadPricingScheme roadPricingScheme, TripRouterFactory tripRouterFactory, TravelDisutilityFactory travelDisutilityFactory, TravelTime travelTime) {
		this.config = config;
		this.roadPricingScheme = roadPricingScheme;
		this.tripRouterFactory = tripRouterFactory;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.travelTime = travelTime;
	}

	@Override
	public void run(final Plan plan) {
		handlePlan(plan);
	}

	private void handlePlan(Plan plan) {
		// This calculates a best-response plan from the two options, paying area toll or not.
		// From what I understand, it may be simpler/better to just throw a coin and produce
		// one of the two options.
		TravelDisutility untolledTravelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime, config.planCalcScore());
		RoutingContextImpl untolledRoutingContext = new RoutingContextImpl(untolledTravelDisutility, travelTime);
		PlanRouter untolledPlanRouter = new PlanRouter(tripRouterFactory.instantiateAndConfigureTripRouter(untolledRoutingContext));
		untolledPlanRouter.run(plan);
		double routeCostWithAreaToll = sumNetworkModeCosts(plan) + roadPricingScheme.getTypicalCosts().iterator().next().amount;
		RoutingContextImpl tolledRoutingContext = new RoutingContextImpl(
				new TravelDisutilityIncludingToll(untolledTravelDisutility, roadPricingScheme, config),
				travelTime);
		new PlanRouter(tripRouterFactory.instantiateAndConfigureTripRouter(tolledRoutingContext)).run(plan);
		double routeCostWithoutAreaToll = sumNetworkModeCosts(plan);
		if (routeCostWithAreaToll < routeCostWithoutAreaToll) {
			// Change the plan back to the one without toll
			untolledPlanRouter.run(plan);
		}
	}

	private double sumNetworkModeCosts(Plan plan) {
		double sum = 0.0;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getRoute() instanceof NetworkRoute) {
					NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
					sum += networkRoute.getTravelCost();
				}
			}
		}
		return sum;
	}

}

