/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.droeder.southAfrica;


import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

import playground.droeder.southAfrica.replanning.PtSubModeDependRouterFactory;

/**
 * @author droeder
 *
 */
public class FixedPtSubModeControler extends Controler {
	private static final Logger log = Logger
			.getLogger(FixedPtSubModeControler.class);
	

	public FixedPtSubModeControler(String configFile) {
		super(configFile);
		log.warn("This controler uses not the default-implementation of public transport. maake sure this is what you want!");
		super.setTransitRouterFactory(new PtSubModeDependRouterFactory());
		for(String modes: super.scenarioData.getConfig().transit().getTransitModes()){
			((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
							setRouteFactory(modes, new ExperimentalTransitRouteFactory());
		}
	}
	
	@Override
	public void run(){
		super.run();
	}
	
	@Override
	public PlanAlgorithm createRoutingAlgorithm(final TravelDisutility travelCosts, final PersonalizableTravelTime travelTimes) {
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
		
		// use own method
		if (this.config.scenario().isUseTransit()) {
			log.warn("As simulation of public transit is enabled a leg router for transit is used. Other features, " +
					"e.g. multimodal simulation, may not work as expected.");
			return  new PlansCalcSubModeDependendTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts,
					travelTimes, this.getLeastCostPathCalculatorFactory(),routeFactory, this.config.transit(),
					super.getTransitRouterFactory().createTransitRouter(), this.scenarioData.getTransitSchedule());
		} else{
			throw new IllegalArgumentException("this controler expects scenario.isUseTransit() == true...");
		}
	}
	

	
	
}
