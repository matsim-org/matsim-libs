package org.matsim.integration.daily.accessibility;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

public class AccessibilityComputationNMBTest {
	public static final Logger log = Logger.getLogger( AccessibilityComputationNMBTest.class ) ;

	private static final double cellSize = 1000.;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
	public void doAccessibilityTest() throws IOException {
//		String folderStructure = "../../../"; // local on dz's computer
		String folderStructure = "../../"; // server
			
		String networkFile = folderStructure + "matsimExamples/countries/za/nmb/network/NMBM_Network_CleanV7.xml.gz";
		String facilitiesFile = folderStructure + "matsimExamples/countries/za/nmb/facilities/20121010/facilities.xml.gz";
		
		// minibus-pt
//		String travelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelTimeMatrix.csv.gz";
//		String travelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelDistanceMatrix.csv.gz";
//		String ptStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/measuringPointsAsStops/stops.csv.gz";

		// regular pt
		String travelTimeMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelTimeMatrix_space.csv";
		String travelDistanceMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelDistanceMatrix_space.csv";
		String ptStopsFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/ptStops.csv";
		
		
		// Parameters
		boolean createQGisOutput = false;
		boolean includeDensityLayer = true;
		String crs = TransformationFactory.WGS84_SA_Albers;
		
		Double lowerBound = 2.;
		Double upperBound = 5.5;
		Integer range = 9;
		int symbolSize = 1010;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		
		// extent of the network are (as they can looked up by using the bounding box):
		// minX = 111083.9441831379, maxX = 171098.03695045778, minY = -3715412.097693177,	maxY = -3668275.43481496
		// choose map view a bit bigger
		double[] mapViewExtent = {100000,-3720000,180000,-3675000};

		
		// config and scenario
		Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.abort);

		// some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		{
			StrategySettings stratSets = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			stratSets.setWeight(1.);
			config.strategy().addStrategySettings(stratSets);
		}
		
		
		Scenario scenario = ScenarioUtils.loadScenario( config );
		
		
		// matrix-based pt
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		mbpcg.setPtStopsInputFile(ptStopsFile);
		mbpcg.setUsingTravelTimesAndDistances(true);
		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrixFile);
		mbpcg.setPtTravelTimesInputFile(travelTimeMatrixFile);
		
		
		// plansClacRoute parameters
		PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();

		// if no travel matrix (distances and times) is provided, the teleported mode speed for pt needs to be set
		// teleported mode speed for pt also required, see PtMatrix:120
//      ModeRoutingParams ptParameters = new ModeRoutingParams(TransportMode.pt);
//      ptParameters.setTeleportedModeSpeed(50./3.6);
//      plansCalcRoute.addModeRoutingParams(ptParameters);

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
        BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
		PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);

		
		assertNotNull(config);

		
		// collect activity types
		List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityTypes(scenario);
		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
		
		// collect homes
		String activityFacilityType = "h";
		ActivityFacilities homes = AccessibilityRunUtils.collectActivityFacilitiesOfType(scenario, activityFacilityType);


		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		
		
		Controler controler = new Controler(scenario) ;

		
		// loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each combination
		for ( String actType : activityTypes ) {
//			if ( !actType.equals("w") ) {
//				log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//				continue ;
//			}

			ActivityFacilities opportunities = AccessibilityRunUtils.collectActivityFacilitiesOfType(scenario, actType);

			activityFacilitiesMap.put(actType, opportunities);

			GridBasedAccessibilityControlerListenerV3 listener = 
					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), 
							ptMatrix, config, scenario.getNetwork());
			listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
			
			listener.addAdditionalFacilityData(homes) ;
			listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
			
			listener.writeToSubdirectoryWithName(actType);
			
			// for push to geoserver
			listener.addSpatialGridDataExchangeListener(new GeoserverUpdater());
			
			listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim

			controler.addControlerListener(listener);
		}

		controler.run();

		
		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();

			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";

				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
//					if ( !actType.equals("w") ) {
//						log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//						continue ;
//					}
					VisualizationUtils.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}