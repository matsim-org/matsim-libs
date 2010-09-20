/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptedControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.ptRouterAdapted;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.config.TransitConfigGroup;
import playground.mmoyo.ptRouterAdapted.AdaptedPlansCalcTransitRoute;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import playground.mmoyo.utils.TransScenarioLoader;
//import playground.mzilske.bvg09.TransitControler;

/**
 * @author manuel
 * 
 * runs transit simulation with adapter router
  */
public class AdaptedControler extends Controler {

	public AdaptedControler(final ScenarioImpl scenario) {
		super(scenario);
	}
	
	public AdaptedControler(final Config config) {
		super(config);
	}
	
	//loads the AdaptedStrategyManagerConfigLoader to get the MmoyoTimeAllocationMutatorReRoute strategy
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		AdaptedStrategyManagerConfigLoader.load(this, manager);
		return manager;
	}
		
	//creates the Adapted pt routing algorithm
	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
		Config config = this.getScenario().getConfig();
		
		FreespeedTravelTimeCost freespeedTravelTimeCost = new FreespeedTravelTimeCost(config.charyparNagelScoring());
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		TransitConfigGroup transitConfig = new TransitConfigGroup(); 
			
		MyTransitRouterConfig myTransitRouterConfig = new MyTransitRouterConfig();
		myTransitRouterConfig.beelineWalkConnectionDistance = 300.0;  			//distance to search stations when transfering
		myTransitRouterConfig.beelineWalkSpeed = 3.0/3.6;  						// presumably, in m/sec.  3.0/3.6 = 3000/3600 = 3km/h.  kai, apr'10
		myTransitRouterConfig.marginalUtilityOfTravelTimeWalk = -6.0 / 3600.0; 	//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
		myTransitRouterConfig.marginalUtilityOfTravelTimeTransit = -6.0 / 3600.0;//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
		myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit = -0.7/1000.0; //-0.7/1000.0;    // yyyy presumably, in Eu/m ?????????  so far, not used.  kai, apr'10
		myTransitRouterConfig.costLineSwitch = 240.0 * - myTransitRouterConfig.marginalUtilityOfTravelTimeTransit;	//* -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle  // in Eu.  kai, apr'10
		myTransitRouterConfig.searchRadius = 600.0;								//initial distance for stations around origin and destination points
		myTransitRouterConfig.extensionRadius = 200.0; 
		myTransitRouterConfig.allowDirectWalks= true;
		
		AdaptedPlansCalcTransitRoute adaptedPlansCalcTransitRoute = new AdaptedPlansCalcTransitRoute(config.plansCalcRoute(), this.getScenario().getNetwork(), freespeedTravelTimeCost, freespeedTravelTimeCost, dijkstraFactory, this.getScenario().getTransitSchedule(), transitConfig, myTransitRouterConfig);
		//adaptedPlansCalcTransitRoute.creteTransitRouter();  Refactoring needed for this
		return adaptedPlansCalcTransitRoute;
	}

	public static void main(final String[] args) {
		String configFile;
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}
			
		ScenarioImpl scenario = new TransScenarioLoader().loadScenario(configFile);
		AdaptedControler adaptedControler = new AdaptedControler(scenario);
		adaptedControler.setCreateGraphs(false);
		adaptedControler.setOverwriteFiles(true);
		adaptedControler.setWriteEventsInterval(5); 
		//adaptedControler.setUseOTFVis(false) ;
		adaptedControler.run();
	}

}
