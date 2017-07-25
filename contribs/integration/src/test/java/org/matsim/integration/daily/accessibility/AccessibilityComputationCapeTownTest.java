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
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationCapeTownTest {
	public static final Logger log = Logger.getLogger(AccessibilityComputationCapeTownTest.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void runAccessibilityComputation() {
		Double cellSize = 1000.;
		boolean push2Geoserver = true; // set true for run on server
		boolean createQGisOutput = false; // set false for run on server
		
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());

		// Input (if pre-downloaded)
		// Network file
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/za/capetown/2015-10-15_network.xml.gz";
		// Adapt folder structure that may be different on different machines, in particular on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
		config.network().setInputFile(folderStructure + networkFile);
		
		config.facilities().setInputFile(folderStructure + "matsimExamples/countries/za/capetown/2015-10-15_facilities.xml.gz");
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		// Input (directly from OSM)
		// TODO
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.controler().setRunId("za_capetown_" + cellSize.toString().split("\\.")[0]);
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setOutputCrs(TransformationFactory.WGS84_SA_Albers);
		
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromNetwork);
		// Network bounds to determine envelope
//		BoundingBox networkBounds = BoundingBox.createBoundingBox(scenario.getNetwork());
//		Envelope networkEnvelope = new Envelope(networkBounds.getXMin(), networkBounds.getXMax(), networkBounds.getYMin(), networkBounds.getYMax());
		
		ConfigUtils.setVspDefaults(config);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// Activity types
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION, FacilityTypes.SHOPPING});
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
			final Double lowerBound = 2.; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
			final Double upperBound = 5.5;
			final int populationThreshold = (int) (50 / (1000/cellSize * 1000/cellSize));
			
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					// TODO maybe use envelope and crs from above
					VisualizationUtils.createQGisOutputGraduated(actType, mode.toString(), new Envelope(-520000, -488000, -3733000, -3698000), workingDirectory, TransformationFactory.WGS84_SA_Albers, includeDensityLayer,
							lowerBound, upperBound, range, cellSize.intValue(), populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}  
		}
	}
}