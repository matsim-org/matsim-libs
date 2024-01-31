/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.matrixbasedptrouter;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestPopulation;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class MatrixBasedPtRouterIT {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@TempDir
	public File folder;

	/**
	 * This method tests the travel time computation with pseudo pt.
	 * The scenario contains a simple network (9 nodes and 9 links, rangeX: 0,...,200, rangeY: 0,...,200) and
	 * a single person who lives at node 1 (0,0) and works at node 3 (0,200) and uses pt.
	 * A single MATSim run is executed. Afterwards, the travel time of the person's executed plan and
	 * the travel time computed by the PtMatrix are compared (should be equal).
	 */
	@Test
	void testIntegration() throws IOException {

		String path = utils.getOutputDirectory();



		//a dummy network is created and written into the output directory
		Network network = CreateTestNetwork.createTestNetwork();
		new NetworkWriter(network).write(path+"network.xml");

		//a dummy population of one person is created and written into the output directory
		Population population = CreateTestPopulation.createTestPtPopulation(1, new Coord((double) 0, (double) 0), new Coord((double) 0, (double) 200));
		new PopulationWriter(population, network).write(path+"plans.xml");

		//dummy csv files for pt stops, travel times and travel distances fitting into the dummy network are created
		String stopsLocation = CreateTestNetwork.createTestPtStationCSVFile( new File(folder, "ptStops.csv"));
		String timesLocation = CreateTestNetwork.createTestPtTravelTimesAndDistancesCSVFile(new File(folder, "ptTravelInfo.csv"));

		//add stops, travel times and travel distances file to the pseudo pt config group
		final MatrixBasedPtRouterConfigGroup matrixBasedPtRouterConfigGroup = new MatrixBasedPtRouterConfigGroup();
		matrixBasedPtRouterConfigGroup.setUsingPtStops(true);
		matrixBasedPtRouterConfigGroup.setUsingTravelTimesAndDistances(true);
		matrixBasedPtRouterConfigGroup.setPtStopsInputFile(stopsLocation);
		matrixBasedPtRouterConfigGroup.setPtTravelTimesInputFile(timesLocation);
		matrixBasedPtRouterConfigGroup.setPtTravelDistancesInputFile(timesLocation);

		//create a new config file and add a config group for pseudo pt
		Config config = ConfigUtils.createConfig() ;
		config.addModule(matrixBasedPtRouterConfigGroup) ;



		//modification of the config according to what's needed
		config.controller().setMobsim("qsim");
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile(path+"network.xml");
		config.plans().setInputFile(path+"plans.xml");

		//add home and work activity to plansCalcScoreConfigGroup
		config.scoring().addParam("activityType_0", "home");
		config.scoring().addParam("activityTypicalDuration_0", "43200");
		config.scoring().addParam("activityType_1", "work");
		config.scoring().addParam("activityTypicalDuration_1", "28800");


		Scenario scenario = ScenarioUtils.loadScenario(config);
		BoundingBox nbb = BoundingBox.createBoundingBox(network);
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.routing(), nbb, ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.GROUP_NAME, MatrixBasedPtRouterConfigGroup.class));

		Controler controler = new Controler(scenario) ;
		controler.addOverridingModule(new MatrixBasedPtModule());
		controler.run();

		// compute the travel time from home to work activity
		double ttime = ptMatrix.getTotalTravelTime_seconds(new Coord((double) 0, (double) 0), new Coord((double) 0, (double) 200));

		// get the actual travel time from the person's plan
		Person person = controler.getScenario().getPopulation().getPersons().values().iterator().next();
		double actualTtime = ((Leg)person.getSelectedPlan().getPlanElements().get(1)).getTravelTime().seconds();

		//compare computed and actual travel time
		Assertions.assertEquals(ttime, actualTtime, 0);

	}

}
