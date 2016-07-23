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

import static playground.boescpa.ivtBaseline.TestRunBaseline.createFacilities;
import static playground.boescpa.ivtBaseline.TestRunBaseline.createPrefs;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.boescpa.ivtBaseline.preparation.IVTConfigCreator;
import playground.boescpa.lib.tools.fileCreation.F2LConfigGroup;
import playground.boescpa.lib.tools.fileCreation.F2LCreator;

/**
 * What is it for?
 *
 * @author boescpa
 */
public class TestRunIVTBaselineCalibration {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testScenario() {
		// NOTE: The original path setup didn't work any more after paths in the config were made relative to the location where the config
		// file was, while all other paths are still relative to the JVM root.  Thus the setup is a bit different now.  kai, jul'16

		// write, and then read:		
		final String pathToFacilities = "facilities.xml";
		final String pathToPrefs = "prefs.xml";
		final String pathToPopulation = "population.xml";
		final String pathToF2L = "f2l.f2l";
		{

			final String pathToInitialPopulation = "population2.xml";
			final String pathToOnlyStreetNetwork = "onlystreetnetwork.xml";

			// ---

			// write, and then read:

			// ---

			Scenario tempScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimNetworkReader(tempScenario.getNetwork()).readFile(utils.getClassInputDirectory() + pathToOnlyStreetNetwork);
			new PopulationReader(tempScenario).readFile(utils.getClassInputDirectory() + pathToInitialPopulation);
			createPrefs(tempScenario, utils.getClassInputDirectory() + pathToPrefs);
			createFacilities(tempScenario, utils.getClassInputDirectory() + pathToFacilities);
			F2LCreator.createF2L(tempScenario, utils.getClassInputDirectory() + pathToF2L);
			new PopulationWriter(tempScenario.getPopulation()).write(utils.getClassInputDirectory() + pathToPopulation);

		}

		final String pathToConfig = utils.getClassInputDirectory() + "config.xml";

		// not included in input files directory "automagic":
		final String pathToPTLinksToMonitor = utils.getClassInputDirectory() + "ptLinksToMonitor.txt";
		final String pathToPTStationsToMonitor = utils.getClassInputDirectory() + "ptStationsToMonitor.txt"; 
		final String pathToStreetLinksDailyToMonitor = utils.getClassInputDirectory() + "streetLinksDailyToMonitor.txt";
		final String pathToStreetLinksHourlyToMonitor = utils.getClassInputDirectory() + "streetLinksHourlyToMonitor.txt";

		final String pathToNetwork = "multimodalnetwork.xml";
		final String pathToSchedule = "transitschedule.xml";
//		final String pathToVehicles = "test/scenarios/pt-tutorial/transitVehicles.xml";
		final String pathToVehicles = "transitVehicles.xml";


		// create config
		String[] argsConfig = {pathToConfig, "100"};
		IVTConfigCreator.main(argsConfig);
		Config config = ConfigUtils.loadConfig(pathToConfig, new F2LConfigGroup());
		config.setParam("controler", "outputDirectory", utils.getOutputDirectory() + "output/");
		// Reduce iterations to one write out interval + 1
		config.setParam("controler", "lastIteration", "3");
		// Set files
		config.setParam("facilities", "inputFacilitiesFile", pathToFacilities);
		config.setParam("f2l", "inputF2LFile", utils.getClassInputDirectory() + pathToF2L); // not included in input files directory "automagic"
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

		String[] argsSim = {pathToConfig, pathToPTLinksToMonitor, pathToPTStationsToMonitor,
				pathToStreetLinksDailyToMonitor, pathToStreetLinksHourlyToMonitor};
		RunIVTBaselineCalibration.main(argsSim);
	}
}
