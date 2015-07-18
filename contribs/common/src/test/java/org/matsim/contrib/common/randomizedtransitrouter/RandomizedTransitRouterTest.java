/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.randomizedtransitrouter;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class RandomizedTransitRouterTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	@Ignore
	public final void test() {
		String scenarioDir = utils.getPackageInputDirectory() ;
		String outputDir = utils.getOutputDirectory() ;

		Config config = ConfigUtils.createConfig();
		
		config.network().setInputFile(scenarioDir + "/network.xml");
		config.plans().setInputFile(scenarioDir + "/population.xml");

		config.transit().setTransitScheduleFile(scenarioDir + "/transitschedule.xml");
		config.transit().setVehiclesFile( scenarioDir + "/transitVehicles.xml" );
		config.transit().setUseTransit(true);
		
		config.controler().setOutputDirectory( outputDir );
		config.controler().setLastIteration(100);
		{
			ActivityParams params = new ActivityParams("home") ;
			params.setTypicalDuration( 6*3600. );
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("education_100") ;
			params.setTypicalDuration( 6*3600. );
			config.planCalcScore().addActivityParams(params);
		}
		{
			StrategySettings stratSets = new StrategySettings(ConfigUtils.createAvailableStrategyId(config)) ;
			stratSets.setStrategyName( DefaultStrategy.ReRoute.name() );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings(ConfigUtils.createAvailableStrategyId(config)) ;
			stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.name() );
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}
		
		config.vspExperimental().setWritingOutputEvents(true);
		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn );
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		controler.run();
		
		// ---
		
	}

}
