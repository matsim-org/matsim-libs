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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.smallScaleCommercialTrafficGeneration.TrafficVolumeGeneration.TrafficVolumeKey;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Ricardo Ewert
 *
 */
public class TrafficVolumeGenerationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testTrafficVolumeGenerationBusinessTraffic() throws IOException {

		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("calculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");


		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory, usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, null, buildingsPerZone);


		String usedTrafficType = "businessTraffic";
		double sample = 1.;
		ArrayList<String> modesORvehTypes = new ArrayList<>(
				List.of("total"));
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);

		Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.size());
		Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.size());

		for (String zone : resultingDataPerZone.keySet()) {
			TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, modesORvehTypes.get(0));
			Assert.assertTrue(trafficVolumePerTypeAndZone_start.containsKey(trafficVolumeKey));
			Assert.assertTrue(trafficVolumePerTypeAndZone_stop.containsKey(trafficVolumeKey));
		}
		TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area1", modesORvehTypes.get(0));
		Assert.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(124, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(277, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(175, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(250, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(105, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(426, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(121, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(65, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area2", modesORvehTypes.get(0));
		Assert.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(211, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(514, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(441, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(630, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(202, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(859, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(246, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(102, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea2_area3", modesORvehTypes.get(0));
		Assert.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(34, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(79, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(62, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(88, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(31, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(128, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(37, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(17, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);


		//test with different sample
		sample = 0.25;
		trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);

		Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.size());
		Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.size());

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area1", modesORvehTypes.get(0));
		Assert.assertEquals(7, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(31, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(69, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(44, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(63, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(26, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(106, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(30, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(16, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area2", modesORvehTypes.get(0));
		Assert.assertEquals(7, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(53, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(129, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(110, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(158, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(50, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(215, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(61, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(25, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea2_area3", modesORvehTypes.get(0));
		Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(8, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(20, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(15, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(22, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(32, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(9, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testTrafficVolumeGenerationFreightTraffic() throws IOException {

		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("calculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");


		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory, usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, null, buildingsPerZone);


		String usedTrafficType = "freightTraffic";
		double sample = 1.;
		ArrayList<String> modesORvehTypes = new ArrayList<>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);

		Assert.assertEquals(15, trafficVolumePerTypeAndZone_start.size());
		Assert.assertEquals(15, trafficVolumePerTypeAndZone_stop.size());

		for (String zone : resultingDataPerZone.keySet()) {
			for (String modesORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, modesORvehType);
				Assert.assertTrue(trafficVolumePerTypeAndZone_start.containsKey(trafficVolumeKey));
				Assert.assertTrue(trafficVolumePerTypeAndZone_stop.containsKey(trafficVolumeKey));
			}
		}

		// test for "testArea1_area1"
		HashMap<Integer, Double> estimatesStart = new HashMap<>();
		estimatesStart.put(1, 12.);
		estimatesStart.put(2, 30.);
		estimatesStart.put(3, 205.);
		estimatesStart.put(4, 174.);
		estimatesStart.put(5, 117.);
		estimatesStart.put(6, 36.);

		HashMap<Integer, Double> estimatesStop = new HashMap<>();
		estimatesStop.put(1, 15.);
		estimatesStop.put(2, 36.);
		estimatesStop.put(3, 139.);
		estimatesStop.put(4, 300.);
		estimatesStop.put(5, 32.);
		estimatesStop.put(6, 31.);
		for (int i = 1; i < 7; i++) {
			double sumStart = 0;
			double sumStop = 0;
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area1", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
				if (modeORvehType.equals("vehTyp1")) {
					Assert.assertEquals(5, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(16, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(101, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(36, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(33, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assert.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(17, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(73, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(54, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp2")) {
					Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(21, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(11, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(23, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(10, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assert.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(13, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(7, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(11, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp3")) {
					Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(44, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(42, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(28, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(23, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assert.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(28, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(73, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(15, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp4")) {
					Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(10, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(13, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(2, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assert.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp5")) {
					Assert.assertEquals(2, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(4, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(29, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(72, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(31, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assert.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assert.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assert.assertEquals(133, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assert.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
			}
			Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}

		// test for "testArea1_area2"
		estimatesStart = new HashMap<>();
		estimatesStart.put(1, 12.);
		estimatesStart.put(2, 37.);
		estimatesStart.put(3, 201.);
		estimatesStart.put(4, 512.);
		estimatesStart.put(5, 343.);
		estimatesStart.put(6, 36.);

		estimatesStop = new HashMap<>();
		estimatesStop.put(1, 15.);
		estimatesStop.put(2, 40.);
		estimatesStop.put(3, 165.);
		estimatesStop.put(4, 273.);
		estimatesStop.put(5, 42.);
		estimatesStop.put(6, 41.);
		for (int i = 1; i < 7; i++) {
			double sumStart = 0;
			double sumStop = 0;
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area2", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
			}
			Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}

		// test for "testArea2_area3"
		estimatesStart = new HashMap<>();
		estimatesStart.put(1, 2.);
		estimatesStart.put(2, 7.);
		estimatesStart.put(3, 42.);
		estimatesStart.put(4, 69.);
		estimatesStart.put(5, 46.);
		estimatesStart.put(6, 8.);

		estimatesStop = new HashMap<>();
		estimatesStop.put(1, 3.);
		estimatesStop.put(2, 8.);
		estimatesStop.put(3, 30.);
		estimatesStop.put(4, 57.);
		estimatesStop.put(5, 6.);
		estimatesStop.put(6, 6.);
		for (int i = 1; i < 7; i++) {
			double sumStart = 0;
			double sumStop = 0;
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea2_area3", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
			}
			Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testAddingExistingScenarios() throws Exception {

		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		double sample = 1.;
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();
		Map<String, HashMap<Id<Link>, Link>> regionLinksMap = CreateSmallScaleCommercialTrafficDemand
				.filterLinksForZones(scenario, shpZones, SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, config.global().getCoordinateSystem()),
                        buildingsPerZone);

		SmallScaleCommercialTrafficUtils.readExistingModels(scenario, sample, inputDataDirectory, regionLinksMap);

		Assert.assertEquals(3, FreightUtils.getCarriers(scenario).getCarriers().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, FreightUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleServiceCarrier_carrier1", Carrier.class)));
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleServiceCarrier_carrier2", Carrier.class)));
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleShipmentCarrier_carrier1", Carrier.class)));

		Carrier addedCarrier1 = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleServiceCarrier_carrier1", Carrier.class));
		Assert.assertNotNull(addedCarrier1.getSelectedPlan());
		Assert.assertEquals(0, CarrierUtils.getJspritIterations(addedCarrier1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier1.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier1.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(3, addedCarrier1.getSelectedPlan().getScheduledTours().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(30, addedCarrier1.getServices().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(6, addedCarrier1.getAttributes().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("businessTraffic", addedCarrier1.getAttributes().getAttribute("subpopulation"));
		Assert.assertEquals(2, (int) addedCarrier1.getAttributes().getAttribute("purpose"));
		Assert.assertEquals("exampleServiceCarrier", addedCarrier1.getAttributes().getAttribute("existingModel"));
		Assert.assertEquals("car", addedCarrier1.getAttributes().getAttribute("networkMode"));
		Assert.assertNull(addedCarrier1.getAttributes().getAttribute("vehicleType"));
		Assert.assertEquals("testArea2_area3", addedCarrier1.getAttributes().getAttribute("tourStartArea"));

		Carrier addedCarrier2 = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleServiceCarrier_carrier2", Carrier.class));
		Assert.assertNotNull(addedCarrier2.getSelectedPlan());
		Assert.assertEquals(0, CarrierUtils.getJspritIterations(addedCarrier2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier2.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier2.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, addedCarrier2.getSelectedPlan().getScheduledTours().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(20, addedCarrier2.getServices().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("businessTraffic", addedCarrier2.getAttributes().getAttribute("subpopulation"));
		Assert.assertEquals(2, (int) addedCarrier2.getAttributes().getAttribute("purpose"));
		Assert.assertEquals("exampleServiceCarrier", addedCarrier2.getAttributes().getAttribute("existingModel"));
		Assert.assertEquals("car", addedCarrier2.getAttributes().getAttribute("networkMode"));
		Assert.assertNull(addedCarrier2.getAttributes().getAttribute("vehicleType"));
		Assert.assertEquals("testArea2_area3", addedCarrier2.getAttributes().getAttribute("tourStartArea"));

		Carrier addedCarrier3 = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleShipmentCarrier_carrier1", Carrier.class));
		Assert.assertNull(addedCarrier3.getSelectedPlan());
		Assert.assertEquals(50, CarrierUtils.getJspritIterations(addedCarrier3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier3.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier3.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(FleetSize.INFINITE, addedCarrier3.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(0, addedCarrier3.getServices().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(5, addedCarrier3.getShipments().size(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testAddingExistingScenariosWithSample() throws Exception {

		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		double sample = 0.2;
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();
		Map<String, HashMap<Id<Link>, Link>> regionLinksMap = CreateSmallScaleCommercialTrafficDemand
				.filterLinksForZones(scenario, shpZones, SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, config.global().getCoordinateSystem()),
                        buildingsPerZone);

		SmallScaleCommercialTrafficUtils.readExistingModels(scenario, sample, inputDataDirectory, regionLinksMap);

		Assert.assertEquals(2, FreightUtils.getCarriers(scenario).getCarriers().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, FreightUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleServiceCarrier_carrier1", Carrier.class)));
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleShipmentCarrier_carrier1", Carrier.class)));

		Carrier addedCarrier1 = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleServiceCarrier_carrier1", Carrier.class));
		Assert.assertNotNull(addedCarrier1.getSelectedPlan());
		Assert.assertEquals(0, CarrierUtils.getJspritIterations(addedCarrier1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier1.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier1.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier1.getSelectedPlan().getScheduledTours().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(10, addedCarrier1.getServices().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(6, addedCarrier1.getAttributes().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("businessTraffic", addedCarrier1.getAttributes().getAttribute("subpopulation"));
		Assert.assertEquals(2, (int) addedCarrier1.getAttributes().getAttribute("purpose"));
		Assert.assertEquals("exampleServiceCarrier", addedCarrier1.getAttributes().getAttribute("existingModel"));
		Assert.assertEquals("car", addedCarrier1.getAttributes().getAttribute("networkMode"));
		Assert.assertNull(addedCarrier1.getAttributes().getAttribute("vehicleType"));
		Assert.assertEquals("testArea2_area3", addedCarrier1.getAttributes().getAttribute("tourStartArea"));

		Carrier addedCarrier3 = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleShipmentCarrier_carrier1", Carrier.class));
		Assert.assertNull(addedCarrier3.getSelectedPlan());
		Assert.assertEquals(50, CarrierUtils.getJspritIterations(addedCarrier3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier3.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier3.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(FleetSize.INFINITE, addedCarrier3.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(0, addedCarrier3.getServices().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, addedCarrier3.getShipments().size(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testReducingDemandAfterAddingExistingScenarios_freight() throws Exception {
		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("calculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		String usedTrafficType = "freightTraffic";
		double sample = 1.;
		ArrayList<String> modesORvehTypes = new ArrayList<>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory, usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, null, buildingsPerZone);

		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);

		Map<String, HashMap<Id<Link>, Link>> regionLinksMap = CreateSmallScaleCommercialTrafficDemand
				.filterLinksForZones(scenario, shpZones, SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, config.global().getCoordinateSystem()),
                        buildingsPerZone);

		SmallScaleCommercialTrafficUtils.readExistingModels(scenario, sample, inputDataDirectory, regionLinksMap);

		TrafficVolumeGeneration.reduceDemandBasedOnExistingCarriers(scenario, regionLinksMap, usedTrafficType,
				trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop);

		// test for "testArea1_area1"
				HashMap<Integer, Double> estimatesStart = new HashMap<>();
				estimatesStart.put(1, 12.);
				estimatesStart.put(2, 30.);
				estimatesStart.put(3, 205.);
				estimatesStart.put(4, 174.);
				estimatesStart.put(5, 117.);
				estimatesStart.put(6, 36.);

				HashMap<Integer, Double> estimatesStop = new HashMap<>();
				estimatesStop.put(1, 15.);
				estimatesStop.put(2, 36.);
				estimatesStop.put(3, 137.);
				estimatesStop.put(4, 300.);
				estimatesStop.put(5, 32.);
				estimatesStop.put(6, 31.);
				for (int i = 1; i < 7; i++) {
					double sumStart = 0;
					double sumStop = 0;
					for (String modeORvehType : modesORvehTypes) {
						TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area1", modeORvehType);
						sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
						sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
						if (modeORvehType.equals("vehTyp3")) {
							Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
							Assert.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
							Assert.assertEquals(44, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
							Assert.assertEquals(42, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
							Assert.assertEquals(28, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
							Assert.assertEquals(23, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

							Assert.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
							Assert.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
							Assert.assertEquals(26, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
							Assert.assertEquals(73, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
							Assert.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
							Assert.assertEquals(15, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
						}
					}
					Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
					Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
				}

				// test for "testArea1_area2"
				estimatesStart = new HashMap<>();
				estimatesStart.put(1, 12.);
				estimatesStart.put(2, 37.);
				estimatesStart.put(3, 201.);
				estimatesStart.put(4, 512.);
				estimatesStart.put(5, 343.);
				estimatesStart.put(6, 36.);

				estimatesStop = new HashMap<>();
				estimatesStop.put(1, 15.);
				estimatesStop.put(2, 40.);
				estimatesStop.put(3, 165.);
				estimatesStop.put(4, 273.);
				estimatesStop.put(5, 42.);
				estimatesStop.put(6, 41.);
				for (int i = 1; i < 7; i++) {
					double sumStart = 0;
					double sumStop = 0;
					for (String modeORvehType : modesORvehTypes) {
						TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area2", modeORvehType);
						sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
						sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
					}
					Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
					Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
				}

				// test for "testArea2_area3"
				estimatesStart = new HashMap<>();
				estimatesStart.put(1, 2.);
				estimatesStart.put(2, 7.);
				estimatesStart.put(3, 40.);
				estimatesStart.put(4, 69.);
				estimatesStart.put(5, 46.);
				estimatesStart.put(6, 8.);

				estimatesStop = new HashMap<>();
				estimatesStop.put(1, 3.);
				estimatesStop.put(2, 8.);
				estimatesStop.put(3, 30.);
				estimatesStop.put(4, 57.);
				estimatesStop.put(5, 6.);
				estimatesStop.put(6, 6.);
				for (int i = 1; i < 7; i++) {
					double sumStart = 0;
					double sumStop = 0;
					for (String modeORvehType : modesORvehTypes) {
						TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea2_area3", modeORvehType);
						sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
						sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
						if (modeORvehType.equals("vehTyp3")) {
							Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
							Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
							Assert.assertEquals(7, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
							Assert.assertEquals(17, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
							Assert.assertEquals(11, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
							Assert.assertEquals(5, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

							Assert.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
							Assert.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
							Assert.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
							Assert.assertEquals(14, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
							Assert.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
							Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
						}
					}
					Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
					Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
				}
	}

	@Test
	public void testReducingDemandAfterAddingExistingScenarios_business() throws Exception {
		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("calculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		String usedTrafficType = "businessTraffic";
		double sample = 1.;
		ArrayList<String> modesORvehTypes = new ArrayList<>(
				List.of("total"));
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory, usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, null, buildingsPerZone);

		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);

		Map<String, HashMap<Id<Link>, Link>> regionLinksMap = CreateSmallScaleCommercialTrafficDemand
				.filterLinksForZones(scenario, shpZones, SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, config.global().getCoordinateSystem()),
                        buildingsPerZone);

		SmallScaleCommercialTrafficUtils.readExistingModels(scenario, sample, inputDataDirectory, regionLinksMap);

		TrafficVolumeGeneration.reduceDemandBasedOnExistingCarriers(scenario, regionLinksMap, usedTrafficType,
				trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop);

		//because the reduction of the start volume in zone3 (purpose 2) is higher than the value, a start reduction will be distributed over other zones
		double sumOfStartOtherAreas = 0;

		TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area1", modesORvehTypes.get(0));
		Assert.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		sumOfStartOtherAreas += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2);
		Assert.assertEquals(277, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(175, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(250, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(85, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(426, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(121, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(65, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area2", modesORvehTypes.get(0));
		Assert.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		sumOfStartOtherAreas += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2);
		Assert.assertEquals(514, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(441, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(630, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(187, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(859, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(246, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(102, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea2_area3", modesORvehTypes.get(0));
		Assert.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		sumOfStartOtherAreas += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2);
		Assert.assertEquals(79, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(62, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(88, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(27, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(128, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(37, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(17, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assert.assertEquals(330, sumOfStartOtherAreas, MatsimTestUtils.EPSILON);
	}



	@Test
	public void testTrafficVolumeKeyGeneration() {
		String zone = "zone1";
		String mode = "modeA";

		TrafficVolumeKey newKey = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, mode);

		Assert.assertEquals(newKey.getZone(), zone);
		Assert.assertEquals(newKey.getModeORvehType(), mode);
	}
}
