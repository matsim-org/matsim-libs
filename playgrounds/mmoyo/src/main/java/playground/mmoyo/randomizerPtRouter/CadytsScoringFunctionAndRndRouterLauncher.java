/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility;

import javax.inject.Provider;

public class CadytsScoringFunctionAndRndRouterLauncher {

	private static Provider<TransitRouter> createRandomizedTransitRouterFactory (final PreparedTransitSchedule preparedSchedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork){
		return 
		new Provider<TransitRouter>() {
			@Override
			public TransitRouter get() {
				RandomizedTransitRouterTravelTimeAndDisutility ttCalculator = 
					new RandomizedTransitRouterTravelTimeAndDisutility(trConfig);
				//ttCalculator.setDataCollection(DataCollection.randomizedParameters, false) ;
				//ttCalculator.setDataCollection(DataCollection.additionInformation, false) ;
				return new TransitRouterImpl(trConfig, preparedSchedule, routerNetwork, ttCalculator, ttCalculator);
			}
		};
	}

	public static void main(String[] args) {
		String configFile ;
		final double cadytsScoringWeight;
		if(args.length==0){
			configFile = "../../";
			cadytsScoringWeight = 0.0;
		}else{
			configFile = args[0];
			cadytsScoringWeight = Double.parseDouble(args[1]);
		}
		
		final Config config = ConfigUtils.loadConfig(configFile) ;
		final double beta=30. ;
		
		configFile= null; //M
		
		config.planCalcScore().setBrainExpBeta(beta) ;
		
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
		
		//strategies settings
		{ //cadyts
		StrategySettings stratSets = new StrategySettings(Id.create(lastStrategyIdx+1, StrategySettings.class));
		stratSets.setStrategyName("myCadyts");
		stratSets.setWeight(0.9);
		config.strategy().addStrategySettings(stratSets);
		}
		
		{ //rnd router
		StrategySettings stratSets2 = new StrategySettings(Id.create(lastStrategyIdx+2, StrategySettings.class));
		stratSets2.setStrategyName("ReRoute"); // 
		stratSets2.setWeight(0.1);
		stratSets2.setDisableAfter(400) ;
		config.strategy().addStrategySettings(stratSets2);
		}		

		//set the controler
		final Scenario scn = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scn);
		controler.setOverwriteFiles(true) ;
		
		//create cadyts context
		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		config.addModule(ccc) ;
		final CadytsPtContext cContext = new CadytsPtContext( config, controler.getEvents()  ) ;
		controler.addControlerListener(cContext) ;
		
		//set cadyts as strategy for plan selector
		controler.addPlanStrategyFactory("myCadyts", new PlanStrategyFactory() {
			@Override
			public PlanStrategy get() {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(scn, cContext);
//				planSelector.setCadytsWeight(0.0) ;
				return new PlanStrategyImpl(planSelector);
			}
		} ) ;
		

		//set scoring functions
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore()); //M
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				//if (params == null) {																			//<- see comment about performance improvement in 
				//	params = new CharyparNagelScoringParameters(config.planCalcScore()); //org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.createNewScoringFunction
				//}

				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				//scoringFunctionAccumulator.setId(plan.getPerson().getId().toString());
				
				final CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(),config, cContext);
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
 
				return scoringFunctionAccumulator;
			}
		}) ;
		
		
		//create and set the factory for rndizedRouter 
		final TransitSchedule routerSchedule = controler.getScenario().getTransitSchedule();
		final TransitRouterConfig trConfig = new TransitRouterConfig( config );
		
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(routerSchedule, trConfig.beelineWalkConnectionDistance);
		final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(routerSchedule);
		Provider<TransitRouter> randomizedTransitRouterFactory = createRandomizedTransitRouterFactory (preparedSchedule, trConfig, routerNetwork);
		controler.setTransitRouterFactory(randomizedTransitRouterFactory);
		
		//add analyzer for specific bus line and stop Zone conversion
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(true); 
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);  
		
	
		controler.run();
	}

}
