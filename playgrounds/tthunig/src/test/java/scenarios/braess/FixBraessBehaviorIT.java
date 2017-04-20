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
import org.matsim.core.controler.AbstractModule;
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
import scenarios.illustrative.braess.analysis.TtAnalyzeBraess;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation.InitRoutes;
import scenarios.illustrative.braess.run.RunBraessSimulation;
import scenarios.illustrative.braess.run.RunBraessSimulation.PricingType;

/**
 * Test to fix the route distribution and travel times in Braess's scenario.
 * If it fails something has changed to previous MATSim behavior.
 * 
 * @author tthunig
 *
 */
public final class FixBraessBehaviorIT{
	
	private static final Logger log = Logger
			.getLogger(FixBraessBehaviorIT.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
//	@Ignore("due to bugfixes in fast capacity update (by michalm) this tests give different results on jenkins and travis")
    @Test
	public void testBraessWoPricing() {
		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.NONE, 26, 1920, 14, 3785111);
	}

//	@Ignore("due to bugfixes in fast capacity update (by michalm) this tests give different results on jenkins and travis")
    @Test
	public void testV3() {
		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.V3, 456, 1091, 453, 2995253);
	}

//	@Ignore("due to bugfixes in fast capacity update (by michalm) this tests give different results on jenkins and travis")
	@Test
	public void testV8() {
		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.V8, 538, 1026, 436, 2965792);
	}

//	@Ignore("due to bugfixes in fast capacity update (by michalm) this tests give different results on jenkins and travis")
	@Test
	public void testV9() {
		fixRouteDistributionAndTT(RunBraessSimulation.PricingType.V9, 605, 850, 545, 2814891);
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
					
		TtAbstractAnalysisTool handler = new TtAnalyzeBraess();
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(handler);
			}
		});
			
		controler.run();		
		
		// get route distribution
		int agentsOnUpperRoute = handler.getRouteUsers()[0];
		int agentsOnMiddleRoute = handler.getRouteUsers()[1];
		int agentsOnLowerRoute = handler.getRouteUsers()[2];
		log.info("Route distribution: " + agentsOnUpperRoute + ", " + agentsOnMiddleRoute + ", " + agentsOnLowerRoute);
		
		// get total travel time
		double totalTT = handler.getTotalTT();
		log.info("Total travel time: " + totalTT);
		
		// test both
		Assert.assertEquals("The number of agents on the upper route has changed to previous MATSim behavior.", expectedNOAgentsOnUpperRoute, agentsOnUpperRoute);
		Assert.assertEquals("The number of agents on the middle route has changed to previous MATSim behavior.", expectedNOAgentsOnMiddleRoute, agentsOnMiddleRoute);
		Assert.assertEquals("The number of agents on the lower route has changed to previous MATSim behavior.", expectedNOAgentsOnLowerRoute, agentsOnLowerRoute);
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
		
		config.vspExperimental().setWritingOutputEvents(false);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setDumpDataAtEnd(false);
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
		popCreator.setSimulationStartTime(8*3600);
		popCreator.createPersons(InitRoutes.ALL, 110.);
	}
	
}
