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
package scenarios.cottbus;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.router.InvertedNetworkRoutingModuleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import analysis.TtTotalTravelTime;
import scenarios.cottbus.run.TtRunCottbusSimulation.ScenarioType;
import scenarios.cottbus.run.TtRunCottbusSimulation.SignalType;

/**
 * @author tthunig
 *
 */
public class FixCottbusResultsIT {
	
	private static final Logger log = Logger.getLogger(FixCottbusResultsIT.class);

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	@Ignore //takes to long
	public void testBC(){
		fixResults(ScenarioType.BaseCase, SignalType.MS, 0.0);
	}

	@Test
	public void testBCContinuedFreeRouteChoice(){
		fixResults(ScenarioType.BaseCaseContinued_MatsimRoutes, SignalType.BTU_OPT, 1133616.0);
	}
	
	@Test
	public void testBCContinuedFixedRouteSet(){
		fixResults(ScenarioType.BaseCaseContinued_BtuRoutes, SignalType.BTU_OPT, 1091221.0);
	}	
	
	private void fixResults(ScenarioType scenarioType, SignalType signalType, double expectedTotalTt) {
		Config config = defineConfig(scenarioType, signalType);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);	
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME,
				new SignalsScenarioLoader(signalsConfigGroup).loadSignalsData());
		
		Controler controler = new Controler(scenario);
		// add missing modules
		controler.addOverridingModule(new SignalsModule());
		controler.addOverridingModule(new InvertedNetworkRoutingModuleModule());

		TtTotalTravelTime handler = new TtTotalTravelTime();
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(handler);
			}
		});
		
		controler.run();
		
		// check travel time
		log.info("scenarioType: " + scenarioType + ", signalType: " + signalType + ", expectedTotalTt: " + expectedTotalTt + ", experiencedTotalTt: " + handler.getTotalTt());
		Assert.assertEquals(expectedTotalTt, handler.getTotalTt(), MatsimTestUtils.EPSILON);	
	}

	private Config defineConfig(ScenarioType scenarioType, SignalType signalType) {
		Config config = ConfigUtils.createConfig();
		
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());

		if (scenarioType.equals(ScenarioType.BaseCase)){
			config.network().setInputFile(testUtils.getClassInputDirectory() + "matsimData/network_wgs84_utm33n.xml.gz");
			config.network().setLaneDefinitionsFile(testUtils.getClassInputDirectory() + "matsimData/lanes.xml");
			config.plans().setInputFile(testUtils.getClassInputDirectory() + "matsimData/cb_spn_gemeinde_nachfrage_landuse_woMines/commuter_population_wgs84_utm33n_car_only.xml.gz");
		} else { // BaseCaseContinued
			config.network().setInputFile(testUtils.getClassInputDirectory() + "btuOpt/network_small_simplified.xml.gz");
			config.network().setLaneDefinitionsFile(testUtils.getClassInputDirectory() + "btuOpt/lanes_network_small.xml.gz");
			if (scenarioType.equals(ScenarioType.BaseCaseContinued_MatsimRoutes))
				config.plans().setInputFile(testUtils.getClassInputDirectory() + "btuOpt/trip_plans_from_morning_peak_ks_commodities_minFlow50.0.xml");
			else // BtuRoutes	
				config.plans().setInputFile(testUtils.getClassInputDirectory() + "btuOpt/2015-03-10_sameEndTimes_ksOptRouteChoice_paths.xml");
		}
		
		// set number of iterations
		config.controler().setLastIteration(10);

		// able or enable signals and lanes
		config.qsim().setUseLanes( true );
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems( true );
		// set signal files
		if (scenarioType.equals(ScenarioType.BaseCase)){
			signalConfigGroup.setSignalSystemFile(testUtils.getClassInputDirectory() + "matsimData/signal_systems_no_13.xml");
		} else { // BaseCaseContinued
			signalConfigGroup.setSignalSystemFile(testUtils.getClassInputDirectory() + "btuOpt/output_signal_systems_v2.0.xml.gz");
		}
		signalConfigGroup.setSignalGroupsFile(testUtils.getClassInputDirectory() + "matsimData/signal_groups_no_13.xml");
		if (signalType.equals(SignalType.MS)){
			signalConfigGroup.setSignalControlFile(testUtils.getClassInputDirectory() + "matsimData/signal_control_no_13.xml");
		} else { // SignalType.BTU_OPT
			signalConfigGroup.setSignalControlFile(testUtils.getClassInputDirectory() + "btuOpt/signal_control_opt.xml");
		}
		
		// set brain exp beta
		config.planCalcScore().setBrainExpBeta( 2 );

		// choose between link to link and node to node routing
		// (only has effect if lanes are used)
		boolean link2linkRouting = true;
		config.controler().setLinkToLinkRoutingEnabled(link2linkRouting);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(link2linkRouting);
		config.travelTimeCalculator().setCalculateLinkTravelTimes(true);

		// set travelTimeBinSize (only has effect if reRoute is used)
		config.travelTimeCalculator().setTraveltimeBinSize( 10 );

		config.travelTimeCalculator().setTravelTimeCalculatorType(TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may'15

		// define strategies:
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultStrategy.ReRoute.toString());
			if (scenarioType.equals(ScenarioType.BaseCaseContinued_BtuRoutes))
				strat.setWeight(0.0); // no ReRoute, fix route choice set
			else // MatsimRoutes or BaseCase
				strat.setWeight(0.1);
			strat.setDisableAfter(config.controler().getLastIteration() - 50);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
			strat.setWeight(0.9);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}

		// choose maximal number of plans per agent. 0 means unlimited
		if (scenarioType.equals(ScenarioType.BaseCaseContinued_BtuRoutes))
			config.strategy().setMaxAgentPlanMemorySize(0); //unlimited because ReRoute is switched off anyway
		else 
			config.strategy().setMaxAgentPlanMemorySize( 5 );

		config.qsim().setStuckTime( 3600 );
		config.qsim().setRemoveStuckVehicles(false);
		
		if (scenarioType.equals(ScenarioType.BaseCase)){
			config.qsim().setStorageCapFactor( 0.7 );
			config.qsim().setFlowCapFactor( 0.7 );
		} else { // BaseCaseContinued
			// use default: 1.0 (i.e. as it is in the BTU network)
		}
		
		config.qsim().setStartTime(3600 * 5); 

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.vspExperimental().setWritingOutputEvents(false);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setCreateGraphs(false);

		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.controler().setWritePlansInterval(config.controler().getLastIteration());

		// define activity types
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			dummyAct.setOpeningTime(5 * 3600);
			dummyAct.setLatestStartTime(10 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
		}
		{
			ActivityParams homeAct = new ActivityParams("home");
			homeAct.setTypicalDuration(15.5 * 3600);
			config.planCalcScore().addActivityParams(homeAct);
		}
		{
			ActivityParams workAct = new ActivityParams("work");
			workAct.setTypicalDuration(8.5 * 3600);
			workAct.setOpeningTime(7 * 3600);
			workAct.setClosingTime(17.5 * 3600);
			config.planCalcScore().addActivityParams(workAct);
		}
		
		return config;
	}
	
}
