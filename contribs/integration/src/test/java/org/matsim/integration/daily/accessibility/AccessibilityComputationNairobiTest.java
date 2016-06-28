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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityStartupListener;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

public class AccessibilityComputationNairobiTest {
	public static final Logger log = Logger.getLogger( AccessibilityComputationNairobiTest.class ) ;

	private static final Double cellSize = 2000.;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void doAccessibilityTest() throws IOException {
		// Input and output
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/ke/nairobi/2015-10-15_network.xml";
		// Adapt folder structure that may be different on different machines, in particular on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
		networkFile = folderStructure + networkFile ;
		String facilitiesFile = folderStructure + "matsimExamples/countries/ke/nairobi/2015-10-15_facilities.xml";
		String outputDirectory = utils.getOutputDirectory();
//		String outputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/output/46/";		

		// Parameters
		final String crs = "EPSG:21037"; // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		final String name = "ke_nairobi_" + cellSize.toString().split("\\.")[0];
		final BoundingBox boundingBox = BoundingBox.createBoundingBox(240000, 9844000, 280000, 9874000);
		
		// QGis Parameters
		boolean createQGisOutput = true;
		boolean includeDensityLayer = false;
		Double lowerBound = 2.;
		Double upperBound = 7.25;
		Integer range = 9; // in the current implementation, this need always be 9
		int symbolSize = 2010;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));

		// Config and scenario
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);
		
		// Switch computation on for all modes		
		AccessibilityConfigGroup accessibilityConfigGroup = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);

		// Some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		stratSets.setWeight(1.);
		config.strategy().addStrategySettings(stratSets);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
//		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
		
		assertNotNull(config);
		
		// Collect activity types
//		final List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityOptionTypes(scenario);
//		log.warn( "found activity types: " + activityTypes );
		final List<String> activityTypes = new ArrayList<>();
		activityTypes.add(FacilityTypes.WORK);		

		// Network density points
		ActivityFacilities measuringPoints = AccessibilityRunUtils.createMeasuringPointsFromNetwork(scenario.getNetwork(), cellSize);
		
		double maximumAllowedDistance = 0.5 * cellSize;
		final ActivityFacilities networkDensityFacilities = AccessibilityRunUtils.createNetworkDensityFacilities(
				scenario.getNetwork(), measuringPoints, maximumAllowedDistance);		

		// Controller
		final Controler controler = new Controler(scenario);
				
		controler.addControlerListener(new AccessibilityStartupListener(activityTypes, networkDensityFacilities, crs, name, cellSize));

		controler.run();

		
		// QGis
		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			double[] mapViewExtent = {boundingBox.getXMin(), boundingBox.getYMin(), boundingBox.getXMax(), boundingBox.getYMax()};

			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";

				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
					VisualizationUtils.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}