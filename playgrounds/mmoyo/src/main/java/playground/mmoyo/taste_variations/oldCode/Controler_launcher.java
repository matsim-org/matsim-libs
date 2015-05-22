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

package playground.mmoyo.taste_variations.oldCode;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.mmoyo.taste_variations.CadytsUtlCorrectionsCollecter;

public class Controler_launcher {
	static final String TB = "\t";
	static final String NL = "\n";

	public static void main(String[] args)  {
		String configFile;
		final double cadytsScoringWeight;
		String strDoStzopZoneConversion;
		
		if (args.length > 0){
			configFile = args[0];
			cadytsScoringWeight = Double.parseDouble(args[1]);
			strDoStzopZoneConversion = args[2];
		}else {
			configFile = "../../ptManuel/calibration/my_config.xml";
			cadytsScoringWeight = 1.0;
			strDoStzopZoneConversion = "false";
		}

		final Config config = ConfigUtils.loadConfig(configFile);
		final Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		final Network net = controler.getScenario().getNetwork();
		final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
		final boolean doStopZoneConversion = Boolean.parseBoolean(strDoStzopZoneConversion);
		strDoStzopZoneConversion= null;
		configFile = null;

		//////  CONFIGURE CADYTS//////////////////////////////////////////////////////
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
		{ 
		StrategySettings stratSets = new StrategySettings(Id.create(lastStrategyIdx+1, StrategySettings.class));
		stratSets.setStrategyName("myCadyts");
		stratSets.setWeight(1.0);
		config.strategy().addStrategySettings(stratSets);
		}
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
						final CadytsPlanChanger planSelector = new CadytsPlanChanger(controler.getScenario(), cContext);
						//planSelector.setCadytsWeight(0.0) ;
						return new PlanStrategyImpl(planSelector);
					}

				});
			}
		});

		//set scoring functions
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore()); //M
		//final boolean useBruteForce =Boolean.parseBoolean(config.getParam("cadytsPt", "useBruteForce")); 
		CadytsConfigGroup cptcg = (CadytsConfigGroup) controler.getConfig().getModule(CadytsConfigGroup.GROUP_NAME);
		final boolean useBruteForce = Boolean.parseBoolean(cptcg.getParams().get("useBruteForce"));
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				
				if (!useBruteForce){
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, net));
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));	
				}
				
				final CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(),config, cContext);
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
	 
				return scoringFunctionAccumulator;
			}
			
		}) ;
		
		//add svd calculator as control listener
		CadytsUtlCorrectionsCollecter utilCollecter = new CadytsUtlCorrectionsCollecter(net, schedule);
		controler.addControlerListener(utilCollecter);
		
		//add analyzer for specific bus line
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(doStopZoneConversion);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);

		controler.run();
	}
}
