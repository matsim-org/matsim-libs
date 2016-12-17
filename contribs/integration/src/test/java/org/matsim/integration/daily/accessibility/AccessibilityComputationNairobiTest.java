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

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationNairobiTest {
	public static final Logger log = Logger.getLogger(AccessibilityComputationNairobiTest.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

//	@Test
//	public void testQuick() {
//		run(1000., false, false);
//	}
//	@Test
//	public void testLocal() {
//		run(1000., false, true);
//	}
	@Test
	public void testOnServer() {
		run(1000., true, false);
	}
	
	public void run(Double cellSize, boolean push2Geoserver, boolean createQGisOutput) {

		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		
		// Network file
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/ke/nairobi/2015-10-15_network.xml";
//		String networkFile = "../shared-svn/projects/maxess/data/nairobi/network/2015-10-15_network_modified_policy.xml";
//		String networkFile = "../shared-svn/projects/maxess/data/kenya/network/2016-10-19_network_detailed.xml";
		// Adapt folder structure that may be different on different machines, in particular on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
		config.network().setInputFile(folderStructure + networkFile);
		
		config.facilities().setInputFile(folderStructure + "matsimExamples/countries/ke/nairobi/2016-07-09_facilities_airports.xml"); //airports
//		final String facilitiesFile = folderStructure + "matsimExamples/countries/ke/nairobi/2015-10-15_facilities.xml";
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/land_use/nairobi_LU_2010/facilities.xml";
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/kodi/schools/primary_public/facilities.xml";
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/kodi/health/hospitals/facilities.xml";
//		final String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/facilities/04/facilities.xml"; //airports
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.controler().setRunId("ke_nairobi_" + AccessibilityUtils.getDate() + "_" + cellSize.toString().split("\\.")[0] + "_kodi_sec_");
		
//		final String outputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/output/27/";
//		String travelTimeMatrix = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/tt.csv";
//		String travelDistanceMatrix = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/td.csv";
//		String ptStops = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/IDs.csv";
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
//		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setEnvelope(new Envelope(240000, 280000, 9844000, 9874000)); // whole Nairobi
//		final Envelope envelope = new Envelope(246000, 271000, 9853000, 9863000); // whole Nairobi // central part
//		final Envelope envelope = new Envelope(249000, 255000, 9854000, 9858000); // western Nairobi with Kibera
//		final Envelope envelope = new Envelope(-70000, 800000, 9450000, 10500000); // whole Kenya
//		final Envelope envelope = new Envelope(-70000, 420000, 9750000, 10100000); // Southwestern half of Kenya
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
		// TODO other modes
		acg.setOutputCrs("EPSG:21037"); // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
		ConfigUtils.setVspDefaults(config);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// Activity types
		final List<String> activityTypes = Arrays.asList(new String[]{"airport"}); 
//		activityTypes.add("Educational"); // land-use file version
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
					// TODO maybe use envelope and crs from above
					VisualizationUtils.createQGisOutput(actType, mode.toString(), new Envelope(240000, 280000, 9844000, 9874000), workingDirectory, "EPSG:21037", includeDensityLayer,
							lowerBound, upperBound, range, cellSize.intValue(), populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}  
		}
	}
}