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

package playground.mmoyo.ptRouterAdapted.replanning;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.config.TransitConfigGroup;

import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import playground.mmoyo.ptRouterAdapted.precalculation.PrecalPlansCalcTransitRoute;

public class AdapPlanStrategyModule extends AbstractMultithreadedModule{ //implements PlanStrategyModule, ActivityEndEventHandler { // this is just there as an example
	private static final Logger log = Logger.getLogger(AdapPlanStrategyModule.class);
	private Controler controler;
	
	public AdapPlanStrategyModule(Controler controler) {
		super(controler.getConfig().global());
		this.controler = controler ;
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		log.info("Creating adaptedRouter algo.");
		Config config =  this.controler.getConfig();
		FreespeedTravelTimeCost freespeedTravelTimeCost = new FreespeedTravelTimeCost(config.planCalcScore());
			
		MyTransitRouterConfig myTransitRouterConfig = new MyTransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental() );
		myTransitRouterConfig.beelineWalkConnectionDistance = 300.0;  			//distance to search stations when transfering
		myTransitRouterConfig.setBeelineWalkSpeed(3.0/3.6);  						// presumably, in m/sec.  3.0/3.6 = 3000/3600 = 3km/h.  kai, apr'10
		myTransitRouterConfig.setMarginalUtilityOfTravelTimeWalk_utl_s(-6.0 / 3600.0); 	//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
		myTransitRouterConfig.setMarginalUtilityOfTravelTimePt_utl_s(-6.0 / 3600.0);//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
		myTransitRouterConfig.setMarginalUtilityOfTravelDistancePt_utl_m(-0.7/1000.0); //-0.7/1000.0;    // yyyy presumably, in Eu/m ?????????  so far, not used.  kai, apr'10
//		myTransitRouterConfig.setUtilityOfLineSwitch_utl(240.0 * - myTransitRouterConfig.getEffectiveMarginalUtilityOfTravelTimePt_utl_s());	//* -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle  // in Eu.  kai, apr'10
		myTransitRouterConfig.setUtilityOfLineSwitch_utl(240.0 * myTransitRouterConfig.getMarginalUtilityOfTravelTimePt_utl_s());	//* -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle  // in Eu.  kai, apr'10
		myTransitRouterConfig.searchRadius = 600.0;								//initial distance for stations around origin and destination points
		myTransitRouterConfig.extensionRadius = 200.0; 
		myTransitRouterConfig.allowDirectWalks= true;
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.controler.getPopulation().getFactory()).getModeRouteFactory();
		
		//normal
		//AdaptedPlansCalcTransitRoute adaptedPlansCalcTransitRoute = new AdaptedPlansCalcTransitRoute(config.plansCalcRoute(), this.controler.getScenario().getNetwork(), freespeedTravelTimeCost, freespeedTravelTimeCost,  new DijkstraFactory(), this.controler.getScenario().getTransitSchedule(), new TransitConfigGroup(), myTransitRouterConfig);
		
		//with a set of precalculated routes 
		PrecalPlansCalcTransitRoute adaptedPlansCalcTransitRoute = new PrecalPlansCalcTransitRoute(config.plansCalcRoute(), 
				this.controler.getScenario().getNetwork(), freespeedTravelTimeCost, freespeedTravelTimeCost,  
				new DijkstraFactory(), routeFactory, this.controler.getScenario().getTransitSchedule(), new TransitConfigGroup(), 
				myTransitRouterConfig);
		
		return adaptedPlansCalcTransitRoute;
	}	
}