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
import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.smallScaleCommercialTrafficGeneration.TrafficVolumeGeneration.TrafficVolumeKey;
import org.matsim.smallScaleCommercialTrafficGeneration.prepare.LanduseBuildingAnalysis;
import org.matsim.smallScaleCommercialTrafficGeneration.prepare.LanduseDataConnectionCreator;
import org.matsim.smallScaleCommercialTrafficGeneration.prepare.LanduseDataConnectionCreatorForOSM_Data;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Ricardo Ewert
 *
 */
public class TrafficVolumeGenerationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testTrafficVolumeGenerationCommercialPersonTraffic() throws IOException {

		Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).resolve("investigationAreaData.csv");
		LanduseDataConnectionCreator landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		Map<String, List<String>> landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();

		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
					usedLanduseConfiguration,
					SCTUtils.getIndexLanduse(inputDataDirectory), SCTUtils.getZoneIndex(inputDataDirectory), SCTUtils.getIndexBuildings(inputDataDirectory),
					SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);


		String usedTrafficType = "commercialPersonTraffic";
		double sample = 1.;
		ArrayList<String> modesORvehTypes = new ArrayList<>(
				List.of("total"));
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);

		Assertions.assertEquals(3, trafficVolumePerTypeAndZone_start.size());
		Assertions.assertEquals(3, trafficVolumePerTypeAndZone_stop.size());

		for (String zone : resultingDataPerZone.keySet()) {
			TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, modesORvehTypes.get(0));
			Assertions.assertTrue(trafficVolumePerTypeAndZone_start.containsKey(trafficVolumeKey));
			Assertions.assertTrue(trafficVolumePerTypeAndZone_stop.containsKey(trafficVolumeKey));
		}
		TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area1", modesORvehTypes.get(0));
		Assertions.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(124, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(277, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(175, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(250, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(105, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(426, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(121, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(65, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area2", modesORvehTypes.get(0));
		Assertions.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(211, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(514, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(441, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(630, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(202, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(859, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(246, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(102, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area3", modesORvehTypes.get(0));
		Assertions.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(34, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(79, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(62, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(88, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(31, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(128, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(37, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(17, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);


		//test with different sample
		sample = 0.25;
		trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);

		Assertions.assertEquals(3, trafficVolumePerTypeAndZone_start.size());
		Assertions.assertEquals(3, trafficVolumePerTypeAndZone_stop.size());

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area1", modesORvehTypes.get(0));
		Assertions.assertEquals(7, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(31, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(69, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(44, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(63, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(26, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(106, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(30, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(16, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area2", modesORvehTypes.get(0));
		Assertions.assertEquals(7, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(53, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(129, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(110, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(158, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(50, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(215, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(61, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(25, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area3", modesORvehTypes.get(0));
		Assertions.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(8, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(20, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(15, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(22, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(32, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(9, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
	}

	@Test
	void testTrafficVolumeGenerationGoodsTraffic() throws IOException {

		Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).resolve("investigationAreaData.csv");
		LanduseDataConnectionCreator landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		Map<String, List<String>> landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();

		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
					usedLanduseConfiguration,
					SCTUtils.getIndexLanduse(inputDataDirectory), SCTUtils.getZoneIndex(inputDataDirectory), SCTUtils.getIndexBuildings(inputDataDirectory),
					SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		String usedTrafficType = "goodsTraffic";
		double sample = 1.;
		ArrayList<String> modesORvehTypes = new ArrayList<>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);

		Assertions.assertEquals(15, trafficVolumePerTypeAndZone_start.size());
		Assertions.assertEquals(15, trafficVolumePerTypeAndZone_stop.size());

		for (String zone : resultingDataPerZone.keySet()) {
			for (String modesORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, modesORvehType);
				Assertions.assertTrue(trafficVolumePerTypeAndZone_start.containsKey(trafficVolumeKey));
				Assertions.assertTrue(trafficVolumePerTypeAndZone_stop.containsKey(trafficVolumeKey));
			}
		}

		// test for "area1"
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
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area1", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
				if (modeORvehType.equals("vehTyp1")) {
					Assertions.assertEquals(5, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(16, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(101, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(36, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(33, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(0, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assertions.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(17, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(73, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(54, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp2")) {
					Assertions.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(21, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(11, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(23, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(10, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assertions.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(13, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(7, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(11, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp3")) {
					Assertions.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(44, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(42, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(28, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(23, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assertions.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(28, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(73, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(15, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp4")) {
					Assertions.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(10, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(13, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(2, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assertions.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp5")) {
					Assertions.assertEquals(2, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(4, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(29, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(72, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(31, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(0, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

					Assertions.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(133, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
			}
			Assertions.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assertions.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}

		// test for "area2"
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
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area2", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
			}
			Assertions.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assertions.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}

		// test for "area3"
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
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area3", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
			}
			Assertions.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assertions.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}
	}

	@Test
	void testAddingExistingScenarios() throws Exception {

		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		double sample = 1.;
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		config.setContext(inputDataDirectory.resolve("config.xml").toUri().toURL());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone = new HashMap<>();
		String shapeFileZoneNameColumn = "name";

		Map<String, Map<Id<Link>, Link>> linksPerZone = GenerateSmallScaleCommercialTrafficDemand.filterLinksForZones(scenario,
			SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, config.global().getCoordinateSystem(), shapeFileZoneNameColumn),
			facilitiesPerZone, shapeFileZoneNameColumn);

		IntegrateExistingTrafficToSmallScaleCommercial integratedExistingModels = new DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl();
		integratedExistingModels.readExistingCarriersFromFolder(scenario, sample, linksPerZone);

		Assertions.assertEquals(3, CarriersUtils.getCarriers(scenario).getCarriers().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertTrue(CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleServiceCarrier_carrier1", Carrier.class)));
		Assertions.assertTrue(CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleServiceCarrier_carrier2", Carrier.class)));
		Assertions.assertTrue(CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleShipmentCarrier_carrier1", Carrier.class)));

		Carrier addedCarrier1 = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleServiceCarrier_carrier1", Carrier.class));
		Assertions.assertNotNull(addedCarrier1.getSelectedPlan());
		Assertions.assertEquals(0, CarriersUtils.getJspritIterations(addedCarrier1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier1.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier1.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(3, addedCarrier1.getSelectedPlan().getScheduledTours().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(30, addedCarrier1.getServices().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(6, addedCarrier1.getAttributes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals("commercialPersonTraffic", addedCarrier1.getAttributes().getAttribute("subpopulation"));
		Assertions.assertEquals(2, (int) addedCarrier1.getAttributes().getAttribute("purpose"));
		Assertions.assertEquals("exampleServiceCarrier", addedCarrier1.getAttributes().getAttribute("existingModel"));
		Assertions.assertEquals("car", addedCarrier1.getAttributes().getAttribute("networkMode"));
		Assertions.assertNull(addedCarrier1.getAttributes().getAttribute("vehicleType"));
		Assertions.assertEquals("area3", addedCarrier1.getAttributes().getAttribute("tourStartArea"));

		Carrier addedCarrier2 = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleServiceCarrier_carrier2", Carrier.class));
		Assertions.assertNotNull(addedCarrier2.getSelectedPlan());
		Assertions.assertEquals(0, CarriersUtils.getJspritIterations(addedCarrier2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier2.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier2.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, addedCarrier2.getSelectedPlan().getScheduledTours().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(20, addedCarrier2.getServices().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals("commercialPersonTraffic", addedCarrier2.getAttributes().getAttribute("subpopulation"));
		Assertions.assertEquals(2, (int) addedCarrier2.getAttributes().getAttribute("purpose"));
		Assertions.assertEquals("exampleServiceCarrier", addedCarrier2.getAttributes().getAttribute("existingModel"));
		Assertions.assertEquals("car", addedCarrier2.getAttributes().getAttribute("networkMode"));
		Assertions.assertNull(addedCarrier2.getAttributes().getAttribute("vehicleType"));
		Assertions.assertEquals("area3", addedCarrier2.getAttributes().getAttribute("tourStartArea"));

		Carrier addedCarrier3 = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleShipmentCarrier_carrier1", Carrier.class));
		Assertions.assertNull(addedCarrier3.getSelectedPlan());
		Assertions.assertEquals(50, CarriersUtils.getJspritIterations(addedCarrier3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier3.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier3.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(FleetSize.INFINITE, addedCarrier3.getCarrierCapabilities().getFleetSize());
		Assertions.assertEquals(0, addedCarrier3.getServices().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(5, addedCarrier3.getShipments().size(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testAddingExistingScenariosWithSample() throws Exception {

		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		double sample = 0.2;
		String shapeFileZoneNameColumn = "name";

		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		config.setContext(inputDataDirectory.resolve("config.xml").toUri().toURL());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone = new HashMap<>();
		Map<String, Map<Id<Link>, Link>> linksPerZone = GenerateSmallScaleCommercialTrafficDemand
				.filterLinksForZones(scenario, SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, config.global().getCoordinateSystem(),
						shapeFileZoneNameColumn),
					facilitiesPerZone, shapeFileZoneNameColumn);

		IntegrateExistingTrafficToSmallScaleCommercial integratedExistingModels = new DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl();
		integratedExistingModels.readExistingCarriersFromFolder(scenario, sample, linksPerZone);

		Assertions.assertEquals(2, CarriersUtils.getCarriers(scenario).getCarriers().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertTrue(CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleServiceCarrier_carrier1", Carrier.class)));
		Assertions.assertTrue(CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("exampleShipmentCarrier_carrier1", Carrier.class)));

		Carrier addedCarrier1 = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleServiceCarrier_carrier1", Carrier.class));
		Assertions.assertNotNull(addedCarrier1.getSelectedPlan());
		Assertions.assertEquals(0, CarriersUtils.getJspritIterations(addedCarrier1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier1.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier1.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier1.getSelectedPlan().getScheduledTours().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(10, addedCarrier1.getServices().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(6, addedCarrier1.getAttributes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals("commercialPersonTraffic", addedCarrier1.getAttributes().getAttribute("subpopulation"));
		Assertions.assertEquals(2, (int) addedCarrier1.getAttributes().getAttribute("purpose"));
		Assertions.assertEquals("exampleServiceCarrier", addedCarrier1.getAttributes().getAttribute("existingModel"));
		Assertions.assertEquals("car", addedCarrier1.getAttributes().getAttribute("networkMode"));
		Assertions.assertNull(addedCarrier1.getAttributes().getAttribute("vehicleType"));
		Assertions.assertEquals("area3", addedCarrier1.getAttributes().getAttribute("tourStartArea"));

		Carrier addedCarrier3 = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("exampleShipmentCarrier_carrier1", Carrier.class));
		Assertions.assertNull(addedCarrier3.getSelectedPlan());
		Assertions.assertEquals(50, CarriersUtils.getJspritIterations(addedCarrier3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier3.getCarrierCapabilities().getCarrierVehicles().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier3.getCarrierCapabilities().getVehicleTypes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(FleetSize.INFINITE, addedCarrier3.getCarrierCapabilities().getFleetSize());
		Assertions.assertEquals(0, addedCarrier3.getServices().size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, addedCarrier3.getShipments().size(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testReducingDemandAfterAddingExistingScenarios_goods() throws Exception {
		Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		String usedTrafficType = "goodsTraffic";
		double sample = 1.;
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).resolve("investigationAreaData.csv");

		LanduseDataConnectionCreator landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		Map<String, List<String>> landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();

		IntegrateExistingTrafficToSmallScaleCommercial integratedExistingModels = new DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl();

		ArrayList<String> modesORvehTypes = new ArrayList<>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		config.setContext(inputDataDirectory.resolve("config.xml").toUri().toURL());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);
		Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone = new HashMap<>();

		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
					usedLanduseConfiguration,
					SCTUtils.getIndexLanduse(inputDataDirectory), SCTUtils.getZoneIndex(inputDataDirectory), SCTUtils.getIndexBuildings(inputDataDirectory),
					SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);

		Map<String, Map<Id<Link>, Link>> linksPerZone = GenerateSmallScaleCommercialTrafficDemand
				.filterLinksForZones(scenario, SCTUtils.getZoneIndex(inputDataDirectory), facilitiesPerZone, shapeFileZoneNameColumn);

		integratedExistingModels.readExistingCarriersFromFolder(scenario, sample, linksPerZone);

		integratedExistingModels.reduceDemandBasedOnExistingCarriers(scenario, linksPerZone, usedTrafficType,
				trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop);

		// test for "area1"
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
						TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area1", modeORvehType);
						sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
						sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
						if (modeORvehType.equals("vehTyp3")) {
							Assertions.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(44, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(42, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(28, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(23, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

							Assertions.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(26, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(73, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(15, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
						}
					}
					Assertions.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
					Assertions.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
				}

				// test for "area2"
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
						TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area2", modeORvehType);
						sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
						sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
					}
					Assertions.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
					Assertions.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
				}

				// test for "area3"
				estimatesStart = new HashMap<>();
				estimatesStart.put(1, 2.);
				estimatesStart.put(2, 7.);
				estimatesStart.put(3, 37.);
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
						TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area3", modeORvehType);
						sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
						sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
						if (modeORvehType.equals("vehTyp3")) {
							Assertions.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(4, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(17, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(11, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(5, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);

							Assertions.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(14, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
							Assertions.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
						}
					}
					Assertions.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
					Assertions.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
				}
	}

	@Test
	void testReducingDemandAfterAddingExistingScenarios_commercialPersonTraffic() throws Exception {
		Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();
		Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		String usedTrafficType = "commercialPersonTraffic";
		double sample = 1.;
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).resolve("investigationAreaData.csv");

		ArrayList<String> modesORvehTypes = new ArrayList<>(
				List.of("total"));
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		config.setContext(inputDataDirectory.resolve("config.xml").toUri().toURL());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		LanduseDataConnectionCreator landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		Map<String, List<String>> landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();

		IntegrateExistingTrafficToSmallScaleCommercial integratedExistingModels = new DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl();


		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
					usedLanduseConfiguration,
					SCTUtils.getIndexLanduse(inputDataDirectory), SCTUtils.getZoneIndex(inputDataDirectory), SCTUtils.getIndexBuildings(inputDataDirectory),
                        SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);

		Map<String, Map<Id<Link>, Link>> regionLinksMap = GenerateSmallScaleCommercialTrafficDemand
				.filterLinksForZones(scenario, SCTUtils.getZoneIndex(inputDataDirectory), facilitiesPerZone, shapeFileZoneNameColumn);

		integratedExistingModels.readExistingCarriersFromFolder(scenario, sample, regionLinksMap);

		integratedExistingModels.reduceDemandBasedOnExistingCarriers(scenario, regionLinksMap, usedTrafficType,
				trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop);

		//because the reduction of the start volume in zone3 (purpose 2) is higher than the value, a start reduction will be distributed over other zones
		double sumOfStartOtherAreas = 0;

		TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area1", modesORvehTypes.get(0));
		Assertions.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		sumOfStartOtherAreas += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2);
		Assertions.assertEquals(277, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(175, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(250, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(85, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(426, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(121, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(65, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area2", modesORvehTypes.get(0));
		Assertions.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		sumOfStartOtherAreas += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2);
		Assertions.assertEquals(514, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(441, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(630, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(187, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(859, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(246, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(102, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("area3", modesORvehTypes.get(0));
		Assertions.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(0, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		sumOfStartOtherAreas += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2);
		Assertions.assertEquals(79, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(62, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(88, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(27, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(128, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(37, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(17, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);

		Assertions.assertEquals(319, sumOfStartOtherAreas, MatsimTestUtils.EPSILON);
	}


	@Test
	void testTrafficVolumeKeyGeneration() {
		String zone = "zone1";
		String mode = "modeA";

		TrafficVolumeKey newKey = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, mode);

		Assertions.assertEquals(newKey.getZone(), zone);
		Assertions.assertEquals(newKey.getModeORvehType(), mode);
	}
}
