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

import org.junit.Assert;
import org.junit.Ignore;
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

import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.TollHandler;
import scenarios.analysis.TtAbstractAnalysisTool;
import scenarios.analysis.TtListenerToBindAndWriteAnalysis;
import scenarios.braess.analysis.TtAnalyzeBraess;

/**
 * Test to fix the route distribution and travel times in Braess's scenario.
 * If it fails something has changed to previous MATSim behavior.
 * 
 * Currently, congestion version V4 throws a runtime exception and is therefore set to ignore (see comment below).
 * 
 * @author tthunig
 *
 */
public class FixBraessBehaviorTest{
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testBraessWoPricing() {
		
		Config config = defineConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		 		 
		// add a controller listener to analyze results
		TtAbstractAnalysisTool handler = new TtAnalyzeBraess();
		controler.addControlerListener(new TtListenerToBindAndWriteAnalysis(scenario, handler));
			
		controler.run();		
		
		// test route distribution
		int agentsOnMiddleRoute = handler.getRouteUsers()[1];
		// was 1983 in sept'15 - why?
		Assert.assertEquals("The number of agents on the middle route has changed to previous MATSim behavior.", 1978, agentsOnMiddleRoute, 5);
		int agentsOnLowerRoute = handler.getRouteUsers()[2];
		// was 8 in sept'15 - why?
		Assert.assertEquals("The number of agents on the lower route has changed to previous MATSim behavior.", 11, agentsOnLowerRoute, 3);
		int agentsOnUpperRoute = handler.getRouteUsers()[0];
		// was 9 in sept'15 - why?
		Assert.assertEquals("The number of agents on the upper route has changed to previous MATSim behavior.", 11, agentsOnUpperRoute, 2);
		
		// test total travel time
		double totalTT = handler.getTotalTT();
		// was 3951597 in sept'15 - why?
		Assert.assertEquals("The total travel time has changed to previous MATSim behavior.", 3949870, totalTT, 2000);		
	}
	
	// TODO test other congestion versions too (v3, v8, v9)
	@Test
	public void testV3() {
		Assert.fail("Not yet implemented.");
	}
	
	/* V4 throws a runtime exception: 
	 * "time=28915.0; 13.799999999999999 sec delay is not internalized. Aborting..."
	 * Amit, please fix this and remove the @Ignore. 
	 * Theresa & Ihab oct'2015 */
	@Test
	@Ignore
	public void testV4() {
		// prepare config and scenario
		Config config = defineConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// prepare the controller
		Controler controler = new Controler(scenario);

		// add tolling
		TollHandler tollHandler = new TollHandler(scenario);

		controler.addControlerListener(
				new MarginalCongestionPricingContolerListener(
						controler.getScenario(), tollHandler, 
						new CongestionHandlerImplV4(controler.getEvents(), 
								controler.getScenario())));

		// run the simulation
		controler.run();
		
		// TODO test whether total travel time, route distribution ... has changed compared to previous MATSim behavior.
	}

	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		// set network and population
		config.network().setInputFile(testUtils.getClassInputDirectory() + "network_cap2000-1000.xml");
		config.plans().setInputFile(testUtils.getClassInputDirectory() + "plans2000_initRoutes.xml");

		// set number of iterations
		config.controler().setLastIteration(100);

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

		config.qsim().setStuckTime(3600 * 10.);

		// set end time to 12 am (4 hours after simulation start) to
		// shorten simulation run time
		config.qsim().setEndTime(3600 * 12);

		// adapt monetary distance cost rate
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(-0.0);

		config.planCalcScore().setMarginalUtilityOfMoney(1.0); // default is 1.0

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
	
}
