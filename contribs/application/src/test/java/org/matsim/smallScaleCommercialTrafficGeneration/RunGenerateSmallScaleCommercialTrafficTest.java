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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
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

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testMainRunAndResults() {
		String inputDataDirectory = utils.getPackageInputDirectory() + "config_demand.xml";
		String output = utils.getOutputDirectory();
		String sample = "0.1";
		String jspritIterations = "2";
		String creationOption = "createNewCarrierFile";
		String landuseConfiguration = "useExistingDataDistribution";
		String smallScaleCommercialTrafficType = "commercialPersonTraffic";
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
				"--smallScaleCommercialTrafficType", smallScaleCommercialTrafficType,
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
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);

		for (File outputFiles : Objects.requireNonNull(Objects.requireNonNull(outputFolder.listFiles())[0].listFiles())) {

			if (outputFiles.getName().contains("pct_plans.xml.gz"))
				population = PopulationUtils.readPopulation(outputFiles.getPath());
			if (outputFiles.getName().contains("output_CarrierDemand.xml"))
				carriersWOSolutionFileLocation = outputFiles.getPath();
			if (outputFiles.getName().contains("output_CarrierDemandWithPlans.xml"))
				carriersWSolutionFileLocation = outputFiles.getPath();
			if (outputFiles.getName().contains("output_carriersVehicleTypes.xml.gz"))
				freightCarriersConfigGroup.setCarriersVehicleTypesFile(outputFiles.getPath());
		}

		freightCarriersConfigGroup.setCarriersFile(carriersWOSolutionFileLocation);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenarioWOSolution);
		freightCarriersConfigGroup.setCarriersFile(carriersWSolutionFileLocation);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenarioWSolution);

		assert population != null;
		for (Person person : population.getPersons().values()) {
			Assertions.assertNotNull(person.getSelectedPlan());
			Assertions.assertTrue(person.getAttributes().getAsMap().containsKey("tourStartArea"));
			Assertions.assertTrue(person.getAttributes().getAsMap().containsKey("vehicles"));
			Assertions.assertTrue(person.getAttributes().getAsMap().containsKey("subpopulation"));
			Assertions.assertTrue(person.getAttributes().getAsMap().containsKey("purpose"));
		}

		Assertions.assertEquals(CarriersUtils.addOrGetCarriers(scenarioWSolution).getCarriers().size(),
				CarriersUtils.addOrGetCarriers(scenarioWOSolution).getCarriers().size(), 0);
		int countedTours = 0;
		for (Carrier carrier_withSolution : CarriersUtils.addOrGetCarriers(scenarioWSolution).getCarriers().values()) {
			countedTours += carrier_withSolution.getSelectedPlan().getScheduledTours().size();
		}
		Assertions.assertEquals(population.getPersons().size(), countedTours, 0);
	}
}
