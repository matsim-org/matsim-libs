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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.artemc.socialCost.SocialCostController.Initializer;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import scenarios.illustrative.analysis.TtAbstractAnalysisTool;
import scenarios.illustrative.analysis.TtListenerToBindAndWriteAnalysis;
import scenarios.illustrative.braess.analysis.TtAnalyzeBraess;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation.InitRoutes;
import scenarios.illustrative.braess.run.RunBraessSimulation;
import scenarios.illustrative.braess.run.RunBraessSimulation.PricingType;

/**
 * Test to fix the route distribution and travel times in Braess's scenario.
 * If it fails something has changed to previous MATSim behavior.
 * 
 * Currently, congestion version V4 throws a runtime exception and is therefore set to ignore (see comment below).
 * 
 * @author tthunig
 *
 */
public final class FixBraessBehaviorTest{
	
	private static final Logger log = Logger
			.getLogger(FixBraessBehaviorTest.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testBraessWoPricing() {
		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.NONE, 24, 1920, 21, 3805298);
	}

	@Test
	public void testV3() {
		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.V3, 457, 1135, 408, 3113939);
	}
	
	/* V4 throws a runtime exception: 
	 * "time=28915.0; 13.799999999999999 sec delay is not internalized. Aborting..."
	 * Amit, please fix this and remove the @Ignore. 
	 * Theresa & Ihab oct'2015 */
	@Test
	@Ignore
	public void testV4() {
		Assert.fail("Not yet tested.");
//		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.V4, , , , );
	}

	@Test
	public void testV8() {
		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.V8, 572, 974, 454, 2992020);
	}
	
	@Test
	public void testV9() {
		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.V9, 603, 863, 534, 2825507);
	}
	
	@Test
	@Ignore
	public void testFlowbased() {
		Assert.fail("Not yet implemented.");
//		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.FLOWBASED, , , , );
	}
	
	private void fixRouteDistributionAndTT(RunBraessSimulation.PricingType pricingType, int expectedNOAgentsOnUpperRoute,
			int expectedNOAgentsOnMiddleRoute, int expectedNOAgentsOnLowerRoute, double expectedTotalTT){

		Config config = defineConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);

		/*
		 * create population instead of reading a population file, so that it is
		 * comparable to the results of RunBraessSimulation. for differences
		 * of reading vs creating population, see ReadVsCreatePopulationTest.
		 */
		createPopulation(scenario);
		
		Controler controler = new Controler(scenario);
		
		if (!pricingType.equals(PricingType.NONE) && !pricingType.equals(PricingType.FLOWBASED)) {
			// add tolling
			TollHandler tollHandler = new TollHandler(scenario);
			// choose the correct congestion handler and add it
			EventHandler congestionHandler = null;
			switch (pricingType) {
			case V3:
				congestionHandler = new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario());
				break;
			case V4:
				congestionHandler = new CongestionHandlerImplV4(controler.getEvents(), controler.getScenario());
				break;
			case V8:
				congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), controler.getScenario());
				break;
			case V9:
				congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), controler.getScenario());
				break;
			default:
				break;
			}
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, congestionHandler));
		} else if (pricingType.equals(PricingType.FLOWBASED)) {
			Initializer initializer = new Initializer();
			controler.addControlerListener(initializer);
		}
					
		// add a controller listener to analyze results
		TtAbstractAnalysisTool handler = new TtAnalyzeBraess();
		controler.addControlerListener(new TtListenerToBindAndWriteAnalysis(scenario, handler, false));
			
		controler.run();		
		
		// test route distribution
		int agentsOnUpperRoute = handler.getRouteUsers()[0];
		int agentsOnMiddleRoute = handler.getRouteUsers()[1];
		int agentsOnLowerRoute = handler.getRouteUsers()[2];
		log.info("Route distribution: " + agentsOnUpperRoute + ", " + agentsOnMiddleRoute + ", " + agentsOnLowerRoute);
		Assert.assertEquals("The number of agents on the upper route has changed to previous MATSim behavior.", expectedNOAgentsOnUpperRoute, agentsOnUpperRoute);
		Assert.assertEquals("The number of agents on the middle route has changed to previous MATSim behavior.", expectedNOAgentsOnMiddleRoute, agentsOnMiddleRoute);
		Assert.assertEquals("The number of agents on the lower route has changed to previous MATSim behavior.", expectedNOAgentsOnLowerRoute, agentsOnLowerRoute);
		
		// test total travel time
		double totalTT = handler.getTotalTT();
		Assert.assertEquals("The total travel time has changed to previous MATSim behavior.", expectedTotalTT, totalTT, MatsimTestUtils.EPSILON);
	}
	
	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		config.network().setInputFile(testUtils.getClassInputDirectory() + "network_cap2000-1000.xml");

		config.controler().setLastIteration(50);

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

		config.qsim().setStuckTime(3600 * 0.5);

		// set end time to 12 am (4 hours after simulation start) to
		// shorten simulation run time
		config.qsim().setEndTime(3600 * 10);

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
