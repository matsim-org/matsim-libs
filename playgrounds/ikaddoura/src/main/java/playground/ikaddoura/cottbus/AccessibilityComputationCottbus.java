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
package playground.ikaddoura.cottbus;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Envelope;

import playground.dziemke.utils.LogToOutputSaver;

/**
 * @author dziemke, ikaddoura
 */
public class AccessibilityComputationCottbus {
	public static final Logger log = Logger.getLogger(AccessibilityComputationCottbus.class);
	
	public static void main(String[] args) {
		// Input and output
		String runOutputFolder = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct/output/be_117j_baseCaseCtd_detailedNetwork/";
		String networkFile = runOutputFolder + "output_network.xml.gz";
		String facilitiesFile = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/berlin-2017-05-03_facilities/berlin-2017-05-03_facilities_DHDN_GK4.xml";
		String plansFile = runOutputFolder + "output_plans.xml.gz";
		String configFile = runOutputFolder + "output_config.xml.gz";
		String accessibilityOutputDirectory = runOutputFolder + "accessibilities/";	
		
		// Parameters
		final Double cellSize = 100.;
		String crs = TransformationFactory.DHDN_GK4;
		Envelope envelope = new Envelope(4571637,461898,5806002,5835967);
		final boolean push2Geoserver = false;
		
		// QGis parameters
		boolean createQGisOutput = true;
		boolean includeDensityLayer = true;
		Double lowerBound = .0;
		Double upperBound = 3.5;
		Integer range = 9;
		int symbolSize = 110;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		
		// Storage objects
		final List<String> modes = new ArrayList<>();
		
		// Config and scenario
		Config config = ConfigUtils.loadConfig(configFile, new AccessibilityConfigGroup());
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.plans().setInputFile(plansFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(accessibilityOutputDirectory);
		config.controler().setLastIteration(0);
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true); // if this is not set to true, output CSV will give NaN values
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		
		// Create facilities from plans
//		ActivityFacilities activityFacilities = AccessibilityRunUtils.createFacilitiesFromPlans(scenario.getPopulation());
//		scenario.setActivityFacilities(activityFacilities);
		
		// Infrastructure
		LogToOutputSaver.setOutputDirectory(accessibilityOutputDirectory);

		// Collect activity types
		List<String> activityTypes = new ArrayList<String>();
		activityTypes.add("work"); 
		activityTypes.add("education"); 
		activityTypes.add("grave_yard");
		activityTypes.add("police");
		activityTypes.add("medical");
		activityTypes.add("fire_station");

		// Collect homes for density layer
		String activityFacilityType = FacilityTypes.HOME;
		final ActivityFacilities densityFacilities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);

		// Controller
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