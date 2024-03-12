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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

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
		String resultPopulation = "testPopulation.xml.gz";

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
				"--shapeCRS", shapeCRS,
				"--nameOutputPopulation", resultPopulation,
				"--pathOutput", output);

		// test results of complete run before
		Config config = ConfigUtils.createConfig();
		Scenario scenarioWOSolution = ScenarioUtils.createScenario(config);
		Scenario scenarioWSolution = ScenarioUtils.createScenario(config);
		Population population = PopulationUtils.readPopulation(utils.getOutputDirectory() + "testPopulation.xml.gz");
		String carriersWOSolutionFileLocation = utils.getOutputDirectory() + "test.output_CarrierDemand.xml";
		String carriersWSolutionFileLocation = utils.getOutputDirectory() + "test.output_CarrierDemandWithPlans.xml";
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getOutputDirectory() + "test.output_carriersVehicleTypes.xml.gz");

		freightCarriersConfigGroup.setCarriersFile(carriersWOSolutionFileLocation);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenarioWOSolution);
		freightCarriersConfigGroup.setCarriersFile(carriersWSolutionFileLocation);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenarioWSolution);

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

		for (File caculatedFile : Objects.requireNonNull(
			Objects.requireNonNull(new File(utils.getOutputDirectory() + "calculatedData").listFiles()))) {
			MatsimTestUtils.assertEqualFilesLineByLine(
				utils.getPackageInputDirectory() + "calculatedData/" + caculatedFile.getName(),
				caculatedFile.getAbsolutePath());
		}

		// compare events
		String expected = utils.getPackageInputDirectory() + "test.output_events.xml.gz" ;
		String actual = utils.getOutputDirectory() + "test.output_events.xml.gz" ;
		EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
		Assertions.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result );
	}
}
