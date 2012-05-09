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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeFactoryWrapper;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.DefaultRoutingModuleFactory;
import playground.thibautd.router.PlanRouter;
import playground.thibautd.router.PlanRouterWrapper;
import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TransitRoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class MultiLegRoutingControler extends Controler {
	private final Map<String, RoutingModuleFactory> userDefinedRoutingModuleFactories =
		new HashMap<String, RoutingModuleFactory>();

	public MultiLegRoutingControler(final Config config) {
		super(config);
	}

	public MultiLegRoutingControler(final Scenario scenario) {
		super(scenario);
	}

	//TODO: check particular settings and handle them
	@Override
	public PlanAlgorithm createRoutingAlgorithm(final TravelDisutility travelCosts, final PersonalizableTravelTime travelTimes) {
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
	 * <b>Creates</b> a new router factory. Thus, modifying the returned instance
	 * will not modify the next returned instances.
	 * To customize the routing behaviour, the default {@link RoutingModule}s can be erased
	 * using the {@link #setRoutingModuleFactory(String, RoutingModuleFactory)}.
	 *
	 * @return a new {@link TripRouterFactory}
	 */
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

		TripRouterFactory factory =  new TripRouterFactory(
				getNetwork(),
				getTravelDisutilityFactory(),
				persFactory,
				getLeastCostPathCalculatorFactory(),
				((PopulationFactoryImpl) (getPopulation().getFactory())).getModeRouteFactory());

		// Base modules
		RoutingModuleFactory defaultFactory =
			new DefaultRoutingModuleFactory(
					getPopulation().getFactory(),
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

		// if the user defined something, erase defaults
		for (Map.Entry<String, RoutingModuleFactory> entry : userDefinedRoutingModuleFactories.entrySet()) {
			factory.setRoutingModuleFactory( entry.getKey() , entry.getValue() );
		}

		return factory;
	}

	/**
	 * Sets the {@link RoutingModuleFactory} to use to create {@link RoutingModule}s
	 * for the given main mode.
	 *
	 * @param mainMode the main mode for which the factory is to use
	 * @param factory the factory
	 * @return the previously set <b>user defined</b> factory. Nothing will be returned
	 * if the defaults were used.
	 */
	public RoutingModuleFactory setRoutingModuleFactory(
			final String mainMode,
			final RoutingModuleFactory factory) {
		return userDefinedRoutingModuleFactories.put( mainMode , factory );
	}
}

