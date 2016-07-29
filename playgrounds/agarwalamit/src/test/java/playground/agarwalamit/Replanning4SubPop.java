/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author amit
 */

public class Replanning4SubPop {
	
	@Rule public MatsimTestUtils helper = new MatsimTestUtils();
	
	private final static String EQUIL_NETWORK = "../../matsim/examples/equil/network.xml";
	private final static String PLANS = "../../matsim/examples/tutorial/programming/MultipleSubpopulations/plans.xml";
	private final static String OBJECT_ATTRIBUTES = "../../matsim/examples/tutorial/programming/MultipleSubpopulations/personAtrributes.xml";
	private final static String CONFIG = "../../matsim/examples/tutorial/programming/MultipleSubpopulations/config.xml";

	private static final String SUBPOP_ATTRIB_NAME = "subpopulation";
	private static final String SUBPOP1_NAME = "time";
	private static final String SUBPOP2_NAME = "reroute";
	
	@Test
	public void test(){
		
		{
			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			MatsimNetworkReader mnr = new MatsimNetworkReader(sc.getNetwork());
			mnr.readFile(EQUIL_NETWORK);
		}
		
		// building and running the simulation based on the example scenario:
		Config config = ConfigUtils.createConfig(); 
		ConfigUtils.loadConfig(config, CONFIG);
		config.plans().setInputFile(PLANS);
		config.plans().setInputPersonAttributeFile(OBJECT_ATTRIBUTES);
		config.plans().setSubpopulationAttributeName(SUBPOP_ATTRIB_NAME); /* This is the default anyway. */
		config.network().setInputFile(EQUIL_NETWORK);
		config.controler().setOutputDirectory(helper.getOutputDirectory());
		config.controler().setMobsim("qsim");
		config.controler().setLastIteration(1);
		{
			/* Set up the 'time' subpopulation to only consider time allocation 
			 * as a strategy, 20% of the time, and the balance using ChangeExpBeta. */
			StrategySettings timeStrategySettings = new StrategySettings();
			timeStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
			timeStrategySettings.setSubpopulation(SUBPOP1_NAME);
			timeStrategySettings.setWeight(0.2);
			config.strategy().addStrategySettings(timeStrategySettings);
			
			StrategySettings changeExpBetaStrategySettings = new StrategySettings();
			changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation(SUBPOP1_NAME);
			changeExpBetaStrategySettings.setWeight(0.8);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		}
		{
			StrategySettings timeStrategySettings = new StrategySettings();
			timeStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
			timeStrategySettings.setSubpopulation(SUBPOP2_NAME);
			timeStrategySettings.setWeight(0.2);
			config.strategy().addStrategySettings(timeStrategySettings);

			StrategySettings changeExpBetaStrategySettings = new StrategySettings();
			changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation(SUBPOP2_NAME);
			changeExpBetaStrategySettings.setWeight(0.8);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		}
		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
		Assert.assertTrue(true);
	}
}