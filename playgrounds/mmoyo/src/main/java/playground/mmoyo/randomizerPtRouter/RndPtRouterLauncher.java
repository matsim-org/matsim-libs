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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility.DataCollection;

public class RndPtRouterLauncher {

	private static TransitRouterFactory createRandomizedTransitRouterFactory (final TransitSchedule schedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork){
		return 
		new TransitRouterFactory() {
			@Override
			public TransitRouter createTransitRouter() {
				RandomizedTransitRouterTravelTimeAndDisutility ttCalculator = 
					new RandomizedTransitRouterTravelTimeAndDisutility(trConfig);
				ttCalculator.setDataCollection(DataCollection.randomizedParameters, false) ;
				ttCalculator.setDataCollection(DataCollection.additionalInformation, false) ;
				return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
			}
		};
	}
		 
	public static void main(final String[] args) {
		String configFile ;
		final double cadytsWeight;
		if(args.length==0){
			configFile = "../../ptManuel/calibration/my_config.xml";
			cadytsWeight = 30.0;
		}else{
			configFile = args[0];
			cadytsWeight = Double.parseDouble(args[1]);
		}
		
		Config config = ConfigUtils.loadConfig(configFile) ;
		
		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		config.addModule(ccc) ;
		
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
		
		//strategies settings
		{ 
		StrategySettings stratSets = new StrategySettings(Id.create(lastStrategyIdx+1, StrategySettings.class));
		stratSets.setStrategyName("myCadyts");
		stratSets.setWeight(0.9);
		config.strategy().addStrategySettings(stratSets);
		}
		
		{    //////!!!!!!!!!!!!!!!!!!!!!///////
		StrategySettings stratSets2 = new StrategySettings(Id.create(lastStrategyIdx+2, StrategySettings.class));
		stratSets2.setStrategyName("ReRoute"); // test that this does work.  Otherwise define this strategy in config file
		stratSets2.setWeight(0.1);
		stratSets2.setDisableAfter(400) ;
		config.strategy().addStrategySettings(stratSets2);
		}
		
		//load data
		final Scenario scn = ScenarioUtils.loadScenario(config);
		final TransitRouterConfig trConfig = new TransitRouterConfig( config ) ; 
		final TransitSchedule trSchedule =  scn.getTransitSchedule();
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(trSchedule, trConfig.beelineWalkConnectionDistance);
		
		//set the controler
		final Controler controler = new Controler(scn);
		controler.setOverwriteFiles(true);
		
		//add cadytsContext as ctrListener
		final double beta = 30. ;
		final CadytsPtContext context = new CadytsPtContext(config, controler.getEvents()) ;
		controler.addControlerListener(context) ;
		controler.addPlanStrategyFactory("myCadyts", new PlanStrategyFactory() {
			@Override
			public PlanStrategy get() {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(controler.getScenario(), context);
///				planSelector.setCadytsWeight(beta*cadytsWeight) ;   
				return new PlanStrategyImpl(planSelector);
			}} ) ;
		
		//create the factory for rndizedRouter
		TransitRouterFactory randomizedTransitRouterFactory = createRandomizedTransitRouterFactory (trSchedule, trConfig, routerNetwork);
		controler.setTransitRouterFactory(randomizedTransitRouterFactory);		
		
		//add analyzer for specific bus line and stop Zone conversion
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(true); 
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);  
		
	
		controler.run();
	}

}
