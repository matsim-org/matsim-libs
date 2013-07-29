/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsController.java
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

package playground.dziemke.cadyts.controller;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.car.CadytsCarConfigGroup;
import org.matsim.contrib.cadyts.car.CadytsCarScoring;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.car.CadytsPlanChanger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CadytsController56 {
	private final static Logger log = Logger.getLogger(CadytsController56.class);
	
	public static void main(String[] args) {
		final Config config = ConfigUtils.createConfig();
		
		// global
		config.global().setCoordinateSystem("GK4");
		
		// network
		String inputNetworkFile = "D:/Workspace/berlin/counts/iv_counts/network.xml";
		config.network().setInputFile(inputNetworkFile);
				
		// plans
		String inputPlansFile = "D:/Workspace/container/demand/input/cemdap2matsim/10/plans.xml.gz";
		config.plans().setInputFile(inputPlansFile);
						
//		// cadytsCar
//		CadytsCarConfigGroup cadytsCarConfigGroup = new CadytsCarConfigGroup();
//		// new
//		cadytsCarConfigGroup.setStartTime(5*60*60);
//		// end new
//		
//		// changed
//		cadytsCarConfigGroup.setEndTime(22*60*60);
//		// end changed
//		
//		cadytsCarConfigGroup.setUseBruteForce(false);
//		config.addModule("ccc", cadytsCarConfigGroup);
		
		// simulation
		config.addSimulationConfigGroup(new SimulationConfigGroup());
		config.simulation().setFlowCapFactor(0.01);
		config.simulation().setStorageCapFactor(0.02);
						
		// counts
		String countsFileName = "D:/Workspace/berlin/counts/iv_counts/vmz_di-do.xml";
		config.counts().setCountsFileName(countsFileName);
		config.counts().setCountsScaleFactor(100);
		config.counts().setOutputFormat("all");
				
		// controller
		String runId = "run_56";
		String outputDirectory = "D:/Workspace/container/demand/output/run_56";
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(150);
		Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));
		config.controler().setEventsFileFormats(eventsFileFormats);
		config.controler().setMobsim("queueSimulation");
		config.controler().setWritePlansInterval(50);
		config.controler().setWriteEventsInterval(50);
				
		// strategy
		StrategySettings strategySettings = new StrategySettings(new IdImpl(1));
		strategySettings.setModuleName("ReRoute");
		strategySettings.setProbability(0.1);
		config.strategy().addStrategySettings(strategySettings);
		
//		StrategySettings strategySettings2 = new StrategySettings(new IdImpl(2));
//		strategySettings2.setModuleName("ccc");
//		strategySettings2.setProbability(1.0);
//		config.strategy().addStrategySettings(strategySettings2);
		
		// moved back here before run 56
		StrategySettings stratSets = new StrategySettings(new IdImpl(2));
		// stratSets.setModuleName("ccc");
		stratSets.setModuleName("cadytsCar") ;
		stratSets.setProbability(1.0) ;
		// controler.getConfig().strategy().addStrategySettings(stratSets);
		config.strategy().addStrategySettings(stratSets);
		// end new 56
		
		config.strategy().setMaxAgentPlanMemorySize(5);
		
		// planCalcScore
		ActivityParams homeActivity = new ActivityParams("home");
		homeActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(homeActivity);
		
		ActivityParams workActivity = new ActivityParams("work");
		workActivity.setTypicalDuration(9*60*60);
		config.planCalcScore().addActivityParams(workActivity);
		
		ActivityParams leisureActivity = new ActivityParams("leis");
		leisureActivity.setTypicalDuration(2*60*60);
		config.planCalcScore().addActivityParams(leisureActivity);
		
		ActivityParams shopActivity = new ActivityParams("shop");
		shopActivity.setTypicalDuration(1*60*60);
		config.planCalcScore().addActivityParams(shopActivity);
		
		ActivityParams otherActivity = new ActivityParams("other");
		otherActivity.setTypicalDuration(0.5*60*60);
		config.planCalcScore().addActivityParams(otherActivity);
				
		
		// start controller
		final Controler controler = new Controler(config);
		
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		// new CadytsContext generates new CadytsCarConfigGroup with name "cadytsCar"
		// accordingly use this name from now on
		controler.addControlerListener(cContext);
		
		// new 52
//		StrategySettings stratSets = new StrategySettings(new IdImpl(2));
//		// stratSets.setModuleName("ccc");
//		stratSets.setModuleName("cadytsCar") ;
//		stratSets.setProbability(1.0) ;
//		controler.getConfig().strategy().addStrategySettings(stratSets);
		// end new 52
		
		
		// new 54
		controler.getConfig().getModule("cadytsCar").addParam("startTime", "00:00:00");
		controler.getConfig().getModule("cadytsCar").addParam("endTime", "24:00:00");
		// end new 54
				
		
		// controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(cContext);

				planSelector.setCadytsWeight(30.*scenario2.getConfig().planCalcScore().getBrainExpBeta() ) ;
				// set cadyts weight very high = close to brute force
				
				return new PlanStrategyImpl(planSelector);
			}
		});
		
		
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {
				
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsCarScoring scoringFunction = new CadytsCarScoring(plan, config, cContext);
				// final double cadytsScoringWeight = 1.0;
				// new run 56#
				final double cadytsScoringWeight = 10.0;
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;
		
		
		// same as using my ZeroScoringFunctionFactroy
//		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
//			@Override
//			public ScoringFunction createNewScoringFunction(Plan plan) {
//				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
//				return scoringFunctionAccumulator;
//			}
//		});
		
		
//		controler.addControlerListener(new KaiAnalysisListener());
		
		controler.run();
	}
}
