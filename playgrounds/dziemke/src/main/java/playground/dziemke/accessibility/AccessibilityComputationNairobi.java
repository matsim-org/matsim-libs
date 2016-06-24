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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityShutdownListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

import playground.dziemke.utils.LogToOutputSaver;

/**
 * @author dziemke
 */
public class AccessibilityComputationNairobi {
	public static final Logger log = Logger.getLogger( AccessibilityComputationNairobi.class ) ;

	private static final Double cellSize = 500.;

	public static void main(String[] args) {
		// Input and output
		String networkFile = "../../../shared-svn/projects/maxess/data/nairobi/network/2015-10-15_network.xml";
		String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/land_use/nairobi_LU_2010/facilites.xml";
//		String facilitiesFile = "../../../shared-svn/projects/maxess/data/nairobi/kodi/schools/secondary/facilities.xml";
		String outputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/output/19/";
		LogToOutputSaver.setOutputDirectory(outputDirectory);
		
		// minibus-pt
		String travelTimeMatrix = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/tt.csv";
		String travelDistanceMatrix = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/td.csv";
		String ptStops = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/temp/IDs.csv";
		
		// Parameters
		final String crs = "EPSG:21037"; // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		BoundingBox boundingBox = BoundingBox.createBoundingBox(240000, 9844000, 280000, 9874000);
		
		// QGis
		boolean createQGisOutput = true;
		boolean includeDensityLayer = false;
		Double lowerBound = -3.5; // (upperBound - lowerBound) is ideally easily divisible by 7
		Double upperBound = 3.5;
		Integer range = 9; // in the current implementation, this must always be 9
		int symbolSize = 510;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));

		// Config and scenario
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setLastIteration(0);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.abort);

		// some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		{
			StrategySettings stratSets = new StrategySettings();
			stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			stratSets.setWeight(1.);
			config.strategy().addStrategySettings(stratSets);
		}
		
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		// yy For a test, "abort" may be too strict.  kai, may'16
				
		final Scenario scenario = ScenarioUtils.loadScenario(config);
//		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());



		// matrix-based pt
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		mbpcg.setPtStopsInputFile(ptStops);
		mbpcg.setUsingTravelTimesAndDistances(true);
		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrix);
		mbpcg.setPtTravelTimesInputFile(travelTimeMatrix);


		// plansClacRoute parameters
		PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();

		// if no travel matrix (distances and times) is provided, the teleported mode speed for pt needs to be set
		// teleported mode speed for pt also required, see PtMatrix:120
//		ModeRoutingParams ptParameters = new ModeRoutingParams(TransportMode.pt);
//		ptParameters.setTeleportedModeSpeed(50./3.6);
//		plansCalcRoute.addModeRoutingParams(ptParameters);

		// by adding ModeRoutingParams (as done above for pt), the other parameters are deleted
		// the walk and bike parameters are needed, however. This is why they have to be set here again

		// teleported mode speed for walking also required, see PtMatrix:141
		ModeRoutingParams walkParameters = new ModeRoutingParams(TransportMode.walk);
		walkParameters.setTeleportedModeSpeed(3./3.6);
		plansCalcRoute.addModeRoutingParams(walkParameters );

		// teleported mode speed for bike also required, see AccessibilityControlerListenerImpl:168
		ModeRoutingParams bikeParameters = new ModeRoutingParams(TransportMode.bike);
		bikeParameters.setTeleportedModeSpeed(15./3.6);
		plansCalcRoute.addModeRoutingParams(bikeParameters );

		// pt matrix
//		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
//		PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);

		
		// collect activity types
//		final List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityOptionTypes(scenario);
//		log.warn( "found activity types: " + activityTypes );
		final List<String> activityTypes = new ArrayList<>();
		activityTypes.add("Educational");
//		activityTypes.add("Commercial");
//		activityTypes.add("Industrial");
//		activityTypes.add("Public Purpose");
//		activityTypes.add("Recreational");
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
		
		// no collection of homes for Nairobi; was necessary for density layer, instead based on network. see below
//		String activityFacilityType = "h";
//		ActivityFacilities homes = AccessibilityRunUtils.collectActivityFacilitiesOfType(scenario, activityFacilityType);

		
		// network density points
//		ActivityFacilities measuringPoints = 
//				AccessibilityRunUtils.createMeasuringPointsFromNetwork(scenario.getNetwork(), cellSize);
//		
//		double maximumAllowedDistance = 0.5 * cellSize;
//		final ActivityFacilities networkDensityFacilities = AccessibilityRunUtils.createNetworkDensityFacilities(
//				scenario.getNetwork(), measuringPoints, maximumAllowedDistance);		

		final Controler controler = new Controler(scenario) ;
//		controler.addOverridingModule(new AccessibilityComputationTestModuleCustomBoundary(
//				activityTypes, networkDensityFacilities, crs, name, cellSize, boundingBox));
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// Loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each
				for ( final String actType : activityTypes ) {
					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
						@Inject Scenario scenario;
						@Inject Map<String, TravelTime> travelTimes;
						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

						@Override
						public ControlerListener get() {
							AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, (Scenario) scenario, ConfigUtils.addOrGetModule((Config) config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class));
							accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox.getXMin(), boundingBox.getYMin(), boundingBox.getXMax(), boundingBox.getYMax(), cellSize));
							GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, (ActivityFacilities) AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, actType), null, config, scenario, travelTimes, travelDisutilityFactories, boundingBox.getXMin(), boundingBox.getYMin(), boundingBox.getXMax(), boundingBox.getYMax(), cellSize);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
//							listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);

//							listener.addAdditionalFacilityData(homes) ;
//							listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
							listener.writeToSubdirectoryWithName(actType);
							listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
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