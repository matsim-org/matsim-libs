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

package playground.droeder.southAfrica.old.run;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

import playground.droeder.southAfrica.old.routing.PlansCalcSubModeTransitRoute;
import playground.droeder.southAfrica.old.routing.PtSubModeRouterFactoryOld;
import playground.droeder.southAfrica.qSimHook.TransitSubModeQSimFactory;

/**
 * @author droeder
 *
 */
public final class PtSubModeControlerOld extends Controler {
	private static final Logger log = Logger
			.getLogger(PtSubModeControlerOld.class);
	
	/**
	 * This class is a extension of the original MATSim-Controler. It will only work with an enabled pt-simulation.
	 * It uses an own implementation of the TransitRouter and will work with the strategy-module <code>ReRouteFixedPtSubMode</code>. 
	 * @param configFile
	 */
	public PtSubModeControlerOld(String configFile, boolean routeOnSameMode) {
		super(configFile);
		//necessary for departure-handling
		super.setMobsimFactory(new TransitSubModeQSimFactory(routeOnSameMode));
		log.warn("This controler uses not the default-implementation of public transport. make sure this is what you want!");
		super.setTransitRouterFactory(new PtSubModeRouterFactoryOld(this, routeOnSameMode));
		//remove default pt-RouteFactory. This just because it is unclear what should happen to "only-transitWalk"-legs
		((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
				setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		//and add new ones for modes defined in pt-module
		for(String modes: super.scenarioData.getConfig().transit().getTransitModes()){
			((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
							setRouteFactory(modes, new ExperimentalTransitRouteFactory());
		}

		throw new RuntimeException("overriding of the run() method is no longer possible.  if this is needed, " +
		"please talk to me for alternatives.  There is also no test for this execution path, otherwise I might have tried fixing it myself. Thanks, kai, feb'13") ;
//		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}
	
	/**
	 * This class is a extension of the original MATSim-Controler. It will only work with an enabled pt-simulation.
	 * It uses an own implementation of the TransitRouter and will work with the strategy-module <code>ReRouteFixedPtSubMode</code>. 
	 * @param configFile
	 */
	public PtSubModeControlerOld(Scenario sc, boolean routeOnSameMode) {
		super(sc);
		//necessary for departure-handling
		super.setMobsimFactory(new TransitSubModeQSimFactory(routeOnSameMode));
		log.warn("This controler uses not the default-implementation of public transport. make sure this is what you want!");
		super.setTransitRouterFactory(new PtSubModeRouterFactoryOld(this, routeOnSameMode));
		//remove default pt-RouteFactory. This just because it is unclear what should happen to "only-tansitWalk"-legs
		((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
				setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		//and add new ones for modes defined in pt-module
		for(String modes: super.scenarioData.getConfig().transit().getTransitModes()){
			((PopulationFactoryImpl)super.getScenario().getPopulation().getFactory()).
							setRouteFactory(modes, new ExperimentalTransitRouteFactory());
		}
		
		throw new RuntimeException("overriding of the run() method is no longer possible.  if this is needed, " +
		"please talk to me for alternatives.  There is also no test for this execution path, otherwise I might have tried fixing it myself. Thanks, kai, feb'13") ;
	}
	
//	@Override
//	public void run(){
//		if(!(super.getTransitRouterFactory() instanceof PtSubModeRouterFactoryOld)){
//			throw new IllegalArgumentException("TransitRouterFactory needs to be instance of PtSubModeDependRouterFactory..."); 
//		}
//		if(!(super.getMobsimFactory() instanceof TransitSubModeQSimFactory)){
//			throw new IllegalArgumentException("QSIMFactory needs to be instance of TransitSubModeQsimFactory...");
//		}
//		// need to add the PtSubmodeDependRouterFactory as last to controlerlistener, so it is explicitly called last, after all changes in schedule are done...
//		super.addControlerListener((PtSubModeRouterFactoryOld)super.getTransitRouterFactory());
//		super.run();
//	}

//	@Override
//	public PlanAlgorithm createRoutingAlgorithm(){
//		return this.createRoutingAlgorithm(super.createTravelCostCalculator(), super.getLinkTravelTimes());
//	}
	
	private boolean thrown =  false;

	public PlansCalcSubModeTransitRoute createRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
		
		// use own PlansCalcRoute
		if (this.config.scenario().isUseTransit()) {
			if(!this.thrown){
				log.warn("As simulation of public transit is enabled a leg router for transit is used. Other features, " +
						"e.g. multimodal simulation, may not work as expected. Furthermore this class uses not the " +
						"default implementation of the transitRouter! Message thrown only once!");
				this.thrown = true;
			}
			return  new PlansCalcSubModeTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts,
					travelTimes, this.getLeastCostPathCalculatorFactory(),routeFactory, this.config.transit(),
					super.getTransitRouterFactory().createTransitRouter(), this.scenarioData.getTransitSchedule());
		} 
		// and make sure this controler is used for pt-simulation only!
		else{
			throw new IllegalArgumentException("this controler expects (scenario.isUseTransit() == true) ...");
		}
	}
	

	
	
}
