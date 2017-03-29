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
import java.net.MalformedURLException;
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
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.AbstractModule;
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
	public void runAccessibilityComputation() throws IOException {
		Double cellSize = 1000.;
		boolean push2Geoserver = true; // set true for run on server
		boolean createQGisOutput = true; // set false for run on server

		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		// Notation: minX, maxX, minY, maxY
//		acg.setEnvelope(new Envelope(240000, 280000, 9844000, 9874000)); // whole Nairobi
		Envelope envelope = new  Envelope(246000, 271000, 9853000, 9863000); // whole Nairobi // central part
//		final Envelope envelope = new Envelope(249000, 255000, 9854000, 9858000); // western Nairobi with Kibera
//		final Envelope envelope = new Envelope(-70000, 800000, 9450000, 10500000); // whole Kenya
//		final Envelope envelope = new Envelope(-70000, 420000, 9750000, 10100000); // Southwestern half of Kenya
		String scenarioCRS = "EPSG:21037"; // EPSG:21037 = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
		// Input (if pre-downloaded)
//		String folderStructure = "../../";
//		String networkFile = "matsimExamples/countries/ke/nairobi/2015-10-15_network.xml";
//		String networkFile = "../shared-svn/projects/maxess/data/nairobi/network/2015-10-15_network_modified_policy.xml";
//		String networkFile = "../shared-svn/projects/maxess/data/kenya/network/2016-10-19_network_detailed.xml";
//		// Adapt folder structure that may be different on different machines, in particular on server
//		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
//		config.network().setInputFile(folderStructure + networkFile);
//		
//		config.facilities().setInputFile(folderStructure + "matsimExamples/countries/ke/nairobi/2016-07-09_facilities_airports.xml"); //airports
//		final String facilitiesFile = folderStructure + "matsimExamples/countries/ke/nairobi/2015-10-15_facilities.xml";
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/land_use/nairobi_LU_2010/facilities.xml";
//		config.facilities().setInputFile("../../../shared-svn/projects/maxess/data/nairobi/kodi/schools/primary_public/facilities.xml");
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/kodi/health/hospitals/facilities.xml";
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/facilities/04/facilities.xml"; //airports
//
//		final String outputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/output/27/";
		
		// Input (directly from OSM)
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(scenarioCRS, "EPSG:4326");
		Coord southwest = transformation.transform(new Coord(envelope.getMinX(), envelope.getMinY()));
		Coord northeast = transformation.transform(new Coord(envelope.getMaxX(), envelope.getMaxY()));
//		URL osm = new URL("http://api.openstreetmap.org/api/0.6/map?bbox=" + southwest.getX() + "," + southwest.getY() + "," + northeast.getX() + "," + northeast.getY());
		URL osm = new URL("http://overpass.osm.rambler.ru/cgi/xapi_meta?*[bbox=" + southwest.getX() + "," + southwest.getY() + "," + northeast.getX() + "," + northeast.getY() +"]");
		HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
		HttpURLConnection connection2 = (HttpURLConnection) osm.openConnection(); // TODO There might be more elegant option without creating this twice
		
	    Network network = CreateNetwork.createNetwork(connection.getInputStream(), scenarioCRS);
	    
	    double buildingTypeFromVicinityRange = 0.;
		ActivityFacilities facilities = RunCombinedOsmReaderKibera.createFacilites(connection2.getInputStream(), scenarioCRS, buildingTypeFromVicinityRange);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setRunId("ke_nairobi_" + cellSize.toString().split("\\.")[0] + "_kodi_sec");
		
//		String travelTimeMatrix = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/tt.csv";
//		String travelDistanceMatrix = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/td.csv";
//		String ptStops = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/IDs.csv";
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, false);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.matrixBasedPt, false);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true );
		acg.setOutputCrs("EPSG:21037"); // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
//		MatrixBasedPtRouterConfigGroup mbConfig = new MatrixBasedPtRouterConfigGroup();
////		mbConfig.setPtStopsInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/IDs.csv");
////		mbConfig.setPtTravelDistancesInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/td.csv");
////		mbConfig.setPtTravelTimesInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/tt.csv");
//		mbConfig.setPtStopsInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/otp_2015-06-16/fromIDs.csv");
//		mbConfig.setPtTravelDistancesInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/otp_2015-06-16/td.csv");
//		mbConfig.setPtTravelTimesInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/otp_2015-06-16/tt.csv");
////		mbConfig.setPtStopsInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/example/ptStops.csv");
////		mbConfig.setPtTravelDistancesInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/example/ptTravelInfo.csv");
////		mbConfig.setPtTravelTimesInputFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/example/ptTravelInfo.csv");
//		mbConfig.setUsingPtStops(true);
//		mbConfig.setUsingTravelTimesAndDistances(true);
//		config.addModule(mbConfig);
		
		ConfigUtils.setVspDefaults(config);
		
//		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		config.transit().setUseTransit(true);
		//
		config.transit().setTransitScheduleFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16/schedule.xml.gz");
		//
		{
			ModeRoutingParams walkPars = new ModeRoutingParams( TransportMode.walk ) ;
			walkPars.setBeelineDistanceFactor(1.3);
			walkPars.setTeleportedModeSpeed(4.);
			config.plansCalcRoute().addModeRoutingParams(walkPars);
		}
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		
		scenario.setNetwork(network);
		scenario.setActivityFacilities(facilities);
//		scenario.setTransitSchedule(schedule);
//		scenario.setTransitVehicles(vehicles);
		
//		BoundingBox bb = new BoundingBox(envelope);
////		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(scenario.getNetwork()), mbConfig);
//		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), bb, mbConfig);
//		scenario.addScenarioElement(PtMatrix.NAME, ptMatrix);
		
		// Activity types
//		final List<String> activityTypes = Arrays.asList(new String[]{"airport"}); 
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.SHOPPING}); // land-use file version
//		activityTypes.add("Commercial"); // land-use file version
//		activityTypes.add("Industrial"); // land-use file version
//		activityTypes.add("Public Purpose"); // land-use file version
//		activityTypes.add("Recreational"); // land-use file version
//		activityTypes.add("Hospital"); // kodi file version
		log.info("Using activity types: " + activityTypes);
		
		// Network density points (as proxy for population density)
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(scenario.getNetwork()); // will be aggregated in downstream code!
		
		final Controler controler = new Controler(scenario);
		
		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			controler.addOverridingModule(module);
		}
		
//		controler.addOverridingModule(new AbstractModule() {			
//			@Override
//			public void install() {
//				bind(PtMatrix.class).toInstance(ptMatrix);
//			}
//		});
		
		controler.run();
		
		// QGis
		if (createQGisOutput) {
			final boolean includeDensityLayer = true;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = -7.; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
			final Double upperBound = 0.;
			final int populationThreshold = (int) (50 / (1000/cellSize * 1000/cellSize));
			
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					VisualizationUtils.createQGisOutput(actType, mode.toString(), envelope, workingDirectory, scenarioCRS, includeDensityLayer,
							lowerBound, upperBound, range, cellSize.intValue(), populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}  
		}
	}
}