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
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;
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
		Map<String, Map<StructuralAttribute, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory()).getParent();
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).getParent().resolve("investigationAreaData.csv");
		LanduseDataConnectionCreator landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		Map<StructuralAttribute, List<String>> landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();
		// Test if the reading of the existing data distribution works correctly

		Map<String, Object2DoubleMap<StructuralAttribute>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
						usedLanduseConfiguration,
						SCTUtils.getIndexLanduse(inputDataDirectory), SCTUtils.getZoneIndex(inputDataDirectory), SCTUtils.getIndexBuildings(inputDataDirectory),
                        SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		Assertions.assertEquals(3, resultingDataPerZone.size(), MatsimTestUtils.EPSILON);

		Assertions.assertTrue(resultingDataPerZone.containsKey("area1"));
		Assertions.assertTrue(resultingDataPerZone.containsKey("area2"));
		Assertions.assertTrue(resultingDataPerZone.containsKey("area3"));

		for (String zone : resultingDataPerZone.keySet()) {
			Object2DoubleMap<StructuralAttribute> categories = resultingDataPerZone.get(zone);
			int employeeSum = 0;
			Assertions.assertEquals(8, categories.size(), MatsimTestUtils.EPSILON);
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.INHABITANTS));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_PRIMARY));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_CONSTRUCTION));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_SECONDARY));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_RETAIL));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_TRAFFIC));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_TERTIARY));

			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_PRIMARY);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_CONSTRUCTION);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_SECONDARY);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_RETAIL);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_TRAFFIC);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_TERTIARY);

			Assertions.assertEquals(categories.getDouble(StructuralAttribute.EMPLOYEE), employeeSum, MatsimTestUtils.EPSILON);

            switch (zone) {
                case "area1" -> {
                    Assertions.assertEquals(4000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.INHABITANTS),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(3500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(0, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_PRIMARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_CONSTRUCTION),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_SECONDARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_RETAIL),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TRAFFIC),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TERTIARY),
                            MatsimTestUtils.EPSILON);
                }
                case "area2" -> {
                    Assertions.assertEquals(4000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.INHABITANTS),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(6500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_PRIMARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_CONSTRUCTION),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_SECONDARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_RETAIL),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TRAFFIC),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(2000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TERTIARY),
                            MatsimTestUtils.EPSILON);
                }
                case "area3" -> {
                    Assertions.assertEquals(800, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.INHABITANTS),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(50, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_PRIMARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(200, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_CONSTRUCTION),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(100, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_SECONDARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(150, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_RETAIL),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(200, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TRAFFIC),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(300, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TERTIARY),
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
		Map<StructuralAttribute, List<SimpleFeature>> buildingsPerArea1 = buildingsPerZone.get("area1");
		Assertions.assertEquals(7, buildingsPerArea1.size(), MatsimTestUtils.EPSILON);
		List<SimpleFeature> inhabitantsBuildings = buildingsPerArea1.get(StructuralAttribute.INHABITANTS);
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
		Assertions.assertFalse(buildingsPerArea1.containsKey(StructuralAttribute.EMPLOYEE_PRIMARY));
		Assertions.assertEquals(1, buildingsPerArea1.get(StructuralAttribute.EMPLOYEE_CONSTRUCTION).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, buildingsPerArea1.get(StructuralAttribute.EMPLOYEE_SECONDARY).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, buildingsPerArea1.get(StructuralAttribute.EMPLOYEE_RETAIL).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, buildingsPerArea1.get(StructuralAttribute.EMPLOYEE_TRAFFIC).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, buildingsPerArea1.get(StructuralAttribute.EMPLOYEE_TERTIARY).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(6, buildingsPerArea1.get(StructuralAttribute.EMPLOYEE).size(), MatsimTestUtils.EPSILON);

		// test for area2
		Map<StructuralAttribute, List<SimpleFeature>> builingsPerArea2 = buildingsPerZone.get("area2");
		Assertions.assertEquals(8, builingsPerArea2.size(), MatsimTestUtils.EPSILON);
		List<SimpleFeature> employeeRetail = builingsPerArea2.get(StructuralAttribute.EMPLOYEE_RETAIL);
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
		Assertions.assertEquals(2, builingsPerArea2.get(StructuralAttribute.INHABITANTS).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea2.get(StructuralAttribute.EMPLOYEE_PRIMARY).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea2.get(StructuralAttribute.EMPLOYEE_CONSTRUCTION).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea2.get(StructuralAttribute.EMPLOYEE_SECONDARY).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, builingsPerArea2.get(StructuralAttribute.EMPLOYEE_TRAFFIC).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, builingsPerArea2.get(StructuralAttribute.EMPLOYEE_TERTIARY).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(8, builingsPerArea2.get(StructuralAttribute.EMPLOYEE).size(), MatsimTestUtils.EPSILON);

		// test for area3
		Map<StructuralAttribute, List<SimpleFeature>> builingsPerArea3 = buildingsPerZone.get("area3");
		Assertions.assertEquals(8, builingsPerArea3.size(), MatsimTestUtils.EPSILON);
		List<SimpleFeature> tertiaryRetail = builingsPerArea3.get(StructuralAttribute.EMPLOYEE_TERTIARY);
		Assertions.assertEquals(1, tertiaryRetail.size(), MatsimTestUtils.EPSILON);
		for (SimpleFeature singleBuilding : tertiaryRetail) {
			int id = (int) (long) singleBuilding.getAttribute("osm_id");
			if (id == 26) {
				Assertions.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assertions.assertEquals("foundation", String.valueOf(singleBuilding.getAttribute("type")));
			} else
				Assertions.fail();
		}
		Assertions.assertEquals(3, builingsPerArea3.get(StructuralAttribute.INHABITANTS).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea3.get(StructuralAttribute.EMPLOYEE_PRIMARY).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea3.get(StructuralAttribute.EMPLOYEE_CONSTRUCTION).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea3.get(StructuralAttribute.EMPLOYEE_SECONDARY).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, builingsPerArea3.get(StructuralAttribute.EMPLOYEE_TRAFFIC).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, builingsPerArea3.get(StructuralAttribute.EMPLOYEE_RETAIL).size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(7, builingsPerArea3.get(StructuralAttribute.EMPLOYEE).size(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testLanduseDistribution() throws IOException {
		Map<String, Map<StructuralAttribute, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory()).getParent();
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).getParent().resolve("investigationAreaData.csv");
		LanduseDataConnectionCreator landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		Map<StructuralAttribute, List<String>> landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();

		// Analyze resultingData per zone
		Map<String, Object2DoubleMap<StructuralAttribute>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
					usedLanduseConfiguration,
					SCTUtils.getIndexLanduse(inputDataDirectory), SCTUtils.getZoneIndex(inputDataDirectory), SCTUtils.getIndexBuildings(inputDataDirectory),
					SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		Assertions.assertEquals(3, resultingDataPerZone.size(), MatsimTestUtils.EPSILON);

		Assertions.assertTrue(resultingDataPerZone.containsKey("area1"));
		Assertions.assertTrue(resultingDataPerZone.containsKey("area2"));
		Assertions.assertTrue(resultingDataPerZone.containsKey("area3"));

		for (String zone : resultingDataPerZone.keySet()) {
			Object2DoubleMap<StructuralAttribute> categories = resultingDataPerZone.get(zone);
			int employeeSum = 0;
			Assertions.assertEquals(8, categories.size(), MatsimTestUtils.EPSILON);
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.INHABITANTS));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_PRIMARY));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_CONSTRUCTION));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_SECONDARY));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_RETAIL));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_TRAFFIC));
			Assertions.assertTrue(categories.containsKey(StructuralAttribute.EMPLOYEE_TERTIARY));

			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_PRIMARY);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_CONSTRUCTION);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_SECONDARY);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_RETAIL);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_TRAFFIC);
			employeeSum += (int) categories.getDouble(StructuralAttribute.EMPLOYEE_TERTIARY);

			Assertions.assertEquals(categories.getDouble(StructuralAttribute.EMPLOYEE), employeeSum, MatsimTestUtils.EPSILON);

            switch (zone) {
                case "area1" -> {
                    Assertions.assertEquals(4000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.INHABITANTS),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(3500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(0, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_PRIMARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_CONSTRUCTION),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_SECONDARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_RETAIL),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TRAFFIC),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TERTIARY),
                            MatsimTestUtils.EPSILON);
                }
                case "area2" -> {
                    Assertions.assertEquals(4000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.INHABITANTS),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(6500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_PRIMARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_CONSTRUCTION),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_SECONDARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_RETAIL),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1500, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TRAFFIC),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(2000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TERTIARY),
                            MatsimTestUtils.EPSILON);
                }
                case "area3" -> {
                    Assertions.assertEquals(800, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.INHABITANTS),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(1000, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(50, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_PRIMARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(200, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_CONSTRUCTION),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(100, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_SECONDARY),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(150, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_RETAIL),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(200, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TRAFFIC),
                            MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(300, resultingDataPerZone.get(zone).getDouble(StructuralAttribute.EMPLOYEE_TERTIARY),
                            MatsimTestUtils.EPSILON);
                }
				default -> Assertions.fail("Zone not found");
            }
		}
	}
}
