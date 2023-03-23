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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.matsim.smallScaleCommercialTrafficGeneration.TrafficVolumeGeneration.TrafficVolumeKey;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Ricardo Ewert
 *
 */
public class TripDistributionMatrixTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testTripDistributionBusinessTraffic() throws IOException {

		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("calculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");
		String networkLocation = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		Network network = NetworkUtils.readNetwork(networkLocation);
		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory, usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, null, buildingsPerZone);

		String usedTrafficType = "businessTraffic";
		double sample = 1.;
		double resistanceFactor = 0.005;

		ArrayList<String> modesORvehTypes = new ArrayList<String>(
				List.of("total"));
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
				.newInstance(shpZones, trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop, usedTrafficType).build();

		Map<String, HashMap<Id<Link>, Link>> regionLinksMap = new HashMap<>();
		regionLinksMap.put("testArea1_area1", new HashMap<>());
		regionLinksMap.get("testArea1_area1").put(Id.createLinkId("i(8,6)"), network.getLinks().get(Id.createLinkId("i(8,6)")));
		regionLinksMap.put("testArea1_area2", new HashMap<>());
		regionLinksMap.get("testArea1_area2").put(Id.createLinkId("i(2,7)R"), network.getLinks().get(Id.createLinkId("i(2,7)R")));
		regionLinksMap.put("testArea2_area3", new HashMap<>());
		regionLinksMap.get("testArea2_area3").put(Id.createLinkId("i(2,1)R"), network.getLinks().get(Id.createLinkId("i(2,7)R")));

		for (String startZone : resultingDataPerZone.keySet()) {
			for (String stopZone : resultingDataPerZone.keySet()) {
				for (String modeORvehType : modesORvehTypes) {
					for (Integer purpose : trafficVolumePerTypeAndZone_start
							.get(TrafficVolumeGeneration.makeTrafficVolumeKey(startZone, modeORvehType)).keySet()) {
						odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose, usedTrafficType,
								network, regionLinksMap, resistanceFactor);
					}
				}
			}
		}
		odMatrix.clearRoundingError();

		//tests
		Assert.assertEquals(3, odMatrix.getListOfZones().size(), MatsimTestUtils.EPSILON);
		for (String zone : resultingDataPerZone.keySet()) {
			Assert.assertTrue(odMatrix.getListOfZones().contains(zone));
		}
		Assert.assertEquals(1, odMatrix.getListOfModesOrVehTypes().size(), MatsimTestUtils.EPSILON);
		Assert.assertTrue(odMatrix.getListOfModesOrVehTypes().contains("total"));

		Assert.assertEquals(5, odMatrix.getListOfPurposes().size(), MatsimTestUtils.EPSILON);
		for (int i = 1; i <= 5; i++) {
			Assert.assertTrue(odMatrix.getListOfPurposes().contains(i));
		}
		double sumStartServices = 0;
		double sumStopServices = 0;

		for (String zone : resultingDataPerZone.keySet()) {
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey key = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, modeORvehType);
				for (Integer purpose : trafficVolumePerTypeAndZone_start
						.get(key).keySet()) {
					int generatedVolume = odMatrix.getSumOfServicesForStopZone(zone, modeORvehType, purpose,
							usedTrafficType);
					sumStopServices += generatedVolume;
					sumStartServices += odMatrix.getSumOfServicesForStartZone(zone, modeORvehType, purpose,
							usedTrafficType);
					double planedVolume = trafficVolumePerTypeAndZone_stop.get(key).getDouble(purpose);
					Assert.assertEquals(planedVolume, generatedVolume, MatsimTestUtils.EPSILON);
				}
			}
		}
		Assert.assertEquals(sumStartServices, sumStopServices, MatsimTestUtils.EPSILON);
	}

	@Test
	public void testTripDistributionFreightTraffic() throws IOException {

		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("calculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");
		String networkLocation = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		Network network = NetworkUtils.readNetwork(networkLocation);
		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory, usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, null, buildingsPerZone);

		String usedTrafficType = "freightTraffic";
		double sample = 1.;
		double resistanceFactor = 0.005;

		ArrayList<String> modesORvehTypes = new ArrayList<String>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
				.newInstance(shpZones, trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop, usedTrafficType).build();

		Map<String, HashMap<Id<Link>, Link>> regionLinksMap = new HashMap<>();
		regionLinksMap.put("testArea1_area1", new HashMap<>());
		regionLinksMap.get("testArea1_area1").put(Id.createLinkId("i(8,6)"), network.getLinks().get(Id.createLinkId("i(8,6)")));
		regionLinksMap.put("testArea1_area2", new HashMap<>());
		regionLinksMap.get("testArea1_area2").put(Id.createLinkId("i(2,7)R"), network.getLinks().get(Id.createLinkId("i(2,7)R")));
		regionLinksMap.put("testArea2_area3", new HashMap<>());
		regionLinksMap.get("testArea2_area3").put(Id.createLinkId("i(2,1)R"), network.getLinks().get(Id.createLinkId("i(2,7)R")));

		for (String startZone : resultingDataPerZone.keySet()) {
			for (String stopZone : resultingDataPerZone.keySet()) {
				for (String modeORvehType : modesORvehTypes) {
					for (Integer purpose : trafficVolumePerTypeAndZone_start
							.get(TrafficVolumeGeneration.makeTrafficVolumeKey(startZone, modeORvehType)).keySet()) {
						odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose, usedTrafficType,
								network, regionLinksMap, resistanceFactor);
					}
				}
			}
		}
		odMatrix.clearRoundingError();

		//tests
		Assert.assertEquals(3, odMatrix.getListOfZones().size(), MatsimTestUtils.EPSILON);
		for (String zone : resultingDataPerZone.keySet()) {
			Assert.assertTrue(odMatrix.getListOfZones().contains(zone));
		}
		Assert.assertEquals(5, odMatrix.getListOfModesOrVehTypes().size(), MatsimTestUtils.EPSILON);
		for (String modeORvehType : modesORvehTypes) {
			Assert.assertTrue(odMatrix.getListOfModesOrVehTypes().contains(modeORvehType));
		}

		Assert.assertEquals(6, odMatrix.getListOfPurposes().size(), MatsimTestUtils.EPSILON);
		for (int i = 1; i <= 6; i++) {
			Assert.assertTrue(odMatrix.getListOfPurposes().contains(i));
		}
		double sumStartServices = 0;
		double sumStopServices = 0;

		for (String zone : resultingDataPerZone.keySet()) {
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey key = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, modeORvehType);
				for (Integer purpose : trafficVolumePerTypeAndZone_start
						.get(key).keySet()) {
					int generatedVolume = odMatrix.getSumOfServicesForStopZone(zone, modeORvehType, purpose,
							usedTrafficType);
					sumStopServices += generatedVolume;
					sumStartServices += odMatrix.getSumOfServicesForStartZone(zone, modeORvehType, purpose,
							usedTrafficType);
					double planedVolume = trafficVolumePerTypeAndZone_stop.get(key).getDouble(purpose);
					Assert.assertEquals(planedVolume, generatedVolume, MatsimTestUtils.EPSILON);
				}
			}
		}
		Assert.assertEquals(sumStartServices, sumStopServices, MatsimTestUtils.EPSILON);
	}
}
