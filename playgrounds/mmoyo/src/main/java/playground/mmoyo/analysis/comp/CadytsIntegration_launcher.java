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

package playground.mmoyo.analysis.comp;

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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.mmoyo.taste_variations.CadytsUtlCorrectionsCollecter;


/**
 * Launches Cadyts as scoring function (without randomized router) for scenarios without stopZoneConversion
 */
public class CadytsIntegration_launcher {
	
	public static void main(String[] args) {
		String configFile ;
		final double cadytsScoringWeight;
		String strDoStzopZoneConversion;
		
		if(args.length==0){
			configFile = "../../";
			cadytsScoringWeight = 0.0;
			strDoStzopZoneConversion = "false";
		}else{
			configFile = args[0];
			cadytsScoringWeight = Double.parseDouble(args[1]);
			strDoStzopZoneConversion = args[2];
		}
		
		final Config config = ConfigUtils.loadConfig(configFile) ;
		
		configFile= null; //M
		
		//final double beta=30. ;   			//set this in config file!!!
		//config.planCalcScore().setBrainExpBeta(beta) ;
		
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
		
		//strategies settings
		{ //cadyts
		StrategySettings stratSets = new StrategySettings(Id.create(lastStrategyIdx+1, StrategySettings.class));
		stratSets.setStrategyName("myCadyts");
		stratSets.setWeight(1.0);
		config.strategy().addStrategySettings(stratSets);
		}
		
		//set the controler
		final Scenario scn = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scn);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		//create cadyts context
		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		config.addModule(ccc) ;
		final CadytsPtContext cContext = new CadytsPtContext( config, controler.getEvents() ) ;
		controler.addControlerListener(cContext) ;
		
		//set cadyts as strategy for plan selector
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("myCadyts").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Override
					public PlanStrategy get() {
						final CadytsPlanChanger planSelector = new CadytsPlanChanger(scn, cContext);
						//planSelector.setCadytsWeight(0.0) ;   // <-set it to zero if only cadyts scores are desired
						return new PlanStrategyImpl(planSelector);
					}
				});
			}
		});


		//set scoring functions
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore()); //M
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				//if (params == null) {																			//<- see comment about performance improvement in 
				//	params = new CharyparNagelScoringParameters(config.planCalcScore()); //org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.createNewScoringFunction
				//}

				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(),config, cContext);
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
 
				return scoringFunctionAccumulator;
			}
		}) ;
		
		//add svd calculator as control listener
		CadytsUtlCorrectionsCollecter utilCollecter = new CadytsUtlCorrectionsCollecter(scn.getNetwork(), scn.getTransitSchedule());
		controler.addControlerListener(utilCollecter);
		
		//add analyzer for specific bus line
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		final boolean doStopZoneConversion = Boolean.parseBoolean(strDoStzopZoneConversion);
		System.out.println("doing stop zone conversion: " + doStopZoneConversion);
		strDoStzopZoneConversion= null;
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(doStopZoneConversion);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);

		controler.run();
	} 
}