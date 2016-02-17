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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;
import scenarios.illustrative.analysis.TtListenerToBindAndWriteAnalysis;
import scenarios.illustrative.braess.analysis.TtAnalyzeBraess;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation.InitRoutes;

/**
 * This test compares the simulation results of two runs, that only differ by
 * the population handling.
 * 
 * The first run creates the population in code. The second run reads a
 * population file, that corresponds to the one that is written by the first.
 * 
 * If one do not specify the time format via
 * Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMMSSDOTSS);
 * differences may occur by activity end times that are non integer values
 * (because they are written as integer values in the plans file). 
 * 
 * Further differences may occur by different person order: In code the persons
 * are created with increasing id's. The PopulationWriter sorts them
 * lexicographically before writing them.
 * This test avoids this differences by sorting the Population after creation.
 * 
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
		// specify global time format
		Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMMSSDOTSS);
		
		Tuple<TtAbstractAnalysisTool,Population> createResults = run(true);
		Tuple<TtAbstractAnalysisTool,Population> readResults = run(false);
		
		// compare populations regarding activity end times
		for (Person pRead : readResults.getSecond().getPersons().values()){
			Person pCreate = createResults.getSecond().getPersons().get(pRead.getId());
			
			for (Plan planRead : pRead.getPlans()){
				for (Plan planCreate : pCreate.getPlans()){
					Activity startActRead = (Activity) planRead.getPlanElements().get(0);
					Activity startActCreate = (Activity) planCreate.getPlanElements().get(0);
					Assert.assertEquals("activity end times differ", startActRead.getEndTime(), startActCreate.getEndTime(), MatsimTestUtils.EPSILON);
				}
			}
		}
		
		// compare results
		log.info("the total travel times are: " + readResults.getFirst().getTotalTT() + " and " + createResults.getFirst().getTotalTT());
		log.info("the route distributions are: " + readResults.getFirst().getRouteUsers()[0] + ", " + readResults.getFirst().getRouteUsers()[1] + ", " + readResults.getFirst().getRouteUsers()[2] 
				+ " and " + createResults.getFirst().getRouteUsers()[0] + ", " + createResults.getFirst().getRouteUsers()[1] + ", " + createResults.getFirst().getRouteUsers()[2]);
		Assert.assertArrayEquals("route distributions differ", readResults.getFirst().getRouteUsers(), createResults.getFirst().getRouteUsers());
		Assert.assertEquals("total travel times differ", readResults.getFirst().getTotalTT(), createResults.getFirst().getTotalTT(), MatsimTestUtils.EPSILON);
	}

	/**
	 * runs the test scenario
	 * 
	 * @param createPopulation
	 *            creates the population in code if true; reads the identical
	 *            population from file if false
	 * @return the handler that contains the results
	 */
	private Tuple<TtAbstractAnalysisTool,Population> run(boolean createPopulation) {
		
		Config config = defineConfig(createPopulation);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (createPopulation){
			createPopulation(scenario);
			// sort the population. Necessary, since the Map in PopulationImpl
			// is no longer a TreeMap but a LinkedHashMap, and PopulationWriter
			// sorts the Population before writing it
			PopulationUtils.sortPersons(scenario.getPopulation());
		}
		
		Controler controler = new Controler(scenario);
					
		// add a controller listener to analyze results
		TtAbstractAnalysisTool handler = new TtAnalyzeBraess();
		controler.addControlerListener(new TtListenerToBindAndWriteAnalysis(scenario, handler, false));
		
		controler.run();
		
		return new Tuple<TtAbstractAnalysisTool,Population>(handler,scenario.getPopulation());
	}
	
	private Config defineConfig(boolean createPopulation) {
		Config config = ConfigUtils.createConfig();

		// set network and population
		config.network().setInputFile(testUtils.getClassInputDirectory() + "network_cap2000.xml");
		if (!createPopulation){ // read population
			config.plans().setInputFile(testUtils.getClassInputDirectory() + "plans2000_initRoutes_exactTime.xml");
		}

		// set number of iterations
		config.controler().setLastIteration(1);

		// define strategies:
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
			strat.setWeight(1.0);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}

		config.controler().setOutputDirectory(createPopulation? testUtils.getOutputDirectory() + "create/" : testUtils.getOutputDirectory() + "read/");
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
