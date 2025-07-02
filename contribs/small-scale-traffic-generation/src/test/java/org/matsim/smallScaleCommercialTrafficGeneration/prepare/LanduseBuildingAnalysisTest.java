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
package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.smallScaleCommercialTrafficGeneration.SCTUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ricardo Ewert
 *
 */
public class LanduseBuildingAnalysisTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testReadOfDataDistributionPerZoneAndBuildingAnalysis() throws IOException {
		Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory()).getParent();
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).getParent().resolve("investigationAreaData.csv");
		LanduseDataConnectionCreator landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		Map<String, List<String>> landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();
		// Test if the reading of the existing data distribution works correctly

		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
						usedLanduseConfiguration,
						SCTUtils.getIndexLanduse(inputDataDirectory), SCTUtils.getZoneIndex(inputDataDirectory), SCTUtils.getIndexBuildings(inputDataDirectory),
                        SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		Assertions.assertEquals(3, resultingDataPerZone.size(), MatsimTestUtils.EPSILON);

		Assertions.assertTrue(resultingDataPerZone.containsKey("area1"));
		Assertions.assertTrue(resultingDataPerZone.containsKey("area2"));
		Assertions.assertTrue(resultingDataPerZone.containsKey("area3"));

		for (String zone : resultingDataPerZone.keySet()) {
			Object2DoubleMap<String> categories = resultingDataPerZone.get(zone);
			int employeeSum = 0;
			Assertions.assertEquals(8, categories.size(), MatsimTestUtils.EPSILON);
			Assertions.assertTrue(categories.containsKey("Inhabitants"));
			Assertions.assertTrue(categories.containsKey("Employee"));
			Assertions.assertTrue(categories.containsKey("Employee Primary Sector"));
			Assertions.assertTrue(categories.containsKey("Employee Construction"));
			Assertions.assertTrue(categories.containsKey("Employee Secondary Sector Rest"));
			Assertions.assertTrue(categories.containsKey("Employee Retail"));
			Assertions.assertTrue(categories.containsKey("Employee Traffic/Parcels"));
			Assertions.assertTrue(categories.containsKey("Employee Tertiary Sector Rest"));

			employeeSum += (int) categories.getDouble("Employee Primary Sector");
			employeeSum += (int) categories.getDouble("Employee Construction");
			employeeSum += (int) categories.getDouble("Employee Secondary Sector Rest");
			employeeSum += (int) categories.getDouble("Employee Retail");
			employeeSum += (int) categories.getDouble("Employee Traffic/Parcels");
			employeeSum += (int) categories.getDouble("Employee Tertiary Sector Rest");

			Assertions.assertEquals(categories.getDouble("Employee"), employeeSum, MatsimTestUtils.EPSILON);

            switch (zone) {
                case "area1" -> {
                    Assertions.assertEquals(4000, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(3500, resultingDataPerZone.get(zone).getDouble("Employee"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(0, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                }
                case "area2" -> {
                    Assertions.assertEquals(4000, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(6500, resultingDataPerZone.get(zone).getDouble("Employee"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1500, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1500, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(2000, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                }
                case "area3" -> {
                    Assertions.assertEquals(800, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(50, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(200, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(100, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(150, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(200, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(300, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                }
				default -> Assertions.fail("Zone not found");
            }

		}

		// tests if the reading of the buildings works correctly
		List<SimpleFeature> buildingsFeatures = SCTUtils.getIndexBuildings(inputDataDirectory).getAllFeatures();
		Assertions.assertEquals(31, buildingsFeatures.size(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(3, buildingsPerZone.size(), MatsimTestUtils.EPSILON);
		Assertions.assertTrue(buildingsPerZone.containsKey("area1"));
		Assertions.assertTrue(buildingsPerZone.containsKey("area2"));
		Assertions.assertTrue(buildingsPerZone.containsKey("area3"));

		// test for area1
		Map<String, List<SimpleFeature>> buildingsPerArea1 = buildingsPerZone.get("area1");
		Assertions.assertEquals(7, buildingsPerArea1.size(), MatsimTestUtils.EPSILON);
		List<SimpleFeature> inhabitantsBuildings = buildingsPerArea1.get("Inhabitants");
		Assertions.assertEquals(4, inhabitantsBuildings.size(), MatsimTestUtils.EPSILON);
		for (SimpleFeature singleBuilding : inhabitantsBuildings) {
			int id = (int) (long) singleBuilding.getAttribute("osm_id");
			if (id == 11) {
				Assertions.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assertions.assertEquals("apartments", String.valueOf(singleBuilding.getAttribute("type")));
			} else if (id == 12) {
				Assertions.assertEquals("1", String.valueOf(singleBuilding.getAttribute("levels")));
				Assertions.assertEquals("house", String.valueOf(singleBuilding.getAttribute("type")));
			} else if (id == 13) {
				Assertions.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assertions.assertEquals("residential", String.valueOf(singleBuilding.getAttribute("type")));
			} else if (id == 19) {
				Assertions.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assertions.assertEquals("detached", String.valueOf(singleBuilding.getAttribute("type")));
			} else
				Assertions.fail();
		}
		Assertions.assertFalse(buildingsPerArea1.containsKey("Employee Primary Sector"));
		Assertions.assertEquals(1, buildingsPerArea1.get("Employee Construction").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, buildingsPerArea1.get("Employee Secondary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, buildingsPerArea1.get("Employee Retail").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, buildingsPerArea1.get("Employee Traffic/Parcels").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, buildingsPerArea1.get("Employee Tertiary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(6, buildingsPerArea1.get("Employee").size(), MatsimTestUtils.EPSILON);

		// test for area2
		Map<String, List<SimpleFeature>> builingsPerArea2 = buildingsPerZone.get("area2");
		Assertions.assertEquals(8, builingsPerArea2.size(), MatsimTestUtils.EPSILON);
		List<SimpleFeature> employeeRetail = builingsPerArea2.get("Employee Retail");
		Assertions.assertEquals(2, employeeRetail.size(), MatsimTestUtils.EPSILON);
		for (SimpleFeature singleBuilding : employeeRetail) {
			int id = (int) (long) singleBuilding.getAttribute("osm_id");
			if (id == 1) {
				Assertions.assertEquals("1", String.valueOf(singleBuilding.getAttribute("levels")));
				Assertions.assertEquals("retail", String.valueOf(singleBuilding.getAttribute("type")));
			} else if (id == 3) {
				Assertions.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assertions.assertEquals("retail", String.valueOf(singleBuilding.getAttribute("type")));
			} else
				Assertions.fail();
		}
		Assertions.assertEquals(2, builingsPerArea2.get("Inhabitants").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea2.get("Employee Primary Sector").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea2.get("Employee Construction").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea2.get("Employee Secondary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, builingsPerArea2.get("Employee Traffic/Parcels").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, builingsPerArea2.get("Employee Tertiary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(8, builingsPerArea2.get("Employee").size(), MatsimTestUtils.EPSILON);

		// test for area3
		Map<String, List<SimpleFeature>> builingsPerArea3 = buildingsPerZone.get("area3");
		Assertions.assertEquals(8, builingsPerArea3.size(), MatsimTestUtils.EPSILON);
		List<SimpleFeature> tertiaryRetail = builingsPerArea3.get("Employee Tertiary Sector Rest");
		Assertions.assertEquals(1, tertiaryRetail.size(), MatsimTestUtils.EPSILON);
		for (SimpleFeature singleBuilding : tertiaryRetail) {
			int id = (int) (long) singleBuilding.getAttribute("osm_id");
			if (id == 26) {
				Assertions.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assertions.assertEquals("foundation", String.valueOf(singleBuilding.getAttribute("type")));
			} else
				Assertions.fail();
		}
		Assertions.assertEquals(3, builingsPerArea3.get("Inhabitants").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea3.get("Employee Primary Sector").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea3.get("Employee Construction").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea3.get("Employee Secondary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea3.get("Employee Traffic/Parcels").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, builingsPerArea3.get("Employee Retail").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(7, builingsPerArea3.get("Employee").size(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testLanduseDistribution() throws IOException {
		Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory()).getParent();
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).getParent().resolve("investigationAreaData.csv");
		LanduseDataConnectionCreator landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		Map<String, List<String>> landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();

		// Analyze resultingData per zone
		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
					usedLanduseConfiguration,
					SCTUtils.getIndexLanduse(inputDataDirectory), SCTUtils.getZoneIndex(inputDataDirectory), SCTUtils.getIndexBuildings(inputDataDirectory),
					SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		Assertions.assertEquals(3, resultingDataPerZone.size(), MatsimTestUtils.EPSILON);

		Assertions.assertTrue(resultingDataPerZone.containsKey("area1"));
		Assertions.assertTrue(resultingDataPerZone.containsKey("area2"));
		Assertions.assertTrue(resultingDataPerZone.containsKey("area3"));

		for (String zone : resultingDataPerZone.keySet()) {
			Object2DoubleMap<String> categories = resultingDataPerZone.get(zone);
			int employeeSum = 0;
			Assertions.assertEquals(8, categories.size(), MatsimTestUtils.EPSILON);
			Assertions.assertTrue(categories.containsKey("Inhabitants"));
			Assertions.assertTrue(categories.containsKey("Employee"));
			Assertions.assertTrue(categories.containsKey("Employee Primary Sector"));
			Assertions.assertTrue(categories.containsKey("Employee Construction"));
			Assertions.assertTrue(categories.containsKey("Employee Secondary Sector Rest"));
			Assertions.assertTrue(categories.containsKey("Employee Retail"));
			Assertions.assertTrue(categories.containsKey("Employee Traffic/Parcels"));
			Assertions.assertTrue(categories.containsKey("Employee Tertiary Sector Rest"));

			employeeSum += (int) categories.getDouble("Employee Primary Sector");
			employeeSum += (int) categories.getDouble("Employee Construction");
			employeeSum += (int) categories.getDouble("Employee Secondary Sector Rest");
			employeeSum += (int) categories.getDouble("Employee Retail");
			employeeSum += (int) categories.getDouble("Employee Traffic/Parcels");
			employeeSum += (int) categories.getDouble("Employee Tertiary Sector Rest");

			Assertions.assertEquals(categories.getDouble("Employee"), employeeSum, MatsimTestUtils.EPSILON);

            switch (zone) {
                case "area1" -> {
                    Assertions.assertEquals(4000, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(3500, resultingDataPerZone.get(zone).getDouble("Employee"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(0, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                }
                case "area2" -> {
                    Assertions.assertEquals(4000, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(6500, resultingDataPerZone.get(zone).getDouble("Employee"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1500, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1500, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(2000, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                }
                case "area3" -> {
                    Assertions.assertEquals(800, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(50, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(200, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(100, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(150, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(200, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(300, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
                            MatsimTestUtils.EPSILON);
                }
				default -> Assertions.fail("Zone not found");
            }
		}
	}
}
