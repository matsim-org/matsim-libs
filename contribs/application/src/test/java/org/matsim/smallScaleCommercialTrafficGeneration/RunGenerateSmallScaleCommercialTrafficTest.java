/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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
package org.matsim.smallScaleCommercialTrafficGeneration;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.Objects;

/**
 * @author Ricardo Ewert
 *
 */
public class RunGenerateSmallScaleCommercialTrafficTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMainRunAndResults() {
		String inputDataDirectory = utils.getPackageInputDirectory();
		String output = utils.getOutputDirectory();
		String sample = "0.1";
		String jspritIterations = "2";
		String creationOption = "createNewCarrierFile";
		String landuseConfiguration = "useExistingDataDistribution";
		String trafficType = "commercialTraffic";
		String includeExistingModels = "true";
		String zoneShapeFileName = utils.getPackageInputDirectory() + "/shp/testZones.shp";
		String buildingsShapeFileName = utils.getPackageInputDirectory() + "/shp/testBuildings.shp";
		String landuseShapeFileName = utils.getPackageInputDirectory() + "/shp/testLanduse.shp";
		String shapeCRS = "EPSG:4326";

		new GenerateSmallScaleCommercialTrafficDemand().execute(
				inputDataDirectory,
				"--sample", sample,
				"--jspritIterations", jspritIterations,
				"--creationOption", creationOption,
				"--landuseConfiguration", landuseConfiguration,
				"--trafficType", trafficType,
				"--includeExistingModels",
				"--zoneShapeFileName", zoneShapeFileName,
				"--buildingsShapeFileName", buildingsShapeFileName,
				"--landuseShapeFileName", landuseShapeFileName,
				"--shapeCRS", shapeCRS);

		// test results of complete run before
		Config config = ConfigUtils.createConfig();
		Scenario scenarioWOSolution = ScenarioUtils.createScenario(config);
		Scenario scenarioWSolution = ScenarioUtils.createScenario(config);
		File outputFolder = Objects.requireNonNull(new File(output).listFiles())[0];
		Population population = null;
		String carriersWOSolutionFileLocation = null;
		String carriersWSolutionFileLocation = null;
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);

		for (File outputFiles : Objects.requireNonNull(Objects.requireNonNull(outputFolder.listFiles())[0].listFiles())) {

			if (outputFiles.getName().contains("pct_plans.xml.gz"))
				population = PopulationUtils.readPopulation(outputFiles.getPath());
			if (outputFiles.getName().contains("output_CarrierDemand.xml"))
				carriersWOSolutionFileLocation = outputFiles.getPath();
			if (outputFiles.getName().contains("output_CarrierDemandWithPlans.xml"))
				carriersWSolutionFileLocation = outputFiles.getPath();
			if (outputFiles.getName().contains("output_carriersVehicleTypes.xml.gz"))
				freightConfigGroup.setCarriersVehicleTypesFile(outputFiles.getPath());
		}

		freightConfigGroup.setCarriersFile(carriersWOSolutionFileLocation);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenarioWOSolution);
		freightConfigGroup.setCarriersFile(carriersWSolutionFileLocation);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenarioWSolution);

		assert population != null;
		for (Person person : population.getPersons().values()) {
			Assert.assertNotNull(person.getSelectedPlan());
			Assert.assertTrue(person.getAttributes().getAsMap().containsKey("tourStartArea"));
			Assert.assertTrue(person.getAttributes().getAsMap().containsKey("vehicles"));
			Assert.assertTrue(person.getAttributes().getAsMap().containsKey("subpopulation"));
			Assert.assertTrue(person.getAttributes().getAsMap().containsKey("purpose"));
		}

		Assert.assertEquals(FreightUtils.addOrGetCarriers(scenarioWSolution).getCarriers().size(),
				FreightUtils.addOrGetCarriers(scenarioWOSolution).getCarriers().size(), 0);
		int countedTours = 0;
		for (Carrier carrier_withSolution : FreightUtils.addOrGetCarriers(scenarioWSolution).getCarriers().values()) {
			countedTours += carrier_withSolution.getSelectedPlan().getScheduledTours().size();
		}
		Assert.assertEquals(population.getPersons().size(), countedTours, 0);
	}
}
