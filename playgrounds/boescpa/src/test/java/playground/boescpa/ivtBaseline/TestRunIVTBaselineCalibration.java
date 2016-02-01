/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.boescpa.ivtBaseline.preparation.IVTConfigCreator;
import playground.boescpa.lib.tools.fileCreation.F2LCreator;

import static playground.boescpa.ivtBaseline.TestRunBaseline.createFacilities;
import static playground.boescpa.ivtBaseline.TestRunBaseline.createPrefs;

/**
 * What is it for?
 *
 * @author boescpa
 */
public class TestRunIVTBaselineCalibration {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		final String pathToPTLinksToMonitor = utils.getClassInputDirectory() + "ptLinksToMonitor.txt";
		final String pathToPTStationsToMonitor = utils.getClassInputDirectory() + "ptStationsToMonitor.txt";

		final String pathToOnlyStreetNetwork = utils.getClassInputDirectory() + "onlystreetnetwork.xml";
		final String pathToNetwork = "test/scenarios/pt-tutorial/multimodalnetwork.xml";
		final String pathToInitialPopulation = utils.getClassInputDirectory() + "population.xml";
		final String pathToPopulation = utils.getOutputDirectory() + "population.xml";
		final String pathToPrefs = utils.getOutputDirectory() + "prefs.xml";
		final String pathToFacilities = utils.getOutputDirectory() + "facilities.xml";
		final String pathToF2L = utils.getOutputDirectory() + "f2l.f2l";
		final String pathToConfig = utils.getOutputDirectory() + "config.xml";
		final String pathToSchedule = utils.getClassInputDirectory() + "transitschedule.xml";
		final String pathToVehicles = "test/scenarios/pt-tutorial/transitVehicles.xml";

		Scenario tempScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(tempScenario.getNetwork()).readFile(pathToOnlyStreetNetwork);
		new MatsimPopulationReader(tempScenario).readFile(pathToInitialPopulation);
		createPrefs(tempScenario, pathToPrefs);
		createFacilities(tempScenario, pathToFacilities);
		F2LCreator.createF2L(tempScenario, pathToF2L);
		new PopulationWriter(tempScenario.getPopulation()).write(pathToPopulation);

		// create config
		String[] argsConfig = {pathToConfig, "100"};
		IVTConfigCreator.main(argsConfig);
		Config config = ConfigUtils.loadConfig(pathToConfig);
		config.setParam("controler", "outputDirectory", utils.getOutputDirectory() + "output/");
		// Reduce iterations to one write out interval + 1
		config.setParam("controler", "lastIteration", "11");
		// Set files
		config.setParam("facilities", "inputFacilitiesFile", pathToFacilities);
		config.setParam("f2l", "inputF2LFile", pathToF2L);
		config.setParam("households", "inputFile", "null");
		config.setParam("households", "inputHouseholdAttributesFile", "null");
		config.setParam("network", "inputNetworkFile", pathToNetwork);
		config.setParam("plans", "inputPersonAttributesFile", pathToPrefs);
		config.setParam("plans", "inputPlansFile", pathToPopulation);
		config.setParam("transit", "transitScheduleFile", pathToSchedule);
		config.setParam("transit", "vehiclesFile", pathToVehicles);
		// Set threads to 1
		config.setParam("global", "numberOfThreads", "1");
		config.setParam("parallelEventHandling", "numberOfThreads", "1");
		config.setParam("qsim", "numberOfThreads", "1");
		// Set counts interval to 5:
		config.setParam("counts", "writeCountsInterval", "5");
		config.setParam("ptCounts", "ptCountsInterval", "5");
		config.ptCounts().setCountsScaleFactor(10);
		new ConfigWriter(config).write(pathToConfig);

		String[] argsSim = {pathToConfig, pathToPTLinksToMonitor, pathToPTStationsToMonitor};
		RunIVTBaselineCalibration.main(argsSim);
	}

	@Test
	public void testScenario() {

	}
}
