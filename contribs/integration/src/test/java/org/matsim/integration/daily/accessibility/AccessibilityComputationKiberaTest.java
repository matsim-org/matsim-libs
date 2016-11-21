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
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.FreeSpeedNetworkModeProvider;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.NetworkModeProvider;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Key;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationKiberaTest {
	public static final Logger log = Logger.getLogger(AccessibilityComputationKiberaTest.class);

	private static final Double cellSize = 1000.;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void doAccessibilityTest() throws IOException {
		// Input and output
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/ke/kibera/2015-11-05_network_paths_detailed.xml";
//		String networkFile = "../shared-svn/projects/maxess/data/nairobi/network/2015-11-05_kibera_paths_detailed.xml";
		// Adapt folder structure that may be different on different machines, in particular on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
		networkFile = folderStructure + networkFile;
		final String facilitiesFile = folderStructure + "matsimExamples/countries/ke/kibera/2015-11-05_facilities.xml";
//		final String facilitiesFile = folderStructure + "../shared-svn/projects/maxess/data/nairobi/facilities/03/facilities.xml";
		final String outputDirectory = utils.getOutputDirectory();

		// Parameters
		final String crs = "EPSG:21037"; // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		final Envelope envelope = new Envelope(252000, 256000, 9854000, 9856000);
		final String runId = "ke_kibera_" + AccessibilityUtils.getDate() + "_" + cellSize.toString().split("\\.")[0];
		final boolean push2Geoserver = true;

		// QGis parameters
		boolean createQGisOutput = false;
		final boolean includeDensityLayer = false;
		final Double lowerBound = 0.; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
//		final Double lowerBound = 1.75;
		final Double upperBound = 3.5;
		final Integer range = 9; // in the current implementation, this need always be 9
		final int symbolSize = 10;
		final int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		
		// Storage objects
		final List<String> modes = new ArrayList<>();

		// Config and scenario
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
				
		// Collect activity types
//		final List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityOptionTypes(scenario);
//		log.info("Found activity types: " + activityTypes);
		final List<String> activityTypes = new ArrayList<>();
		activityTypes.add(FacilityTypes.DRINKING_WATER);
		
		// Settings for VSP check
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration);
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);

		// Network density points
		ActivityFacilities measuringPoints = AccessibilityUtils.createMeasuringPointsFromNetworkBounds(scenario.getNetwork(), cellSize);
		double maximumAllowedDistance = 0.5 * cellSize;
		final ActivityFacilities densityFacilities = AccessibilityUtils.createNetworkDensityFacilities(scenario.getNetwork(), measuringPoints, maximumAllowedDistance);

		// Controller
		final Controler controler = new Controler(scenario);
		controler.addControlerListener(new AccessibilityStartupListener(activityTypes, densityFacilities, crs, runId, envelope, cellSize, push2Geoserver));
		
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