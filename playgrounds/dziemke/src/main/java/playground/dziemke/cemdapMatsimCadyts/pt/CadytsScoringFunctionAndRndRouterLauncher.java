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

package playground.dziemke.cemdapMatsimCadyts.pt;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.contrib.cadyts.pt.CadytsPtModule;
import org.matsim.contrib.common.randomizedtransitrouter.RandomizingTransitRouterTravelTimeAndDisutility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CadytsScoringFunctionAndRndRouterLauncher {

	private static Provider<TransitRouter> createRandomizedTransitRouterFactory (final PreparedTransitSchedule preparedSchedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork){
		return 
		new Provider<TransitRouter>() {
			@Override
			public TransitRouter get() {
				RandomizingTransitRouterTravelTimeAndDisutility ttCalculator = 
					new RandomizingTransitRouterTravelTimeAndDisutility(trConfig);
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
//			configFile = "../../";
			configFile = "../../../shared-svn/projects/ptManuel/calibration/my_config_dz.xml";
//			cadytsScoringWeight = 0.0;
			cadytsScoringWeight = 100.0;
//			cadytsScoringWeight = 0.0;
		}else{
			configFile = args[0];
			cadytsScoringWeight = Double.parseDouble(args[1]);
		}
		
		final Config config = ConfigUtils.loadConfig(configFile) ;
//		final double beta=30. ;
		
		configFile= null; //M
		
//		config.planCalcScore().setBrainExpBeta(beta) ;
		
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
		
		//
		config.transit().setUseTransit(true);
		//
		
		//strategies settings
		{ //cadyts
//		StrategySettings stratSets = new StrategySettings(Id.create(lastStrategyIdx+1, StrategySettings.class));
		StrategySettings stratSets = new StrategySettings();
//		stratSets.setStrategyName("myCadyts");
		stratSets.setStrategyName("ChangeExpBeta");
		stratSets.setWeight(0.9);
		config.strategy().addStrategySettings(stratSets);
		}
		
		{ //rnd router
//		StrategySettings stratSets2 = new StrategySettings(Id.create(lastStrategyIdx+2, StrategySettings.class));
		StrategySettings stratSets2 = new StrategySettings();
		stratSets2.setStrategyName("ReRoute"); // 
		stratSets2.setWeight(0.1);
//		stratSets2.setDisableAfter(400) ;
		stratSets2.setDisableAfter(30) ;
		config.strategy().addStrategySettings(stratSets2);
		}		

		//set the controler
		final Scenario scn = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scn);
		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		//create cadyts context
		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		config.addModule(ccc) ;
		
		//
		controler.addOverridingModule(new CadytsPtModule());
		//
//		final CadytsPtContext cContext = new CadytsPtContext( config, controler.getEvents()  ) ;
//		controler.addControlerListener(cContext) ;
		
		//set cadyts as strategy for plan selector
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				addPlanStrategyBinding("myCadyts").toProvider(new Provider<PlanStrategy>() {
//					@Override
//					public PlanStrategy get() {
//						final CadytsPlanChanger planSelector = new CadytsPlanChanger(scn, cContext);
////				planSelector.setCadytsWeight(0.0) ;
//						return new PlanStrategyImpl(planSelector);
//					}
//				});
//			}
//		});

		
		

		//set scoring functions
//		final CharyparNagelScoringParameters params = CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).create(); //M
		// weiter unten
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			//
			@Inject private CadytsPtContext cadytsContext;
			@Inject ScoringParametersForPerson parameters;
			//
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				//if (params == null) {																			//<- see comment about performance improvement in 
				//	params = new CharyparNagelScoringParameters(config.planCalcScore()); //org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.createNewScoringFunction
				//}
				
				//
				final ScoringParameters params = parameters.getScoringParameters(person);
				//

//				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				SumScoringFunction sumScoringFunction = new SumScoringFunction();
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				//scoringFunctionAccumulator.setId(plan.getPerson().getId().toString());
				
//				final CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(),config, cContext);
				final CadytsScoring<TransitStopFacility> scoringFunction = new CadytsScoring<TransitStopFacility>(person.getSelectedPlan(), config, cadytsContext);
				
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
//				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
				sumScoringFunction.addScoringFunction(scoringFunction );
 
//				return scoringFunctionAccumulator;
				return sumScoringFunction;
			}
		}) ;
		
		
		//create and set the factory for rndizedRouter 
		final TransitSchedule routerSchedule = controler.getScenario().getTransitSchedule();
		final TransitRouterConfig trConfig = new TransitRouterConfig( config );
		
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(routerSchedule, trConfig.getBeelineWalkConnectionDistance());
		final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(routerSchedule);
		final Provider<TransitRouter> randomizedTransitRouterFactory = createRandomizedTransitRouterFactory (preparedSchedule, trConfig, routerNetwork);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(randomizedTransitRouterFactory);
			}
		});

		//add analyzer for specific bus line and stop Zone conversion
		// TODO reactivate
//		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
//		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(true); 
//		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);  
		
	
		controler.run();
	}

}
