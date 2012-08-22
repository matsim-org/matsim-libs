/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingControler.java
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
package playground.thibautd.hitchiking.run;

import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeFactoryWrapper;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

import playground.thibautd.hitchiking.HitchHikingConfigGroup;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.HitchHikingSpots;
import playground.thibautd.hitchiking.HitchHikingUtils;
import playground.thibautd.hitchiking.qsim.HitchHikingQsimFactory;
import playground.thibautd.hitchiking.routing.HitchHikingDriverRoutingModuleFactory;
import playground.thibautd.hitchiking.routing.HitchHikingPassengerRoutingModuleFactory;
import playground.thibautd.hitchiking.spotweights.SpotWeighter;
import playground.thibautd.router.controler.MultiLegRoutingControler;
import playground.thibautd.router.DefaultRoutingModuleFactory;
import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TransitRoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class HitchHikingControler extends MultiLegRoutingControler {
	private static final Logger log =
		Logger.getLogger(HitchHikingControler.class);
	private final SpotWeighter spotWeighter;


	public HitchHikingControler(
			final Scenario scenario,
			final SpotWeighter spotWeighter) {
		super(scenario);
		this.spotWeighter = spotWeighter;
	}

	@Override
	protected void loadData() {
		setMobsimFactory( new HitchHikingQsimFactory( this ) );
		super.loadData();
	}

	@Override
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

		TripRouterFactory factory =  new HitchHikingTripRouterFactory(
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

		// hitch hiking specific
		HitchHikingSpots spots = HitchHikingUtils.getSpots( getScenario() );
		HitchHikingConfigGroup hhConfigGroup = HitchHikingUtils.getConfigGroup( getConfig() );
		factory.setRoutingModuleFactory(
				HitchHikingConstants.DRIVER_MODE,
				new HitchHikingDriverRoutingModuleFactory(
					spots,
					spotWeighter,
					hhConfigGroup));
		factory.setRoutingModuleFactory(
				HitchHikingConstants.PASSENGER_MODE,
				new HitchHikingPassengerRoutingModuleFactory(
					spots,
					spotWeighter,
					hhConfigGroup) );

		// if the user defined something, erase defaults
		for (Map.Entry<String, RoutingModuleFactory> entry : userDefinedRoutingModuleFactories.entrySet()) {
			factory.setRoutingModuleFactory( entry.getKey() , entry.getValue() );
		}

		return factory;
	}
}

