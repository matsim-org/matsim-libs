/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoutingTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.integration.always;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

import java.util.EnumSet;

public class ReRoutingIT extends MatsimTestCase {

	/*package*/ static final Logger log = Logger.getLogger(ReRoutingIT.class);

	
	private Scenario loadScenario() {
		Config config = loadConfig(getClassInputDirectory() + "config.xml");
		config.qsim().setTimeStepSize(10.0);
        config.qsim().setStuckTime(100.0);
        config.qsim().setRemoveStuckVehicles(true);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		config.controler().setLastIteration(1);

		/*
		 * The input plans file is not sorted. After switching from TreeMap to LinkedHashMap
		 * to store the persons in the population, we have to sort the population manually.  
		 * cdobler, oct'11
		 */
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationUtils.sortPersons(scenario.getPopulation());
		return scenario;
	}
	
	public void testReRoutingDijkstra() {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}

	public void testReRoutingFastDijkstra() {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastDijkstra);
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}
	
	/**
	 * This test seems to have race conditions somewhere (i.e. it fails intermittently without code changes). kai, aug'13
	 */
	public void testReRoutingAStarLandmarks() {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}

	public void testReRoutingFastAStarLandmarks() {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}
	
	private void evaluate() {
		Config config = loadConfig(getClassInputDirectory() + "config.xml");
		final String originalFileName = getInputDirectory() + "1.plans.xml.gz";
		final String revisedFileName = getOutputDirectory() + "ITERS/it.1/1.plans.xml.gz";
		
		Scenario expectedPopulation = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(expectedPopulation.getNetwork()).readFile(config.network().getInputFile());
		new MatsimPopulationReader(expectedPopulation).readFile(originalFileName);
		Scenario actualPopulation = ScenarioUtils.createScenario(config);
	
		new MatsimPopulationReader(actualPopulation).readFile(revisedFileName);

		Assert.assertTrue("different plans files", 
				PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation()));		
	}

}
