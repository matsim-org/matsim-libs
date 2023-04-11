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

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Ricardo Ewert
 *
 */
public class LanduseBuildingAnalysisTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReadOfDataDistributionPerZoneAndBuildingAnalysis() throws IOException {
		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("calculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");
		String shapeCRS = "EPSG:4326";

		// Test if the reading of the existing data distribution works correctly
		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory, usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, null, buildingsPerZone);

		Assert.assertEquals(3, resultingDataPerZone.size(), MatsimTestUtils.EPSILON);

		Assert.assertTrue(resultingDataPerZone.containsKey("testArea1_area1"));
		Assert.assertTrue(resultingDataPerZone.containsKey("testArea1_area2"));
		Assert.assertTrue(resultingDataPerZone.containsKey("testArea2_area3"));

		for (String zone : resultingDataPerZone.keySet()) {
			Object2DoubleMap<String> categories = resultingDataPerZone.get(zone);
			int employeeSum = 0;
			Assert.assertEquals(8, categories.size(), MatsimTestUtils.EPSILON);
			Assert.assertTrue(categories.containsKey("Inhabitants"));
			Assert.assertTrue(categories.containsKey("Employee"));
			Assert.assertTrue(categories.containsKey("Employee Primary Sector"));
			Assert.assertTrue(categories.containsKey("Employee Construction"));
			Assert.assertTrue(categories.containsKey("Employee Secondary Sector Rest"));
			Assert.assertTrue(categories.containsKey("Employee Retail"));
			Assert.assertTrue(categories.containsKey("Employee Traffic/Parcels"));
			Assert.assertTrue(categories.containsKey("Employee Tertiary Sector Rest"));

			employeeSum += categories.getDouble("Employee Primary Sector");
			employeeSum += categories.getDouble("Employee Construction");
			employeeSum += categories.getDouble("Employee Secondary Sector Rest");
			employeeSum += categories.getDouble("Employee Retail");
			employeeSum += categories.getDouble("Employee Traffic/Parcels");
			employeeSum += categories.getDouble("Employee Tertiary Sector Rest");

			Assert.assertEquals(categories.getDouble("Employee"), employeeSum, MatsimTestUtils.EPSILON);

			if (zone.equals("testArea1_area1")) {
				Assert.assertEquals(4000, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(3500, resultingDataPerZone.get(zone).getDouble("Employee"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(0, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
						MatsimTestUtils.EPSILON);
			}
			if (zone.equals("testArea1_area2")) {
				Assert.assertEquals(4000, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(6500, resultingDataPerZone.get(zone).getDouble("Employee"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1500, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1500, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(2000, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
						MatsimTestUtils.EPSILON);
			}
			if (zone.equals("testArea2_area3")) {
				Assert.assertEquals(800, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(50, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(200, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(100, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(150, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(200, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(300, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
						MatsimTestUtils.EPSILON);
			}
		}

		// tests if the reading of the buildings works correctly
		Index indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS);
		ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, null, StandardCharsets.UTF_8);
		List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
		Assert.assertEquals(31, buildingsFeatures.size(), MatsimTestUtils.EPSILON);
		LanduseBuildingAnalysis.analyzeBuildingType(buildingsFeatures, buildingsPerZone,
				landuseCategoriesAndDataConnection, shapeFileLandusePath, indexZones, shapeCRS);

		Assert.assertEquals(3, buildingsPerZone.size(), MatsimTestUtils.EPSILON);
		Assert.assertTrue(buildingsPerZone.containsKey("testArea1_area1"));
		Assert.assertTrue(buildingsPerZone.containsKey("testArea1_area2"));
		Assert.assertTrue(buildingsPerZone.containsKey("testArea2_area3"));

		// test for area1
		HashMap<String, ArrayList<SimpleFeature>> builingsPerArea1 = buildingsPerZone.get("testArea1_area1");
		Assert.assertEquals(7, builingsPerArea1.size(), MatsimTestUtils.EPSILON);
		ArrayList<SimpleFeature> inhabitantsBuildings = builingsPerArea1.get("Inhabitants");
		Assert.assertEquals(4, inhabitantsBuildings.size(), MatsimTestUtils.EPSILON);
		for (SimpleFeature singleBuilding : inhabitantsBuildings) {
			int id = (int) (long) singleBuilding.getAttribute("osm_id");
			if (id == 11) {
				Assert.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assert.assertEquals("apartments", String.valueOf(singleBuilding.getAttribute("type")));
			} else if (id == 12) {
				Assert.assertEquals("1", String.valueOf(singleBuilding.getAttribute("levels")));
				Assert.assertEquals("house", String.valueOf(singleBuilding.getAttribute("type")));
			} else if (id == 13) {
				Assert.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assert.assertEquals("residential", String.valueOf(singleBuilding.getAttribute("type")));
			} else if (id == 19) {
				Assert.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assert.assertEquals("detached", String.valueOf(singleBuilding.getAttribute("type")));
			} else
				Assert.fail();
		}
		Assert.assertFalse(builingsPerArea1.containsKey("Employee Primary Sector"));
		Assert.assertEquals(1, builingsPerArea1.get("Employee Construction").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea1.get("Employee Secondary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, builingsPerArea1.get("Employee Retail").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea1.get("Employee Traffic/Parcels").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, builingsPerArea1.get("Employee Tertiary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(6, builingsPerArea1.get("Employee").size(), MatsimTestUtils.EPSILON);

		// test for area2
		HashMap<String, ArrayList<SimpleFeature>> builingsPerArea2 = buildingsPerZone.get("testArea1_area2");
		Assert.assertEquals(8, builingsPerArea2.size(), MatsimTestUtils.EPSILON);
		ArrayList<SimpleFeature> employeeRetail = builingsPerArea2.get("Employee Retail");
		Assert.assertEquals(2, employeeRetail.size(), MatsimTestUtils.EPSILON);
		for (SimpleFeature singleBuilding : employeeRetail) {
			int id = (int) (long) singleBuilding.getAttribute("osm_id");
			if (id == 1) {
				Assert.assertEquals("1", String.valueOf(singleBuilding.getAttribute("levels")));
				Assert.assertEquals("retail", String.valueOf(singleBuilding.getAttribute("type")));
			} else if (id == 3) {
				Assert.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assert.assertEquals("retail", String.valueOf(singleBuilding.getAttribute("type")));
			} else
				Assert.fail();
		}
		Assert.assertEquals(2, builingsPerArea2.get("Inhabitants").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea2.get("Employee Primary Sector").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea2.get("Employee Construction").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea2.get("Employee Secondary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, builingsPerArea2.get("Employee Traffic/Parcels").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, builingsPerArea2.get("Employee Tertiary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(8, builingsPerArea2.get("Employee").size(), MatsimTestUtils.EPSILON);

		// test for area3
		HashMap<String, ArrayList<SimpleFeature>> builingsPerArea3 = buildingsPerZone.get("testArea2_area3");
		Assert.assertEquals(8, builingsPerArea3.size(), MatsimTestUtils.EPSILON);
		ArrayList<SimpleFeature> tertiaryRetail = builingsPerArea3.get("Employee Tertiary Sector Rest");
		Assert.assertEquals(1, tertiaryRetail.size(), MatsimTestUtils.EPSILON);
		for (SimpleFeature singleBuilding : tertiaryRetail) {
			int id = (int) (long) singleBuilding.getAttribute("osm_id");
			if (id == 26) {
				Assert.assertEquals("2", String.valueOf(singleBuilding.getAttribute("levels")));
				Assert.assertEquals("foundation", String.valueOf(singleBuilding.getAttribute("type")));
			} else
				Assert.fail();
		}
		Assert.assertEquals(3, builingsPerArea3.get("Inhabitants").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea3.get("Employee Primary Sector").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea3.get("Employee Construction").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea3.get("Employee Secondary Sector Rest").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, builingsPerArea3.get("Employee Traffic/Parcels").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, builingsPerArea3.get("Employee Retail").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(7, builingsPerArea3.get("Employee").size(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testLanduseDistribution() throws IOException {
		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("calculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");

		// Analyze resultingData per zone
		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory, usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, null, buildingsPerZone);

		Assert.assertEquals(3, resultingDataPerZone.size(), MatsimTestUtils.EPSILON);

		Assert.assertTrue(resultingDataPerZone.containsKey("testArea1_area1"));
		Assert.assertTrue(resultingDataPerZone.containsKey("testArea1_area2"));
		Assert.assertTrue(resultingDataPerZone.containsKey("testArea2_area3"));

		for (String zone : resultingDataPerZone.keySet()) {
			Object2DoubleMap<String> categories = resultingDataPerZone.get(zone);
			int employeeSum = 0;
			Assert.assertEquals(8, categories.size(), MatsimTestUtils.EPSILON);
			Assert.assertTrue(categories.containsKey("Inhabitants"));
			Assert.assertTrue(categories.containsKey("Employee"));
			Assert.assertTrue(categories.containsKey("Employee Primary Sector"));
			Assert.assertTrue(categories.containsKey("Employee Construction"));
			Assert.assertTrue(categories.containsKey("Employee Secondary Sector Rest"));
			Assert.assertTrue(categories.containsKey("Employee Retail"));
			Assert.assertTrue(categories.containsKey("Employee Traffic/Parcels"));
			Assert.assertTrue(categories.containsKey("Employee Tertiary Sector Rest"));

			employeeSum += categories.getDouble("Employee Primary Sector");
			employeeSum += categories.getDouble("Employee Construction");
			employeeSum += categories.getDouble("Employee Secondary Sector Rest");
			employeeSum += categories.getDouble("Employee Retail");
			employeeSum += categories.getDouble("Employee Traffic/Parcels");
			employeeSum += categories.getDouble("Employee Tertiary Sector Rest");

			Assert.assertEquals(categories.getDouble("Employee"), employeeSum, MatsimTestUtils.EPSILON);

			if (zone.equals("testArea1_area1")) {
				Assert.assertEquals(4000, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(3500, resultingDataPerZone.get(zone).getDouble("Employee"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(0, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
						MatsimTestUtils.EPSILON);
			}
			if (zone.equals("testArea1_area2")) {
				Assert.assertEquals(4000, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(6500, resultingDataPerZone.get(zone).getDouble("Employee"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1500, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(500, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1500, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(2000, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
						MatsimTestUtils.EPSILON);
			}
			if (zone.equals("testArea2_area3")) {
				Assert.assertEquals(800, resultingDataPerZone.get(zone).getDouble("Inhabitants"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(1000, resultingDataPerZone.get(zone).getDouble("Employee"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(50, resultingDataPerZone.get(zone).getDouble("Employee Primary Sector"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(200, resultingDataPerZone.get(zone).getDouble("Employee Construction"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(100, resultingDataPerZone.get(zone).getDouble("Employee Secondary Sector Rest"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(150, resultingDataPerZone.get(zone).getDouble("Employee Retail"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(200, resultingDataPerZone.get(zone).getDouble("Employee Traffic/Parcels"),
						MatsimTestUtils.EPSILON);
				Assert.assertEquals(300, resultingDataPerZone.get(zone).getDouble("Employee Tertiary Sector Rest"),
						MatsimTestUtils.EPSILON);
			}
		}
	}
}
