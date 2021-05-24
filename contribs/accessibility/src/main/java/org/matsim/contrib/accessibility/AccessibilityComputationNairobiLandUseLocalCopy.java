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
package org.matsim.contrib.accessibility;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

/**
 * @author dziemke
 */
class AccessibilityComputationNairobiLandUseLocalCopy {
	public static final Logger LOG = Logger.getLogger(AccessibilityComputationNairobiLandUseLocalCopy.class);
	
	public static void main(String[] args) {
		int tileSize_m = 500;
		boolean push2Geoserver = false; // Set true for run on server
		boolean createQGisOutput = true; // Set false for run on server
		
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		
		Envelope envelope = new Envelope(246000, 271000, 9853000, 9863000); // Central part of Nairobi
		String scenarioCRS = "EPSG:21037"; // EPSG:21037 = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
		config.network().setInputFile("../nairobi/data/nairobi/input/2015-10-15_network.xml");
		config.facilities().setInputFile("../nairobi/data/land_use/Nairobi_LU_2010/facilities.xml");
		String runId = "ke_nairobi_landuse_hexagons_" + tileSize_m;
		config.controler().setOutputDirectory("../nairobi/data/nairobi/output/" + runId + "_lcpt_par4_car_tr-7_500/");
		config.controler().setRunId(runId);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBoxHexagons);
		acg.setEnvelope(envelope);
		acg.setTileSize_m(tileSize_m);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		//acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setOutputCrs(scenarioCRS);

		//acg.setUseParallelization(false);
		
		ConfigUtils.setVspDefaults(config);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		final List<String> activityTypes = Arrays.asList(new String[]{"educational", "commercial", "industrial", "recreational", "water_body"});
		final List<String> activityTypes = Arrays.asList(new String[]{"educational"});
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(Labels.DENSITIY, scenario.getNetwork());
		
		final Controler controler = new Controler(scenario);
		
		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			module.setCreateQGisOutput(createQGisOutput);
			controler.addOverridingModule(module);
		}
		
		controler.run();
		
		if (createQGisOutput) {
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = -3.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
			final Double upperBound = 3.5;
			final int populationThreshold = (int) (10 / (1000/tileSize_m * 1000/tileSize_m)); // People per km^2 or roads (?)
			
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					VisualizationUtils.createQGisOutputRuleBasedStandardColorRange(actType, mode.toString(), envelope, workingDirectory,
							scenarioCRS, lowerBound, upperBound, range, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}
		}
	}
}
