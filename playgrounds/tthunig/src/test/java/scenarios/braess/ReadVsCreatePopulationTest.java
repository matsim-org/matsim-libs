/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.braess;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import scenarios.analysis.TtAbstractAnalysisTool;
import scenarios.analysis.TtListenerToBindAndWriteAnalysis;
import scenarios.braess.analysis.TtAnalyzeBraess;
import scenarios.braess.createInput.TtCreateBraessPopulation;
import scenarios.braess.createInput.TtCreateBraessPopulation.InitRoutes;

/**
 * @author tthunig
 *
 */
public class ReadVsCreatePopulationTest {

	private static final Logger log = Logger
			.getLogger(ReadVsCreatePopulationTest.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testReadVsCreatePopulation() {
		TtAbstractAnalysisTool handlerRead = run(false);
		TtAbstractAnalysisTool handlerCreate = run(true);
		
		// compare results
		log.info("the total travel times are: " + handlerRead.getTotalTT() + " and " + handlerCreate.getTotalTT());
		log.info("the route distributions are: " + handlerRead.getRouteUsers()[0] + ", " + handlerRead.getRouteUsers()[1] + ", " + handlerRead.getRouteUsers()[2] 
				+ " and " + handlerCreate.getRouteUsers()[0] + ", " + handlerCreate.getRouteUsers()[1] + ", " + handlerCreate.getRouteUsers()[2]);
		Assert.assertArrayEquals("route distribution does not match", handlerRead.getRouteUsers(), handlerCreate.getRouteUsers());
		Assert.assertEquals("total travel time does not match", handlerRead.getTotalTT(), handlerCreate.getTotalTT(), MatsimTestUtils.EPSILON);
	}

	/**
	 * runs the test scenario
	 * 
	 * @param createPopulation
	 *            creates the population in code if true; reads the identical
	 *            population from file if false
	 * @return the handler that contains the results
	 */
	private TtAbstractAnalysisTool run(boolean createPopulation) {
		
		Config config = defineConfig(createPopulation);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (createPopulation){
			createPopulation(scenario);
		}
		
		Controler controler = new Controler(scenario);
					
		// add a controller listener to analyze results
		TtAbstractAnalysisTool handler = new TtAnalyzeBraess();
		controler.addControlerListener(new TtListenerToBindAndWriteAnalysis(scenario, handler));
			
		controler.run();
		
		return handler;
	}
	
	private Config defineConfig(boolean createPopulation) {
		Config config = ConfigUtils.createConfig();

		// set network and population
		config.network().setInputFile(testUtils.getClassInputDirectory() + "network_cap2000-1000.xml");
		if (!createPopulation){ // read population
			config.plans().setInputFile(testUtils.getClassInputDirectory() + "plans2000_initRoutes.xml");
		}

		// set number of iterations
		config.controler().setLastIteration(1);

		// set brain exp beta
		config.planCalcScore().setBrainExpBeta(20);

		// define strategies:
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
			strat.setWeight(1.0);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}
		
		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize(3);

		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.controler().setWritePlansInterval(config.controler().getLastIteration());

		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);

		config.controler().setCreateGraphs(false);

		return config;
	}
	
	private static void createPopulation(Scenario scenario) {
		
		TtCreateBraessPopulation popCreator = 
				new TtCreateBraessPopulation(scenario.getPopulation(), scenario.getNetwork());
		popCreator.setNumberOfPersons(2000);
		
		popCreator.createPersons(InitRoutes.ALL, 110.);
	}
	
}
