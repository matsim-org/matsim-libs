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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.smallScaleCommercialTrafficGeneration.TrafficVolumeGeneration.TrafficVolumeKey;
import org.matsim.smallScaleCommercialTrafficGeneration.prepare.LanduseBuildingAnalysis;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.matsim.smallScaleCommercialTrafficGeneration.SCTUtils.*;

/**
 * @author Ricardo Ewert
 *
 */
public class TripDistributionMatrixTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testTripDistributionCommercialPersonTrafficTraffic() throws IOException {

		Map<String, List<String>> landuseCategoriesAndDataConnection = new HashMap<>();
		Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String networkLocation = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		Network network = NetworkUtils.readNetwork(networkLocation);
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).resolve("investigationAreaData.csv");

		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
					usedLanduseConfiguration,
					getIndexLanduse(inputDataDirectory), getZoneIndex(inputDataDirectory), getIndexBuildings(inputDataDirectory),
					SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		String usedTrafficType = "commercialPersonTraffic";
		double sample = 1.;
		double resistanceFactor = 0.005;

		List<String> modesORvehTypes = new ArrayList<>(List.of("total"));
		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		ArrayList<String> listOfZones = new ArrayList<>( List.of("area1", "area2", "area3"));
		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
				.newInstance(getZoneIndex(inputDataDirectory), trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop, usedTrafficType,
                        listOfZones).build();

		Map<String, Map<Id<Link>, Link>> regionLinksMap = new HashMap<>();
		regionLinksMap.put("area1", new HashMap<>());
		regionLinksMap.get("area1").put(Id.createLinkId("i(8,6)"), network.getLinks().get(Id.createLinkId("i(8,6)")));
		regionLinksMap.put("area2", new HashMap<>());
		regionLinksMap.get("area2").put(Id.createLinkId("i(2,7)R"), network.getLinks().get(Id.createLinkId("i(2,7)R")));
		regionLinksMap.put("area3", new HashMap<>());
		regionLinksMap.get("area3").put(Id.createLinkId("i(2,1)R"), network.getLinks().get(Id.createLinkId("i(2,7)R")));

		for (String startZone : resultingDataPerZone.keySet()) {
			for (String stopZone : resultingDataPerZone.keySet()) {
				for (String modeORvehType : modesORvehTypes) {
					for (Integer purpose : trafficVolumePerTypeAndZone_start
							.get(TrafficVolumeGeneration.makeTrafficVolumeKey(startZone, modeORvehType)).keySet()) {
						odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose, usedTrafficType,
								network, regionLinksMap, resistanceFactor, shapeFileZoneNameColumn);
					}
				}
			}
		}
		odMatrix.clearRoundingError();

		//tests
		Assertions.assertEquals(3, odMatrix.getListOfZones().size(), MatsimTestUtils.EPSILON);
		for (String zone : resultingDataPerZone.keySet()) {
			Assertions.assertTrue(odMatrix.getListOfZones().contains(zone));
		}
		Assertions.assertEquals(1, odMatrix.getListOfModesOrVehTypes().size(), MatsimTestUtils.EPSILON);
		Assertions.assertTrue(odMatrix.getListOfModesOrVehTypes().contains("total"));

		Assertions.assertEquals(5, odMatrix.getListOfPurposes().size(), MatsimTestUtils.EPSILON);
		for (int i = 1; i <= 5; i++) {
			Assertions.assertTrue(odMatrix.getListOfPurposes().contains(i));
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
					Assertions.assertEquals(planedVolume, generatedVolume, MatsimTestUtils.EPSILON);
				}
			}
		}
		Assertions.assertEquals(sumStartServices, sumStopServices, MatsimTestUtils.EPSILON);
	}

	@Test
	void testTripDistributionGoodsTraffic() throws IOException {

		Map<String, List<String>> landuseCategoriesAndDataConnection = new HashMap<>();
		Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path outputDataDistributionFile = Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv");
		assert(new File(outputDataDistributionFile.getParent().resolve("calculatedData").toString()).mkdir());
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useOSMBuildingsAndLanduse";
		String networkLocation = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		Network network = NetworkUtils.readNetwork(networkLocation);
		String shapeFileZoneNameColumn = "name";
		String shapeFileBuildingTypeColumn = "type";
		Path pathToInvestigationAreaData = Path.of(utils.getPackageInputDirectory()).resolve("investigationAreaData.csv");

		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
					usedLanduseConfiguration,
					getIndexLanduse(inputDataDirectory), getZoneIndex(inputDataDirectory), getIndexBuildings(inputDataDirectory),
					SCTUtils.getIndexRegions(inputDataDirectory), shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		String usedTrafficType = "goodsTraffic";
		double sample = 1.;
		double resistanceFactor = 0.005;

		ArrayList<String> modesORvehTypes = new ArrayList<String>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		ArrayList<String> listOfZones = new ArrayList<>( List.of("area1", "area2", "area3"));

		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, outputDataDistributionFile.getParent(), sample, modesORvehTypes, usedTrafficType);
		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
				.newInstance(getZoneIndex(inputDataDirectory), trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop, usedTrafficType,
                        listOfZones).build();

		Map<String, Map<Id<Link>, Link>> regionLinksMap = new HashMap<>();
		regionLinksMap.put("area1", new HashMap<>());
		regionLinksMap.get("area1").put(Id.createLinkId("i(8,6)"), network.getLinks().get(Id.createLinkId("i(8,6)")));
		regionLinksMap.put("area2", new HashMap<>());
		regionLinksMap.get("area2").put(Id.createLinkId("i(2,7)R"), network.getLinks().get(Id.createLinkId("i(2,7)R")));
		regionLinksMap.put("area3", new HashMap<>());
		regionLinksMap.get("area3").put(Id.createLinkId("i(2,1)R"), network.getLinks().get(Id.createLinkId("i(2,7)R")));

		for (String startZone : resultingDataPerZone.keySet()) {
			for (String stopZone : resultingDataPerZone.keySet()) {
				for (String modeORvehType : modesORvehTypes) {
					for (Integer purpose : trafficVolumePerTypeAndZone_start
							.get(TrafficVolumeGeneration.makeTrafficVolumeKey(startZone, modeORvehType)).keySet()) {
						odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose, usedTrafficType,
								network, regionLinksMap, resistanceFactor, shapeFileZoneNameColumn);
					}
				}
			}
		}
		odMatrix.clearRoundingError();

		//tests
		Assertions.assertEquals(3, odMatrix.getListOfZones().size(), MatsimTestUtils.EPSILON);
		for (String zone : resultingDataPerZone.keySet()) {
			Assertions.assertTrue(odMatrix.getListOfZones().contains(zone));
		}
		Assertions.assertEquals(5, odMatrix.getListOfModesOrVehTypes().size(), MatsimTestUtils.EPSILON);
		for (String modeORvehType : modesORvehTypes) {
			Assertions.assertTrue(odMatrix.getListOfModesOrVehTypes().contains(modeORvehType));
		}

		Assertions.assertEquals(6, odMatrix.getListOfPurposes().size(), MatsimTestUtils.EPSILON);
		for (int i = 1; i <= 6; i++) {
			Assertions.assertTrue(odMatrix.getListOfPurposes().contains(i));
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
					Assertions.assertEquals(planedVolume, generatedVolume, MatsimTestUtils.EPSILON);
				}
			}
		}
		Assertions.assertEquals(sumStartServices, sumStopServices, MatsimTestUtils.EPSILON);
	}
}
