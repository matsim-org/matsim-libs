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
package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import com.vividsolutions.jts.geom.Envelope;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityShutdownListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;

import playground.dziemke.utils.LogToOutputSaver;

public class AccessibilityComputationCottbus {
	public static final Logger log = Logger.getLogger(AccessibilityComputationCottbus.class);
	
	private static final double cellSize = 200.;
	
	public static void main(String[] args) {
		// Input and output	
//		String runOutputFolder = "../../../public-svn/matsim/scenarios/countries/de/cottbus/cottbus-with-pt/output/cb02/";
		String runOutputFolder = "../../../public-svn/matsim/scenarios/countries/de/cottbus/commuter-population-only-car-traffic-only-100pct-2016-03-18/";

		// Parameters
		String crs = TransformationFactory.WGS84_UTM33N; // EPSG:32633 -- UTM33N
		

		// QGis parameters
		boolean createQGisOutput = true;
		boolean includeDensityLayer = true;
		Double lowerBound = .0;
		Double upperBound = 3.5;
		Integer range = 9;
		int symbolSize = 210;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		Envelope envelope = new Envelope(447000,5729000,461000,5740000);
		
		
		//
//		Config config = ConfigUtils.loadConfig(runOutputFolder + "output_config.xml.gz", new AccessibilityConfigGroup());
//		Config config = ConfigUtils.loadConfig(runOutputFolder + "output_config_2.xml", new AccessibilityConfigGroup());
		Config config = ConfigUtils.loadConfig(runOutputFolder + "config.xml", new AccessibilityConfigGroup()); // adaption after change to relative path in config
		//
		
		// Infrastructure
		String accessibilityOutputDirectory = runOutputFolder + "accessibilities/";
		LogToOutputSaver.setOutputDirectory(accessibilityOutputDirectory);

		// Config and scenario
//		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(accessibilityOutputDirectory);
		
//		config.network().setInputFile(runOutputFolder + "output_network.xml.gz");
		config.network().setInputFile("network_wgs84_utm33n.xml.gz");
		
		config.controler().setLastIteration(0);
		
//		config.plans().setInputFile(runOutputFolder + "output_plans.xml.gz");
		config.plans().setInputFile("commuter_population_wgs84_utm33n_car_only.xml.gz");
		
//		config.transit().setTransitScheduleFile(runOutputFolder + "output_transitSchedule.xml.gz");
//		config.transit().setVehiclesFile(runOutputFolder + "output_transitVehicles.xml.gz");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		
		ActivityFacilities activityFacilities = AccessibilityUtils.createFacilitiesFromPlans(scenario.getPopulation());
		scenario.setActivityFacilities(activityFacilities);

		// collect activity types
//		final List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityOptionTypes(scenario);
		List<String> activityTypes = new ArrayList<String>();
		activityTypes.add("work"); // manually setting computation only for work

		// collect homes
		String activityFacilityType = "home";
		final ActivityFacilities homes = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);

		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// Loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each
				for (final String actType : activityTypes) {
					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
						@Inject Scenario scenario;
						@Inject Map<String, TravelTime> travelTimes;
						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

						@Override
						public ControlerListener get() {
							AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, (Scenario) scenario);
							accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(447759., 5729049., 460617., 5740192., cellSize));
							GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, (ActivityFacilities) AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, actType), null, config, scenario, travelTimes,travelDisutilityFactories, 447759., 5729049., 460617., 5740192., cellSize);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
//							listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
							listener.addAdditionalFacilityData(homes);
//							listener.generateGridsAndMeasuringPointsByCustomBoundary(447000., 5729000., 461000., 5740000., cellSize);
							listener.writeToSubdirectoryWithName(actType);
							return listener;
						}
					});
				}
			}
		});
		controler.run();

		
		/* Write QGis output */
		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();

			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";

				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
					VisualizationUtils.createQGisOutput(actType, mode, envelope, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}