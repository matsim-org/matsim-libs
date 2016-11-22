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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityContributionCalculator;
import org.matsim.contrib.accessibility.AccessibilityStartupListener;
import org.matsim.contrib.accessibility.ConstantSpeedModeProvider;
import org.matsim.contrib.accessibility.FreeSpeedNetworkModeProvider;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.NetworkModeProvider;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtModule;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Key;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationNMBTest {
	public static final Logger log = Logger.getLogger( AccessibilityComputationNMBTest.class);

	private static final Double cellSize = 10000.;
//	private static final double timeOfDay = 8.*60*60;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void doAccessibilityTest() throws IOException {
		// Input and output
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/za/nmb/network/NMBM_Network_CleanV7.xml.gz";
		// Adapt folder structure that may be different on different machines, in particular on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
		networkFile = folderStructure + networkFile;
		final String facilitiesFile = folderStructure + "matsimExamples/countries/za/nmb/facilities/20121010/facilities.xml.gz";
		final String outputDirectory = utils.getOutputDirectory();
//		final String outputDirectory = "../../../shared-svn/projects/maxess/data/nmb/output/46/";
		final String travelTimeMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelTimeMatrix_space.csv";
		final String travelDistanceMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelDistanceMatrix_space.csv";
		final String ptStopsFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/ptStops.csv";

		// Parameters
		final String crs = TransformationFactory.WGS84_SA_Albers;
		final Envelope envelope = new Envelope(115000,161000,-3718000,-3679000);
		final String runId = "za_nmb_" + AccessibilityUtils.getDate() + "_" + cellSize.toString().split("\\.")[0];
		final boolean push2Geoserver = true;
		
		// QGis parameters
		boolean createQGisOutput = true;
		final boolean includeDensityLayer = true;
		final Double lowerBound = -3.5; // (upperBound - lowerBound) is ideally easily divisible by 7
		final Double upperBound = 3.5;
		final Integer range = 9; // in the current implementation, this must always be 9
		final int symbolSize = 1000;
		final int populationThreshold = (int) (120 / (1000/cellSize * 1000/cellSize));
		
		// Storage objects
		final List<String> modes = new ArrayList<>();
		
		// Config and scenario
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true); // TODO add PT in new version
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// Collect activity types
//		final List<String> activityTypes = AccessibilityUtils.collectAllFacilityOptionTypes(scenario);
//		log.info("Found activity types: " + activityTypes);
		final List<String> activityTypes = new ArrayList<>();
		activityTypes.add("education");
		activityTypes.add("shopping");
		activityTypes.add("leisure");
		
		// Settings for VSP check
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration);
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		
		// Matrix-based pt
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		mbpcg.setPtStopsInputFile(ptStopsFile);
		mbpcg.setUsingTravelTimesAndDistances(true);
		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrixFile);
		mbpcg.setPtTravelTimesInputFile(travelTimeMatrixFile);
		
		// Collect homes for density layer
		String activityFacilityType = "home";
		ActivityFacilities densityFacilities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);
		
		// Network bounds to determine envelope
		BoundingBox networkBounds = BoundingBox.createBoundingBox(scenario.getNetwork());
		Envelope networkEnvelope = new Envelope(networkBounds.getXMin(), networkBounds.getXMax(), networkBounds.getYMin(), networkBounds.getYMax());
		
		// Controller
		final Controler controler = new Controler(scenario);
		controler.addControlerListener(new AccessibilityStartupListener(activityTypes, densityFacilities, crs, runId, networkEnvelope, cellSize, push2Geoserver));
		controler.addOverridingModule(new MatrixBasedPtModule());
		
		// Add calculators
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				MapBinder<String,AccessibilityContributionCalculator> accBinder = MapBinder.newMapBinder(this.binder(), String.class, AccessibilityContributionCalculator.class);
				{
					String mode = "freeSpeed";
					this.binder().bind(AccessibilityContributionCalculator.class).annotatedWith(Names.named(mode)).toProvider(new FreeSpeedNetworkModeProvider(TransportMode.car));
					accBinder.addBinding(mode).to(Key.get(AccessibilityContributionCalculator.class, Names.named(mode)));
					if (!modes.contains(mode)) modes.add(mode); // This install method is called four times, but each new mode should only be added once
				}
				{
					String mode = TransportMode.car;
					this.binder().bind(AccessibilityContributionCalculator.class).annotatedWith(Names.named(mode)).toProvider(new NetworkModeProvider(mode));
					accBinder.addBinding(mode).to(Key.get(AccessibilityContributionCalculator.class, Names.named(mode)));
					if (!modes.contains(mode)) modes.add(mode); // This install method is called four times, but each new mode should only be added once
				}
				{ 
					String mode = TransportMode.bike;
					this.binder().bind(AccessibilityContributionCalculator.class).annotatedWith(Names.named(mode)).toProvider(new ConstantSpeedModeProvider(mode));
					accBinder.addBinding(mode).to(Key.get(AccessibilityContributionCalculator.class, Names.named(mode)));
					if (!modes.contains(mode)) modes.add(mode); // This install method is called four times, but each new mode should only be added once
				}
				{
					final String mode = TransportMode.walk;
					this.binder().bind(AccessibilityContributionCalculator.class).annotatedWith(Names.named(mode)).toProvider(new ConstantSpeedModeProvider(mode));
					accBinder.addBinding(mode).to(Key.get(AccessibilityContributionCalculator.class, Names.named(mode)));
					if (!modes.contains(mode)) modes.add(mode); // This install method is called four times, but each new mode should only be added once
				}
			}
		});
		controler.run();
		
		// QGis
		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (String mode : modes) {
					VisualizationUtils.createQGisOutput(actType, mode, envelope, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}