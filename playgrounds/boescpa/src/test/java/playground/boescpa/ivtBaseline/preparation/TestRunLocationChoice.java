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

package playground.boescpa.ivtBaseline.preparation;

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
import playground.boescpa.ivtBaseline.TestRunBaseline;
import playground.boescpa.lib.tools.fileCreation.F2LConfigGroup;
import playground.boescpa.lib.tools.fileCreation.F2LCreator;

/**
 * @author boescpa
 */
public class TestRunLocationChoice {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {

		final String pathToOnlyStreetNetwork = utils.getClassInputDirectory() + "onlystreetnetwork.xml";
		final String pathToNetwork = "test/scenarios/pt-tutorial/multimodalnetwork.xml";
		final String pathToInitialPopulation = utils.getClassInputDirectory() + "population.xml";
		final String pathToPopulation = utils.getOutputDirectory() + "population.xml";
		final String pathToPrefs = utils.getOutputDirectory() + "prefs.xml";
		final String pathToFacilities = utils.getOutputDirectory() + "facilities.xml";
		final String pathToF2L = utils.getOutputDirectory() + "f2l.f2l";
		final String pathToConfig = utils.getOutputDirectory() + "config.xml";
		final String pathToSchedule = "test/scenarios/pt-tutorial/transitschedule.xml";
		final String pathToVehicles = "test/scenarios/pt-tutorial/transitVehicles.xml";

		Scenario tempScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(tempScenario.getNetwork()).readFile(pathToOnlyStreetNetwork);
		new MatsimPopulationReader(tempScenario).readFile(pathToInitialPopulation);
		TestRunBaseline.createPrefs(tempScenario, pathToPrefs);
		TestRunBaseline.createFacilities(tempScenario, pathToFacilities);
		F2LCreator.createF2L(tempScenario, pathToF2L);
		new PopulationWriter(tempScenario.getPopulation()).write(pathToPopulation);

		// create config
		String[] argsConfig = {pathToConfig, "100"};
		ChooseSecondaryFacilitiesConfigCreator.main(argsConfig);
		Config config = ConfigUtils.loadConfig(pathToConfig, new F2LConfigGroup());
		config.setParam("controler", "outputDirectory", utils.getOutputDirectory() + "output/");
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
			// handle location choice
		config.setParam("locationchoice", "prefsFile", pathToPrefs);
		config.setParam("locationchoice", "flexible_types", "shop");
		config.setParam("locationchoice", "epsilonScaleFactors", "0.3");
			// Set threads to 1
		config.setParam("global", "numberOfThreads", "1");
		config.setParam("parallelEventHandling", "numberOfThreads", "1");
		config.setParam("qsim", "numberOfThreads", "1");
		new ConfigWriter(config).write(pathToConfig);

		String[] argsSim = {pathToConfig};
		RunLocationChoice.main(argsSim);
	}

	@Test
	public void testScenario() {

	}
}
