/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.randomizerPtRouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.cadyts.pt.CadytsContext;
import org.matsim.contrib.cadyts.pt.CadytsPtPlanChanger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.kai.usecases.randomizedptrouter.RandomizedTransitRouterNetworkTravelTimeAndDisutility2;
import playground.kai.usecases.randomizedptrouter.RandomizedTransitRouterNetworkTravelTimeAndDisutility2.DataCollection;

public class RndPtRouterLauncher {

	private static TransitRouterFactory createRandomizedTransitRouterFactory (final TransitSchedule schedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork){
		return 
		new TransitRouterFactory() {
			@Override
			public TransitRouter createTransitRouter() {
				RandomizedTransitRouterNetworkTravelTimeAndDisutility2 ttCalculator = 
					new RandomizedTransitRouterNetworkTravelTimeAndDisutility2(trConfig);
				ttCalculator.setDataCollection(DataCollection.randomizedParameters, true) ;
				ttCalculator.setDataCollection(DataCollection.additionInformation, false) ;
				return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
			}
		};
	}
		 
	public static void main(final String[] args) {
		String configFile ;
		if(args.length==0){
			configFile = "../../ptManuel/calibration/100plans_bestValues_config.xml";
		}else{
			configFile = args[0];
		}
		
		Config config = ConfigUtils.loadConfig(configFile) ;
		
		
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
		
		{
		StrategySettings stratSets = new StrategySettings(new IdImpl(lastStrategyIdx+1));
		stratSets.setModuleName("myCadyts");
		stratSets.setProbability(0.9);
		config.strategy().addStrategySettings(stratSets);
		}
		{
		StrategySettings stratSets2 = new StrategySettings(new IdImpl(lastStrategyIdx+2));
		stratSets2.setModuleName("ReRoute"); // test that this does work.  Otherwise define this strategy in config file
		stratSets2.setProbability(0.9);
		stratSets2.setDisableAfter(400) ;
		config.strategy().addStrategySettings(stratSets2);
		}
		
		//load data
		Scenario scn = ScenarioUtils.loadScenario(config);
		final TransitRouterConfig trConfig = new TransitRouterConfig( scn.getConfig() ) ; 
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(scn.getTransitSchedule(), trConfig.beelineWalkConnectionDistance);
		
		//create the factory for rndizedRouter
		TransitRouterFactory randomizedTransitRouterFactory = createRandomizedTransitRouterFactory (scn.getTransitSchedule(), trConfig, routerNetwork);
		
		//set the controler
		final Controler controler = new Controler(scn);
		controler.setTransitRouterFactory(randomizedTransitRouterFactory);
//		controler.addControlerListener(new CadytsPtControlerListener(controler));  //<-set cadyts as controler listener
		controler.setOverwriteFiles(true);

		final CadytsContext context = new CadytsContext( config ) ;
		controler.addPlanStrategyFactory("myCadyts", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
				return new PlanStrategyImpl(new CadytsPtPlanChanger(context));
			}
		});
		
		controler.run();
	}

}
