/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRouteWithTollOrNot.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.roadpricing;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PlansCalcRouteWithTollOrNot extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private RoadPricingSchemeImpl scheme;
	private TravelTime timeCalculator;
	private ModeRouteFactory routeFactory;
	private TravelDisutility timeCostCalc;
	private Network network;
	private Config config;
	private LeastCostPathCalculatorFactory factory;

	public PlansCalcRouteWithTollOrNot(Config config, PlansCalcRouteConfigGroup configGroup, final Network network, final TravelDisutility costCalculator, final TravelTime timeCalculator,
			LeastCostPathCalculatorFactory factory, final ModeRouteFactory routeFactory, final RoadPricingSchemeImpl scheme) {
		this.config = config;
		this.network = network;
		this.factory = factory;
		this.timeCostCalc = costCalculator;
		this.routeFactory = routeFactory;
		this.scheme = scheme;
		this.timeCalculator = timeCalculator;
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			handlePlan(person, plan);
		}
	}

	private void handlePlan(Person person, Plan plan) {
		new PlansCalcRoute(config.plansCalcRoute(), network, timeCostCalc, (TravelTime) timeCostCalc, factory, routeFactory).run(plan);
		double routeCostWithAreaToll = sumNetworkModeCosts(plan) + scheme.getCosts().iterator().next().amount;
		new PlansCalcRoute(config.plansCalcRoute(), network, new TravelDisutilityIncludingToll(timeCostCalc, scheme), timeCalculator, factory, routeFactory).run(plan);
		double routeCostWithoutAreaToll = sumNetworkModeCosts(plan);
		if (routeCostWithAreaToll < routeCostWithoutAreaToll) {
			// Change the plan back to the one without toll
			new PlansCalcRoute(config.plansCalcRoute(), network, timeCostCalc, (TravelTime) timeCostCalc, factory, routeFactory).run(plan);
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

	@Override
	public void run(final Plan plan) {

	}

}

