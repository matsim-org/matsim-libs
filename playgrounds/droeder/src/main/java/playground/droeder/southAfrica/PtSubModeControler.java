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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

import playground.droeder.southAfrica.routing.PlansCalcSubModeDependendTransitRoute;
import playground.droeder.southAfrica.routing.PtSubModeDependRouterFactory;

/**
 * @author droeder
 *
 */
public class PtSubModeControler extends Controler {
	private static final Logger log = Logger
			.getLogger(PtSubModeControler.class);
	
	/**
	 * This class is a extension of the original MATSim-Controler. It will only work with an enabled pt-simulation.
	 * It uses an own implementation of the TransitRouter and will work with the strategy-module <code>ReRouteFixedPtSubMode</code>. 
	 * @param configFile
	 */
	public PtSubModeControler(String configFile, boolean routeOnSameMode) {
		super(configFile);
		log.warn("This controler uses not the default-implementation of public transport. make sure this is what you want!");
		super.setTransitRouterFactory(new PtSubModeDependRouterFactory(this, routeOnSameMode));
		//remove default pt-RouteFactory. This just because it is unclear what should happen to "only-transitWalk"-legs
		((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
				setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		//and add new ones for modes defined in pt-module
		for(String modes: super.scenarioData.getConfig().transit().getTransitModes()){
			((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
							setRouteFactory(modes, new ExperimentalTransitRouteFactory());
		}
	}
	
	/**
	 * This class is a extension of the original MATSim-Controler. It will only work with an enabled pt-simulation.
	 * It uses an own implementation of the TransitRouter and will work with the strategy-module <code>ReRouteFixedPtSubMode</code>. 
	 * @param configFile
	 */
	public PtSubModeControler(Scenario sc, boolean routeOnSameMode) {
		super(sc);
		log.warn("This controler uses not the default-implementation of public transport. make sure this is what you want!");
		super.setTransitRouterFactory(new PtSubModeDependRouterFactory(this, routeOnSameMode));
		//remove default pt-RouteFactory. This just because it is unclear what should happen to "only-tansitWalk"-legs
		((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
				setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		//and add new ones for modes defined in pt-module
		for(String modes: super.scenarioData.getConfig().transit().getTransitModes()){
			((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
							setRouteFactory(modes, new ExperimentalTransitRouteFactory());
		}
	}
	
	@Override
	public void run(){
		if(!(super.getTransitRouterFactory() instanceof PtSubModeDependRouterFactory)){
			throw new IllegalArgumentException("TransitRouterFactory needs to be instance of PtSubModeDependRouterFactory..."); 
		}
		super.run();
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(){
		return this.createRoutingAlgorithm(super.createTravelCostCalculator(), super.getTravelTimeCalculator());
	}
	
	private boolean thrown =  false;
	@Override
	public PlansCalcSubModeDependendTransitRoute createRoutingAlgorithm(final TravelDisutility travelCosts, final PersonalizableTravelTime travelTimes) {
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
		
		// use own PlansCalcRoute
		if (this.config.scenario().isUseTransit()) {
			if(!this.thrown){
				log.warn("As simulation of public transit is enabled a leg router for transit is used. Other features, " +
						"e.g. multimodal simulation, may not work as expected. Furthermore this class uses not the " +
						"default implementation of the transitRouter! Message thrown only once!");
				this.thrown = true;
			}
			return  new PlansCalcSubModeDependendTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts,
					travelTimes, this.getLeastCostPathCalculatorFactory(),routeFactory, this.config.transit(),
					super.getTransitRouterFactory().createTransitRouter(), this.scenarioData.getTransitSchedule());
		} 
		// and make sure this controler is used for pt-simulation only!
		else{
			throw new IllegalArgumentException("this controler expects (scenario.isUseTransit() == true) ...");
		}
	}
	

	
	
}
