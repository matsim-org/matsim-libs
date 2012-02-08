/* *********************************************************************** *
 * project: org.matsim.*
 * MultiLegRoutingControler.java
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
package playground.thibautd.router.controler;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.TravelTimeFactoryWrapper;

import playground.thibautd.router.PlanRouterWrapper;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class MultiLegRoutingControler extends Controler {
	public MultiLegRoutingControler(final Config config) {
		super(config);
	}

	@Override
	protected void setUp() {
		super.setUp();
	}

	//TODO: check particular settings and handle them
	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
		PlansCalcRoute plansCalcRoute = null;

		TripRouterFactory tripRouterFactory = getTripRouterFactory();
		plansCalcRoute = new PlanRouterWrapper(
				getConfig().plansCalcRoute(),
				getNetwork(),
				travelCosts,
				travelTimes,
				getLeastCostPathCalculatorFactory(),
				tripRouterFactory.getModeRouteFactory(),
				tripRouterFactory);

		return plansCalcRoute;
	}

	public TripRouterFactory getTripRouterFactory() {
		// initialise each time, as components may vary accross iterations
		TravelTimeCalculatorFactory travelTimeFactory = getTravelTimeCalculatorFactory();
		PersonalizableTravelTimeFactory persFactory;

		if ( travelTimeFactory instanceof PersonalizableTravelTimeFactory ) {
			persFactory = (PersonalizableTravelTimeFactory) travelTimeFactory;
		}
		else {
			persFactory = new TravelTimeFactoryWrapper( getTravelTimeCalculator() );
		}

		return new TripRouterFactory(
				getConfig().plansCalcRoute(),
				getConfig().planCalcScore(),
				getNetwork(),
				getTravelCostCalculatorFactory(),
				persFactory,
				getLeastCostPathCalculatorFactory(),
				((PopulationFactoryImpl) (getPopulation().getFactory())).getModeRouteFactory());
	}
}

