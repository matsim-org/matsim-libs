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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.DefaultRoutingModuleFactory;
import playground.thibautd.router.PlanRouter;
import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TransitRoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class MultiLegRoutingControler extends Controler {
	private TripRouterFactory tripRouterFactory;

	public MultiLegRoutingControler(final Config config) {
		super(config);
	}

	public MultiLegRoutingControler(final Scenario scenario) {
		super(scenario);
	}

	//TODO: check particular settings and handle them
	@Override
	public PlanAlgorithm createRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
		PlansCalcRoute plansCalcRoute = null;

		TripRouterFactory tripRouterFactory = getTripRouterFactory();
		//plansCalcRoute = new PlanRouterWrapper(
		//		getConfig().plansCalcRoute(),
		//		getNetwork(),
		//		travelCosts,
		//		travelTimes,
		//		getLeastCostPathCalculatorFactory(),
		//		tripRouterFactory.getModeRouteFactory(),
		//		tripRouterFactory);

		//return plansCalcRoute;
		return new PlanRouter(
				tripRouterFactory.createTripRouter(),
				getScenario().getActivityFacilities());
	}

	/**
	 * creates an uninitialized trip router factory.
	 * Allows to change easily the implementation of the trip router factory,
	 * whithout having to reimplement the initialization routine.
	 */
	protected TripRouterFactory createUninitializedTripRouterFactory() {
		 return new TripRouterFactory(
				getNetwork(),
				getTravelDisutilityFactory(),
				new TravelTimeFactory() {
					@Override
					public TravelTime createTravelTime() {
						return getTravelTimeCalculator();
					}
				},
				getLeastCostPathCalculatorFactory(),
				getPopulation().getFactory(),
				((PopulationFactoryImpl) (getPopulation().getFactory())).getModeRouteFactory());
	}

	protected void setUpTripRouterFactory(final TripRouterFactory factory) {
		// Base modules
		RoutingModuleFactory defaultFactory =
			new DefaultRoutingModuleFactory(
					getConfig().plansCalcRoute(),
					getConfig().planCalcScore());

		for (String mode : DefaultRoutingModuleFactory.HANDLED_MODES) {
			factory.setRoutingModuleFactory( mode , defaultFactory );
		}

		// PT module: if use transit, erase default
		if (getConfig().scenario().isUseTransit()) {
			factory.setRoutingModuleFactory(
					TransportMode.pt,
					new TransitRoutingModuleFactory(getTransitRouterFactory(), getScenario().getTransitSchedule()));
		}
	}

	/**
	 * Returns the (only) trip router factory instance for this controler.
	 * The instance is created at the first call of this method.
	 * <br>
	 * The fact of having one only instance simplifies the custom configuration
	 * (you just have to tune this instance to your needs) but has the drawback
	 * that a change in the controler (for example, a change of the travelDisutilityFactory)
	 * will <b>not</b> be reflected in the router.
	 *
	 * @return the {@link TripRouterFactory} instance
	 */
	public TripRouterFactory getTripRouterFactory() {
		if (tripRouterFactory == null) {
			tripRouterFactory = createUninitializedTripRouterFactory();
			setUpTripRouterFactory( tripRouterFactory );
		}
		return tripRouterFactory;
	}
}

