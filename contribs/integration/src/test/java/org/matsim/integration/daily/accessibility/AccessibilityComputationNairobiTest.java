/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.integration.daily.accessibility;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityOsmNetworkReader;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationNairobiTest {
	public static final Logger log = Logger.getLogger(AccessibilityComputationNairobiTest.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public void runAccessibilityComputation() {
		Double cellSize = 2000.;
		boolean push2Geoserver = false; // Set true for run on server
		boolean createQGisOutput = true; // Set false for run on server

		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		Envelope envelope = new Envelope(240000, 280000, 9844000, 9874000); // whole Nairobi
//		Envelope envelope = new Envelope(246000, 271000, 9853000, 9863000); // whole Nairobi // central part
//		Envelope envelope = new Envelope(251800.0, 258300.0, 9854300.0, 9858700.0); // City center and Kibera for minibus caluclation
		String scenarioCRS = "EPSG:21037"; // EPSG:21037 = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
		// ---------- Input (if pre-downloaded)
//		String folderStructure = "../../";
//		String networkFile = "matsimExamples/countries/ke/nairobi/2015-10-15_network.xml";
//		String networkFile = "../shared-svn/projects/maxess/data/nairobi/network/2015-10-15_network.xml";
//		String networkFile = "../shared-svn/projects/maxess/data/nairobi/minibus/07/grid10min_0.output_network.xml.gz";
//		String networkFile = "../shared-svn/projects/maxess/data/nairobi/network/2015-10-15_network_modified_policy.xml";
//		String networkFile = "../shared-svn/projects/maxess/data/kenya/network/2016-10-19_network_detailed.xml";
//		// Adapt folder structure that may be different on different machines, in particular on server
//		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
//		config.network().setInputFile(folderStructure + networkFile);
//		
//		final String facilitiesFile = folderStructure + "matsimExamples/countries/ke/nairobi/2015-10-15_facilities.xml";
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/land_use/nairobi_LU_2010/facilities.xml";
//		config.facilities().setInputFile("../../../shared-svn/projects/maxess/data/nairobi/facilities/2017-04-25_nairobi_central_and_kibera/2017-04-25_facilities_landuse_buildings.xml");
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/kodi/health/hospitals/facilities.xml";
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/facilities/04/facilities.xml"; //airports
//
//		final String outputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/output/27/";
		// ---------- 
		
		// ---------- Input (directly from OSM)
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(scenarioCRS, "EPSG:4326");
		Coord southwest = transformation.transform(new Coord(envelope.getMinX(), envelope.getMinY()));
		Coord northeast = transformation.transform(new Coord(envelope.getMaxX(), envelope.getMaxY()));
		Network network = null;
		ActivityFacilities facilities = null;
		try {
//			URL osm = new URL("http://api.openstreetmap.org/api/0.6/map?bbox=" + southwest.getX() + "," + southwest.getY() + "," + northeast.getX() + "," + northeast.getY());
			URL osm = new URL("http://overpass.osm.rambler.ru/cgi/xapi_meta?*[bbox=" + southwest.getX() + "," + southwest.getY() + "," + northeast.getX() + "," + northeast.getY() +"]");
			HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
			HttpURLConnection connection2 = (HttpURLConnection) osm.openConnection(); // TODO There might be more elegant option without creating this twice
//		    network = AccessibilityNetworkUtils.createNetwork(connection.getInputStream(), scenarioCRS, true, true, false);
			AccessibilityOsmNetworkReader networkReader = new AccessibilityOsmNetworkReader(connection.getInputStream(), scenarioCRS);
			networkReader.createNetwork();
			network = networkReader.getNetwork();
			
			facilities = CombinedOsmFacilityReaderKenya.createFacilites(connection2.getInputStream(), scenarioCRS, 0.);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// ---------- 
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setRunId("ke_nairobi_" + cellSize.toString().split("\\.")[0]);
		
		config.global().setCoordinateSystem(scenarioCRS);
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setEnvelope(envelope);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, false);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, false );
		acg.setOutputCrs(scenarioCRS);
		
		ConfigUtils.setVspDefaults(config);
		
//		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		// ---------- Schedule-based pt
//		config.transit().setUseTransit(true);
//		config.transit().setInputScheduleCRS("EPSG:4326");
//		config.transit().setTransitScheduleFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16_2/schedule2.xml");
//		config.transit().setVehiclesFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16_2/vehicles.xml");
//		config.transit().setInputScheduleCRS("EPSG:21037");
//		config.transit().setTransitScheduleFile("../../../shared-svn/projects/maxess/data/nairobi/minibus/07/grid10min_0.output_transitSchedule.xml.gz");
//		config.transit().setVehiclesFile("../../../shared-svn/projects/maxess/data/nairobi/minibus/07/grid10min_0.output_transitVehicles.xml.gz");
//		config.qsim().setEndTime(100*3600.);
//		config.transit().setTransitScheduleFile("../../../shared-svn/projects/maxess/data/nairobi/minibus/07/ITERS/it.100/grid10min_0.100.transitScheduleScored.xml.gz");
//		config.transit().setVehiclesFile("../../../shared-svn/projects/maxess/data/nairobi/minibus/07/ITERS/it.100/grid10min_0.100.vehicles.xml.gz");
//		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(TransformationFactory.getCoordinateTransformation("EPSG:4326", scenarioCRS), 
//		scenario2.getNetwork()).readFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16_2/network.xml");
//		MergeNetworks.merge(network, null, scenario2.getNetwork());
		ModeParams ptParams = new ModeParams(TransportMode.transit_walk);
		config.planCalcScore().addModeParams(ptParams);

		// ... trying to alter settings to drive down the number of "35527609 transfer links to be added".
//		config.transitRouter().setAdditionalTransferTime(additionalTransferTime); // default: 0.0
//		config.transitRouter().setDirectWalkFactor(directWalkFactor); // default: 1.0
//		config.transitRouter().setMaxBeelineWalkConnectionDistance(0.); // default: 100.0
//		config.transitRouter().setExtensionRadius(0.); // default: 200.0
//		config.transitRouter().setSearchRadius(0.); // default: 1000.0
		// ----------
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		
		scenario.setNetwork(network);
//		scenario.setNetwork(scenario2.getNetwork());
		
		scenario.setActivityFacilities(facilities);
//		scenario.setTransitSchedule(schedule);
//		scenario.setTransitVehicles(vehicles);
		
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.SHOPPING, FacilityTypes.EDUCATION});
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.SHOPPING});
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.WORK});

		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(scenario.getNetwork()); // Will be aggregated in downstream code!

		final Controler controler = new Controler(scenario);

		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			controler.addOverridingModule(module);
		}

		controler.run();

		if (createQGisOutput) {
			final boolean includeDensityLayer = true;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = -7.; // (upperBound - lowerBound) ideally	nicely divisible by (range - 2)
			final Double upperBound = 0.;
			final int populationThreshold = (int) (50 / (1000 / cellSize * 1000 / cellSize));

			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					VisualizationUtils.createQGisOutputGraduatedStandardColorRange(actType, mode.toString(), envelope, workingDirectory,
							scenarioCRS, includeDensityLayer, lowerBound, upperBound, range, cellSize.intValue(), populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}
		}
	}
}