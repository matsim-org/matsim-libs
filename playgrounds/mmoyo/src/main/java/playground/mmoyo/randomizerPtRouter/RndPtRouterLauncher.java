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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.kai.usecases.randomizedptrouter.RandomizedTransitRouterNetworkTravelTimeAndDisutility;
import playground.kai.usecases.randomizedptrouter.RandomizedTransitRouterNetworkTravelTimeAndDisutility.DataCollection;

public class RndPtRouterLauncher {

	private static TransitRouterFactory createRandomizedTransitRouterFactory (final TransitSchedule schedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork){
		return 
		new TransitRouterFactory() {
			@Override
			public TransitRouter createTransitRouter() {
				RandomizedTransitRouterNetworkTravelTimeAndDisutility ttCalculator = new RandomizedTransitRouterNetworkTravelTimeAndDisutility(trConfig);
				ttCalculator.setDataCollection(DataCollection.randomizedParameters, true) ;
				ttCalculator.setDataCollection(DataCollection.additionInformation, false) ;
				return new TransitRouterImpl(trConfig, routerNetwork, ttCalculator, ttCalculator);
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

		//load data
		Scenario scn = ScenarioUtils.loadScenario(config);
		final TransitRouterConfig trConfig = new TransitRouterConfig( scn.getConfig() ) ; 
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(scn.getTransitSchedule(), trConfig.beelineWalkConnectionDistance);
		
		//create the factory for rndizedRouter
		TransitRouterFactory randomizedTransitRouterFactory = createRandomizedTransitRouterFactory (scn.getTransitSchedule(), trConfig, routerNetwork);
		
		//set the controler
		Controler controler = new Controler(scn);
		controler.setTransitRouterFactory(randomizedTransitRouterFactory);
//		controler.addControlerListener(new CadytsPtControlerListener(controler));  //<-set cadyts as controler listener
		controler.setOverwriteFiles(true);
		
		controler.run();
	}

}
