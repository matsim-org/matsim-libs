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

package org.matsim.integration.replanning;

import java.io.File;
import java.net.MalformedURLException;
import java.util.EnumSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControllerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ReRoutingIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private Scenario loadScenario() {
		Config config = utils.loadConfig(utils.getClassInputDirectory() +"config.xml");
		config.network().setInputFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz").toString());
		config.plans().setInputFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "plans_hwh_1pct.xml.gz").toString());
		config.qsim().setTimeStepSize(10.0);
		config.qsim().setStuckTime(100.0);
		config.qsim().setRemoveStuckVehicles(true);
		config.controller().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		config.controller().setLastIteration(1);
		/* linear interpolate the into time bins aggregated travel time data to avoid artifacts at the boundaries of time bins:
		 * e.g. a first time bin with aggregated travel time of 90 seconds and a second time bin with 45 seconds; time bin size 60;
		 * i.e. consolidateData-method in TravelTimeCalculator will accept this difference; imagine an requested route starting 2
		 * seconds before the end of the first time bin, another route starts 2 seconds after the start of the second time bin; then
		 * the second one will arrive 41 seconds earlier than the first. Depending on the algorithm, some routers will detect this,
		 * some not (see MATSim-730), which is why we decided to test the linear interpolated travel time data here (which does not
		 * contain this artifacts). theresa, sep'17
		 * */
		config.travelTimeCalculator().setTravelTimeGetterType("linearinterpolation");

		/*
		 * The input plans file is not sorted. After switching from TreeMap to LinkedHashMap
		 * to store the persons in the population, we have to sort the population manually.
		 * cdobler, oct'11
		 */
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationUtils.sortPersons(scenario.getPopulation());
		return scenario;
	}

	@Test
	void testReRoutingDijkstra() throws MalformedURLException {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controller().setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		Controler controler = new Controler(scenario);
		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}

	@Test
	void testReRoutingAStarLandmarks() throws MalformedURLException {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controller().setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);
		Controler controler = new Controler(scenario);
		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}

	@Test
	void testReRoutingSpeedyALT() throws MalformedURLException {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controller().setRoutingAlgorithmType(RoutingAlgorithmType.SpeedyALT);
		Controler controler = new Controler(scenario);
		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate("plans_speedyALT.xml.gz");
	}

	private void evaluate() throws MalformedURLException {
		this.evaluate("plans.xml.gz");
	}

	private final static Logger LOG = LogManager.getLogger(ReRoutingIT.class);
	private void evaluate(String plansFilename) throws MalformedURLException {
		Config config = utils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.network().setInputFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz").toString());
		config.plans().setInputFile(plansFilename);
		Scenario referenceScenario = ScenarioUtils.loadScenario(config);

		config.plans().setInputFile(new File(utils.getOutputDirectory() + "ITERS/it.1/1.plans.xml.gz").toURI().toURL().toString());
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Gbl.startMeasurement();
		final boolean isEqual = PopulationUtils.equalPopulation(referenceScenario.getPopulation(), scenario.getPopulation());
		Gbl.printElapsedTime();
		if ( !isEqual ) {
			new PopulationWriter(referenceScenario.getPopulation(), scenario.getNetwork()).write(utils.getOutputDirectory() + "/reference_population.xml.gz");
			new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(utils.getOutputDirectory() + "/output_population.xml.gz");
		}
		Assertions.assertTrue(isEqual, "different plans files.");
	}

}
