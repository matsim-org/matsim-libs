/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
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

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		String pathToConfig = utils.getPackageInputDirectory() + "config_demand.xml";
		Path pathToDataDistributionToZones = Path.of(utils.getPackageInputDirectory()).resolve("dataDistributionPerZone.csv");
		String pathToCommercialFacilities = "commercialFacilities.xml.gz";
		String output = utils.getOutputDirectory();
		String sample = "0.1";
		String jspritIterations = "2";
		String creationOption = "createNewCarrierFile";
		String smallScaleCommercialTrafficType = "completeSmallScaleCommercialTraffic";
		String zoneShapeFileName = utils.getPackageInputDirectory() + "/shp/testZones.shp";
		String zoneShapeFileNameColumn = "name";
		String shapeCRS = "EPSG:4326";
		String resultPopulation = "testPopulation.xml.gz";

		new GenerateSmallScaleCommercialTrafficDemand().execute(
				pathToConfig,
				"--pathToDataDistributionToZones", pathToDataDistributionToZones.toString(),
				"--pathToCommercialFacilities", pathToCommercialFacilities,
				"--sample", sample,
				"--jspritIterations", jspritIterations,
				"--creationOption", creationOption,
				"--smallScaleCommercialTrafficType", smallScaleCommercialTrafficType,
				"--includeExistingModels",
				"--zoneShapeFileName", zoneShapeFileName,
				"--zoneShapeFileNameColumn", zoneShapeFileNameColumn,
				"--shapeCRS", shapeCRS,
				"--nameOutputPopulation", resultPopulation,
				"--pathOutput", output);

		// test results of complete run before
		Config config = ConfigUtils.createConfig();
		Scenario scenarioWOSolution = ScenarioUtils.createScenario(config);
		Scenario scenarioWSolution = ScenarioUtils.createScenario(config);
		Population population = PopulationUtils.readPopulation(utils.getOutputDirectory() + "testPopulation.xml.gz");
		String carriersWOSolutionFileLocation = utils.getOutputDirectory() + "test.output_carriers_noPlans.xml";
		String carriersWSolutionFileLocation = utils.getOutputDirectory() + "test.output_carriers_withPlans.xml";
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

			for (Plan plan : person.getPlans()) {
				List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
				Assertions.assertEquals("commercial_start", activities.getFirst().getType());
				Assertions.assertEquals("commercial_end", activities.getLast().getType());
				activities.forEach(activity -> {
					Assertions.assertNotNull(activity.getCoord());
					if (!activity.getType().equals("commercial_start") && !activity.getType().equals("commercial_end")) {
						Assertions.assertNotEquals(OptionalTime.undefined(), activity.getMaximumDuration());
					}
				});
			}
		}

		Assertions.assertEquals(CarriersUtils.addOrGetCarriers(scenarioWSolution).getCarriers().size(),
				CarriersUtils.addOrGetCarriers(scenarioWOSolution).getCarriers().size(), 0);
		int countedTours = 0;
		for (Carrier carrier_withSolution : CarriersUtils.addOrGetCarriers(scenarioWSolution).getCarriers().values()) {
			countedTours += carrier_withSolution.getSelectedPlan().getScheduledTours().size();
		}
		Assertions.assertEquals(population.getPersons().size(), countedTours, 0);

		for (File calculatedFile : Objects.requireNonNull(
			Objects.requireNonNull(new File(utils.getOutputDirectory() + "calculatedData").listFiles()))) {
			Map<String, Object2DoubleMap<String>> simulatedDataDistribution = readCSVInputAndCreateMap(calculatedFile.getAbsolutePath());
			Map<String, Object2DoubleMap<String>> existingDataDistribution = readCSVInputAndCreateMap(
				utils.getPackageInputDirectory() + "calculatedData/" + calculatedFile.getName());
			compareDataDistribution(calculatedFile.getName(), existingDataDistribution, simulatedDataDistribution);
		}

		// compare events
		String expected = utils.getPackageInputDirectory() + "test.output_events.xml.gz";
		String actual = utils.getOutputDirectory() + "test.output_events.xml.gz" ;
		ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
		Assertions.assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
	}

	/**
	 * Reads a CSV file and creates a map with the first column as a key and the rest as a map with the header as key and the value as value
	 *
	 * @param calculatedFile the file to read
	 * @return the map with the data distribution
	 */
	private static Map<String, Object2DoubleMap<String>> readCSVInputAndCreateMap(String calculatedFile) {
		Map<String, Object2DoubleMap<String>> dataDistribution = new HashMap<>();
		try (BufferedReader reader = IOUtils.getBufferedReader(calculatedFile)) {
			CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
				.setSkipHeaderRecord(true).build().parse(reader);
			for (CSVRecord record : parse) {
				System.out.println(record);
				dataDistribution.computeIfAbsent(record.get(0), k -> new Object2DoubleOpenHashMap<>());
				for (int i = 1; i < record.size(); i++) {
					if (i == 1 && (calculatedFile.contains("dataDistributionPerZone") || calculatedFile.contains("TrafficVolume_")))
						continue;
					dataDistribution.get(record.get(0)).put(parse.getHeaderNames().get(i), Double.parseDouble(record.get(i)));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return dataDistribution;
	}

	/**
	 * Compares the data distribution of two files
	 *
	 * @param calculatedFile            the file to compare
	 * @param existingDataDistribution  the existing data distribution
	 * @param simulatedDataDistribution the simulated data distribution
	 */
	private void compareDataDistribution(String calculatedFile, Map<String, Object2DoubleMap<String>> existingDataDistribution,
										 Map<String, Object2DoubleMap<String>> simulatedDataDistribution) {
		Assertions.assertEquals(existingDataDistribution.size(), simulatedDataDistribution.size());
		for (String key : existingDataDistribution.keySet()) {
			Object2DoubleMap<String> existingMap = existingDataDistribution.get(key);
			Object2DoubleMap<String> simulatedMap = simulatedDataDistribution.get(key);
			for (String subKey : existingMap.keySet()) {
				Assertions.assertEquals(existingMap.getDouble(subKey), simulatedMap.getDouble(subKey),
					"File: " + calculatedFile + "; Expected: " + existingMap.getDouble(subKey) + " but was: " + simulatedMap.getDouble(
						subKey) + " for key: " + key + " and subKey: " + subKey);
			}
		}
	}
}
