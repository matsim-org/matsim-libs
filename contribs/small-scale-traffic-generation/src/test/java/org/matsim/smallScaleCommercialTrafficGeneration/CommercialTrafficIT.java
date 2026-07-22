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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.PURPOSE;
import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.SUBPOPULATION;
import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.TOUR_ID;
import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.TOUR_START_AREA;

/**
 * @author Ricardo Ewert
 *
 */
public class CommercialTrafficIT {
	private static final Logger log = LogManager.getLogger( CommercialTrafficIT.class );

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

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
		String shapeCRS = "Atlantis";
		String resultPopulation = "testPopulation.xml.gz";
		String network = String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "grid9x9.xml"));

		new GenerateSmallScaleCommercialTrafficDemand().execute(
			pathToConfig,
			"--pathToDataDistributionToZones", pathToDataDistributionToZones.toString(),
			"--pathToCommercialFacilities", pathToCommercialFacilities,
			"--network", network,
			"--sample", sample,
			"--jspritIterations", jspritIterations,
			"--creationOption", creationOption,
			"--smallScaleCommercialTrafficType", smallScaleCommercialTrafficType,
			"--additionalTravelBufferPerIterationInMinutes", "10",
			"--includeExistingModels",
			"--zoneShapeFileName", zoneShapeFileName,
			"--zoneShapeFileNameColumn", zoneShapeFileNameColumn,
			"--shapeCRS", shapeCRS,
			"--nameOutputPopulation", resultPopulation,
			"--pathOutput", output,
			"--resistanceFactor_commercialPersonTraffic", "0.3",
			"--resistanceFactor_goodsTraffic", "0.2",
			"--MATSimIterationsAfterDemandGeneration", "0",
			"--factorForTravelBufferCalculation", "1.2",
			"--maxNumberOfLoopsForVRPSolving", "2");

		// test results of complete run before
		Config config = ConfigUtils.createConfig();
		Scenario scenarioWOSolution = ScenarioUtils.createScenario(config);
		Scenario scenarioWSolution = ScenarioUtils.createScenario(config);
		Population population = PopulationUtils.readPopulation(utils.getOutputDirectory() + "testPopulation.xml.gz");
		String carriersWOSolutionFileLocation = utils.getOutputDirectory() + "test.output_carriers_unsolvedVRP.xml.gz";
		String carriersWSolutionFileLocation = utils.getOutputDirectory() + "test.output_carriers_solvedVRP.xml.gz";
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getOutputDirectory() + "test.output_vehicles.xml.gz");

		freightCarriersConfigGroup.setCarriersFile(carriersWOSolutionFileLocation);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenarioWOSolution);
		freightCarriersConfigGroup.setCarriersFile(carriersWSolutionFileLocation);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenarioWSolution);

		for (Person person : population.getPersons().values()) {
			Assertions.assertNotNull(person.getSelectedPlan());
//			Assertions.assertTrue(person.getAttributes().getAsMap().containsKey( TOUR_START_AREA ) );
			if( !person.getAttributes().getAsMap().containsKey( TOUR_START_AREA ) ) {
				log.warn("does not contain TOUR_START_AREA; person=" + person.getId() );
			}
			Assertions.assertTrue(person.getAttributes().getAsMap().containsKey( TOUR_ID ) );
			Assertions.assertTrue(person.getAttributes().getAsMap().containsKey( SUBPOPULATION ) );
			Assertions.assertTrue(person.getAttributes().getAsMap().containsKey( PURPOSE ) );

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

	@Test
	void testMainRunAndResultsWithCarrierParts() throws IOException {
		String pathToConfig = utils.getPackageInputDirectory() + "config_demand.xml";
		Path pathToDataDistributionToZones = Path.of(utils.getPackageInputDirectory()).resolve("dataDistributionPerZone.csv");
		String pathToCommercialFacilities = "commercialFacilities.xml.gz";
		Path output = Path.of(utils.getOutputDirectory()).resolve("carrierPartsRun");
		Files.createDirectories(output);
		String network = String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "grid9x9.xml"));
		String zoneShapeFileName = utils.getPackageInputDirectory() + "/shp/testZones.shp";

		List<String> commonArgs = new ArrayList<>(List.of(
			pathToConfig,
			"--pathToDataDistributionToZones", pathToDataDistributionToZones.toString(),
			"--pathToCommercialFacilities", pathToCommercialFacilities,
			"--network", network,
			"--sample", "0.1",
			"--jspritIterations", "2",
			"--creationOption", "createNewCarrierFile",
			"--smallScaleCommercialTrafficType", "completeSmallScaleCommercialTraffic",
			"--additionalTravelBufferPerIterationInMinutes", "10",
			"--includeExistingModels",
			"--zoneShapeFileName", zoneShapeFileName,
			"--zoneShapeFileNameColumn", "name",
			"--shapeCRS", "Atlantis",
			"--nameOutputPopulation", "testPopulation.xml.gz",
			"--resistanceFactor_commercialPersonTraffic", "0.3",
			"--resistanceFactor_goodsTraffic", "0.2",
			"--MATSimIterationsAfterDemandGeneration", "0",
			"--factorForTravelBufferCalculation", "1.2",
			"--maxNumberOfLoopsForVRPSolving", "2"
		));

		// Step 1: Create the shared unsolved carrier file without running jsprit.
		List<String> initArgs = new ArrayList<>(commonArgs);
		initArgs.addAll(List.of("--pathOutput", output.toString(), "--createSmallScaleCommercialCarrierFileOnly"));
		new GenerateSmallScaleCommercialTrafficDemand().execute(initArgs.toArray(new String[0]));

		// Step 2: Count all jobs from the shared carrier file as the reference for the part runs.
		Path sharedCarrierFile = output.resolve("test.output_carriers_unsolvedVRP.xml.gz");
		Path carrierVehicleTypesFile = output.resolve("test.output_carriersVehicleTypes.xml.gz");
		Assertions.assertTrue(Files.exists(sharedCarrierFile));
		Assertions.assertTrue(Files.exists(carrierVehicleTypesFile));
		int initialJobs = countCarrierJobs(sharedCarrierFile);
		Path referenceCarrierFile = output.getParent().resolve("testMainRunAndResults").resolve("test.output_carriers_unsolvedVRP.xml.gz");
		if (Files.exists(referenceCarrierFile)) {
			Assertions.assertEquals(countCarrierJobs(referenceCarrierFile), initialJobs);
		}

		// Step 3: Solve each carrier part independently based on the shared unsolved carrier file.
		int jobsInParts = 0;
		for (int partIndex = 0; partIndex < 2; partIndex++) {
			List<String> partArgs = new ArrayList<>(commonArgs);
			partArgs.remove("--includeExistingModels");
			int creationOptionIndex = partArgs.indexOf("--creationOption") + 1;
			partArgs.set(creationOptionIndex, "useExistingCarrierFileWithoutSolution");
			partArgs.addAll(List.of(
				"--pathOutput", output.toString(),
				"--carrierFilePath", sharedCarrierFile.toAbsolutePath().toString(),
				"--smallScaleCommercialCarrierPartCount", "2",
				"--smallScaleCommercialCarrierPartIndex", String.valueOf(partIndex)
			));
			new GenerateSmallScaleCommercialTrafficDemand().execute(partArgs.toArray(new String[0]));

			Path partCarrierFile = output.resolve("carrierParts")
				.resolve("part-" + String.format("%03d", partIndex + 1) + "-of-002")
				.resolve("test.output_carriers_unsolvedVRP.xml.gz");
			Assertions.assertTrue(Files.exists(partCarrierFile));
			jobsInParts += countCarrierJobs(partCarrierFile);
		}

		// Step 4: Verify that splitting did not add or drop jobs across all part files.
		Assertions.assertEquals(initialJobs, jobsInParts);

		// Step 5: Merge the independently solved carrier parts and create the resulting population.
		List<String> mergeArgs = new ArrayList<>(commonArgs);
		mergeArgs.remove("--includeExistingModels");
		mergeArgs.addAll(List.of(
			"--pathOutput", output.toString(),
			"--mergeSmallScaleCommercialCarrierParts",
			"--smallScaleCommercialCarrierPartCount", "2"
		));
		new GenerateSmallScaleCommercialTrafficDemand().execute(mergeArgs.toArray(new String[0]));

		// Step 6: Verify that init's unsolved file remains the reference and the merged solved file contains all jobs.
		Path mergedSolvedCarrierFile = output.resolve("test.output_carriers_solvedVRP.xml.gz");
		Assertions.assertTrue(Files.exists(sharedCarrierFile));
		Assertions.assertTrue(Files.exists(mergedSolvedCarrierFile));
		Assertions.assertTrue(Files.exists(output.resolve("calculatedData")));
		Assertions.assertEquals(initialJobs, countCarrierJobs(sharedCarrierFile));
		Assertions.assertEquals(initialJobs, countCarrierJobs(mergedSolvedCarrierFile));
	}

	private static int countCarrierJobs(Path carrierFile) {
		try (var inputStream = IOUtils.getInputStream(IOUtils.resolveFileOrResource(carrierFile.toString()))) {
			var reader = XMLInputFactory.newFactory().createXMLStreamReader(inputStream);
			int jobs = 0;
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT
					&& (reader.getLocalName().equals("service") || reader.getLocalName().equals("shipment"))) {
					jobs++;
				}
			}
			reader.close();
			return jobs;
		} catch (IOException | XMLStreamException e) {
			throw new RuntimeException("Could not count carrier jobs in " + carrierFile, e);
		}
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
				.setSkipHeaderRecord(true).get().parse(reader);
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
