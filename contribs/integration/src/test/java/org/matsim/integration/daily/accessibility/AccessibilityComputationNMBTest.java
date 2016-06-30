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
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtModule;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationNMBTest {
	public static final Logger log = Logger.getLogger( AccessibilityComputationNMBTest.class ) ;

	private static final Double cellSize = 500.;
//	private static final double time = 8.*60*60;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void doAccessibilityTest() throws IOException {
		// Input and output
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/za/nmb/network/NMBM_Network_CleanV7.xml.gz";
		// Adapt folder structure that may be different on different machines, in particular on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
		networkFile = folderStructure + networkFile ;
		String facilitiesFile = folderStructure + "matsimExamples/countries/za/nmb/facilities/20121010/facilities.xml.gz";
		String outputDirectory = utils.getOutputDirectory();
//		String outputDirectory = "../../../shared-svn/projects/maxess/data/nmb/output/46/";
		
		// Regular pt
		String travelTimeMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelTimeMatrix_space.csv";
		String travelDistanceMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelDistanceMatrix_space.csv";
		String ptStopsFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/ptStops.csv";

		// Parameters
		String crs = TransformationFactory.WGS84_SA_Albers;
		String name = "za_nmb_" + cellSize.toString().split("\\.")[0];
		
		//QGis
		boolean createQGisOutput = true;
		boolean includeDensityLayer = true;
		Double lowerBound = -3.5;
		Double upperBound = 3.5;
		Integer range = 9; // in the current implementation, this must always be 9
		int symbolSize = 510;
		int populationThreshold = (int) (120 / (1000/cellSize * 1000/cellSize));
//		final BoundingBox boundingBox = BoundingBox.createBoundingBox(115000,-3718000,161000,-3679000);
		Envelope envelope = new Envelope(115000,161000,-3718000,-3679000);

		// Config and scenario
		Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);
		
		// Switch computation on for all modes		
		AccessibilityConfigGroup accessibilityConfigGroup = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			accessibilityConfigGroup.setComputingAccessibilityForMode(mode, true);
		}

		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);

		// Some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration);

		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		stratSets.setWeight(1.);
		config.strategy().addStrategySettings(stratSets);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// Matrix-based pt
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		mbpcg.setPtStopsInputFile(ptStopsFile);
		mbpcg.setUsingTravelTimesAndDistances(true);
		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrixFile);
		mbpcg.setPtTravelTimesInputFile(travelTimeMatrixFile);

		assertNotNull(config);
		
		// Collect activity types
//		List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityTypes(scenario);
//		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
		List<String> activityTypes = new ArrayList<String>();
		activityTypes.add("e");
		
		// Collect homes
		String activityFacilityType = "h";
		ActivityFacilities homes = AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);
		
		// Controller
		Controler controler = new Controler(scenario) ;
	
		controler.addControlerListener(new AccessibilityStartupListener(activityTypes, homes, crs, name, cellSize));
		controler.addOverridingModule(new MatrixBasedPtModule());
		
		controler.run();

		
		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			double[] mapViewExtent = {envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()};
			
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